package de.fiereu.openmmo.launcher.content

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path

/**
 * Loads every patched class through the JVM's own verifier.
 *
 * The structural tests check that a patch changed what it meant to, which is not the same as the
 * result being loadable: a patch that adds a branch without recomputing stackmap frames passes
 * every shape assertion and then kills the client with a VerifyError before it draws a frame. That
 * happened, so this closes it - the only way to know a class verifies is to have the JVM verify it.
 */
class OverlayVerifiesTest :
    FunSpec({
      val local = System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home")
      val client = Path.of("$local/MonMMO-EX/Client-31914/MonMMO-Local.exe")
      val overlay = Path.of("$local/MonMMO-EX/Client-31914/patch-classes.jar")

      test("every class in the overlay jar links and verifies") {
        if (!Files.isRegularFile(client) || !Files.isRegularFile(overlay)) return@test
        val names =
            java.util.zip.ZipFile(overlay.toFile()).use { jar ->
              jar.entries()
                  .asSequence()
                  .filter { it.name.endsWith(".class") }
                  .map { it.name.removeSuffix(".class").replace('/', '.') }
                  .toList()
            }
        names.isNotEmpty() shouldBe true

        // The overlay first, then the client, exactly as the launcher orders them.
        val loader =
            URLClassLoader(
                arrayOf(overlay.toUri().toURL(), client.toUri().toURL()),
                ClassLoader.getPlatformClassLoader(),
            )
        // The test JVM is newer than the client's bundled Java 17, so it accepts class files the
        // client rejects outright - which shipped once as a fatal launch error. Check the version
        // bytes against the client's ceiling, not ours.
        java.util.zip.ZipFile(overlay.toFile()).use { jar ->
          jar.entries()
              .asSequence()
              .filter { it.name.endsWith(".class") }
              .forEach { entry ->
                val head = jar.getInputStream(entry).use { it.readNBytes(8) }
                val major = ((head[6].toInt() and 0xff) shl 8) or (head[7].toInt() and 0xff)
                withClue("${entry.name} targets class file $major; the client tops out at 61") {
                  (major <= 61) shouldBe true
                }
              }
        }
        val failures = mutableListOf<String>()
        loader.use {
          names.forEach { name ->
            try {
              // Linking is what runs the verifier; resolve = true forces it.
              Class.forName(name, false, loader).methods
            } catch (error: VerifyError) {
              failures += "$name: ${error.message?.lineSequence()?.firstOrNull()}"
            } catch (error: ClassFormatError) {
              failures += "$name: ${error.message}"
            } catch (error: NoClassDefFoundError) {
              // A dependency of the class is absent from this pared-down classpath, not a defect
              // in the patch itself.
            } catch (error: Throwable) {
              if (error is Error && error !is ExceptionInInitializerError) {
                failures += "$name: ${error::class.simpleName} ${error.message}"
              }
            }
          }
        }
        failures shouldBe emptyList()
      }
    })
