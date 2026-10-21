package pirated.launcher.patch

import pirated.launcher.client.FeedClient
import pirated.launcher.client.ManagedInstall
import pirated.launcher.client.Platform
import pirated.launcher.launch.GeneratedKeys
import pirated.launcher.launch.executableName
import pirated.launcher.launch.feedPatches
import pirated.launcher.launch.loginHostPatch
import java.net.http.HttpClient
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

private const val ROOT_PROPERTY = "pirated.root"
private const val MANIFEST_PROPERTY = "pirated.manifest"

object PatchCli {

  @JvmStatic
  fun main(args: Array<String>) {
    val root = System.getProperty(ROOT_PROPERTY)?.let(Path::of) ?: ManagedInstall.defaultRoot()
    val manifestPath =
        System.getProperty(MANIFEST_PROPERTY)?.let(Path::of)
            ?: error("Missing -D$MANIFEST_PROPERTY=<path to manifest xml>")

    val install = ManagedInstall(root).create()
    val manifest = PatchManifestParser.parse(Files.readAllBytes(manifestPath))
    val feeds = FeedClient(HttpClient.newHttpClient()).load()

    println("manifest   $manifestPath for revision ${manifest.revision}")
    println("feed       revision ${feeds.main.revision}")
    if (manifest.revision != feeds.main.revision) {
      System.err.println(
          "No patch manifest for client revision ${feeds.main.revision}, refusing to patch")
      exitProcess(1)
    }

    val assets = PatchAssets.ofDirectory(manifestPath.parent ?: Path.of("."))
    val tree =
        try {
          PatchEngine(install, assets, GeneratedKeys.values(), executableName(Platform.current()))
              .apply(manifest, feeds.update, feedPatches() + loginHostPatch())
        } catch (e: PatchFailedException) {
          System.err.println("Patch failed: ${e.message}")
          exitProcess(1)
        }

    val byOrigin = tree.files.values.groupingBy { it }.eachCount()
    println("runtime    ${tree.root}")
    println("files      ${tree.files.size} total, $byOrigin")
    tree.files
        .filterValues { it != Origin.FEED }
        .forEach { (name, origin) -> println("  $origin $name") }
  }
}
