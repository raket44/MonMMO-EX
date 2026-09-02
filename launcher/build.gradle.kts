import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  id("buildsrc.convention.kotlin-jvm")
  id("buildsrc.convention.spotless")
  id("buildsrc.convention.sonarlint")
  id("buildsrc.common.keys")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.compose)
  alias(libs.plugins.compose.compiler)
}

dependencies {
  implementation(project(":codegen"))
  implementation(libs.bundles.crypto)
  implementation(libs.kotlinx.coroutines)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.tomlkt)
  implementation(libs.vcdiff.core)
  implementation(compose.desktop.currentOs)
  implementation(compose.material3)
  implementation(libs.asm)
  testImplementation(libs.bundles.kotest)
  testImplementation(libs.kotlinx.coroutines.test)
}

tasks.register<JavaExec>("stageExpansionClientContent") {
  group = "openmmo"
  description = "Builds a local, inactive Expansion data.pak and English string table"
  dependsOn(":codegen:generateExpansionPokemon", "classes")
  mainClass.set("de.fiereu.openmmo.launcher.content.ExpansionClientContentMain")
  classpath(sourceSets.main.get().runtimeClasspath)
  val local = System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home")
  val install =
      (project.findProperty("expansion.clientRoot") as String?) ?: "$local/MonMMO-EX/Client-31914"
  // Once generated content is installed the client no longer holds stock data. Installing keeps a
  // pristine copy beside it, and staging reads that so the task stays repeatable.
  val stock = File("$install/stock-backup")
  val client = if (stock.isDirectory) stock.path else install
  val output = layout.buildDirectory.dir("expansion-client").get().asFile
  args(
      "$client/data/data.pak",
      File(output, "data/data.pak").path,
      "$client/data/strings/strings_en.xml",
      File(output, "data/strings/strings_en.xml").path,
      rootProject.layout.projectDirectory.dir("../pokeemerald-expansion").asFile.absolutePath,
  )
}

tasks.register<JavaExec>("stageRetailData") {
  group = "openmmo"
  description = "Compacts the retail monsters.json into server calibration tables"
  dependsOn("classes")
  mainClass.set("de.fiereu.openmmo.launcher.content.RetailMonstersMain")
  classpath(sourceSets.main.get().runtimeClasspath)
  maxHeapSize = "2g"
  args(
      rootProject.layout.projectDirectory
          .file("reference/monsters-retail.json")
          .asFile
          .absolutePath,
      rootProject.layout.projectDirectory
          .dir("codegen/src/main/resources/monmmo")
          .asFile
          .absolutePath,
  )
}

tasks.register<JavaExec>("patchClientTypes") {
  group = "openmmo"
  description = "Writes Fairy-aware client classes into a classpath overlay jar"
  dependsOn("classes")
  mainClass.set("de.fiereu.openmmo.launcher.content.FairyTypePatchMain")
  classpath(sourceSets.main.get().runtimeClasspath)
  val local = System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home")
  val install =
      (project.findProperty("expansion.clientRoot") as String?) ?: "$local/MonMMO-EX/Client-31914"
  args(
      "$install/PokeMMO.exe",
      rootProject.layout.projectDirectory.dir("../pokeemerald-expansion").asFile.absolutePath,
      "$install/patch-classes.jar",
  )
}

compose.desktop {
  application {
    mainClass = "de.fiereu.openmmo.launcher.ui.MainKt"
    jvmArgs += "--enable-native-access=ALL-UNNAMED"

    nativeDistributions {
      // jlink ships only what is asked for, and a missing module fails at runtime, not at build.
      modules(
          "java.net.http",
          "java.xml",
          "jdk.httpserver",
          "jdk.crypto.ec",
          "java.naming",
          "jdk.unsupported",
      )
      targetFormats(TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Dmg)
      packageName = "OpenMMO"
      packageVersion = project.version.toString()
      description = "Provisions and patches a PokeMMO client for OpenMMO"
      vendor = "openmmo-org"
      licenseFile.set(rootProject.file("LICENSE"))

      // Only the platform subdirectories of this root are copied, hence the staged "common".
      appResourcesRootDir.set(layout.buildDirectory.dir("appResources"))
    }
  }
}

val stageAppResources by
    tasks.registering(Copy::class) {
      group = "openmmo"
      description = "Stages the patch manifests and keys where the packaging step picks them up"
      dependsOn("copyPublicKeys", "copyPrivateKeyFeed")
      from(layout.projectDirectory.dir("manifests"))
      // Only public keys ship. Development runs from Gradle, where the private one is on the
      // classpath already.
      from(layout.projectDirectory.dir("src/main/resources")) { include("*.public.pem") }
      into(layout.buildDirectory.dir("appResources/common"))
    }

// The Compose plugin registers this one lazily, so it cannot be looked up by name here.
tasks.matching { it.name == "prepareAppResources" }.configureEach { dependsOn(stageAppResources) }

val feedOrigin =
    (project.findProperty("openmmo.feedOrigin") as String?) ?: "https://127.0.0.1:20443"

val archiveOrigin =
    (project.findProperty("openmmo.archiveOrigin") as String?)
        ?: "https://github.com/openmmo-org/archive"

val archiveRawOrigin =
    (project.findProperty("openmmo.archiveRawOrigin") as String?)
        ?: "https://raw.githubusercontent.com/openmmo-org/archive/master"

// Loopback by default. A hostname cannot be padded, so a replacement must be exactly 23 wide.

val launcherPropertiesDir = layout.buildDirectory.dir("launcherProperties")

val writeLauncherProperties by
    tasks.registering(WriteProperties::class) {
      group = "openmmo"
      description = "Bakes the feed and archive origins into the launcher"
      destinationFile.set(launcherPropertiesDir.map { it.file("launcher.properties") })
      property("feed.origin", feedOrigin)
      property("archive.origin", archiveOrigin)
      property("archive.rawOrigin", archiveRawOrigin)
    }

sourceSets.main.get().resources.srcDir(launcherPropertiesDir)

tasks.named("processResources") { dependsOn(writeLauncherProperties) }

fun JavaExec.devFeed() {
  group = "application"
  mainClass.set("de.fiereu.openmmo.launcher.launch.DevFeedServerCli")
  classpath(sourceSets.main.get().runtimeClasspath)
  systemProperty("openmmo.manifests", layout.projectDirectory.dir("manifests").asFile.path)
  listOf(
          "openmmo.root",
          "openmmo.manifests",
          "openmmo.devFeedPort",
          "openmmo.revision",
          "openmmo.archiveOrigin",
          "openmmo.archiveRawOrigin")
      .forEach { name -> (project.findProperty(name) as String?)?.let { systemProperty(name, it) } }
  standardInput = System.`in`
}

tasks.register<JavaExec>("feedServer") {
  description = "Serves the development feed on loopback"
  devFeed()
}

tasks.register<JavaExec>("dev") {
  description = "Serves the development feed and starts the launcher against it"
  devFeed()
  systemProperty("openmmo.launchUi", "true")
}

tasks.register<JavaExec>("launcherUi") {
  group = "application"
  description = "Runs the OpenMMO launcher window"

  mainClass.set("de.fiereu.openmmo.launcher.ui.MainKt")
  classpath(sourceSets.main.get().runtimeClasspath)
  systemProperty("openmmo.manifests", layout.projectDirectory.dir("manifests").asFile.path)
  listOf("openmmo.root", "openmmo.manifests").forEach { name ->
    (project.findProperty(name) as String?)?.let { systemProperty(name, it) }
  }
  // Skiko loads its native renderer, which Java 25 warns about unless it is allowed up front.
  jvmArgs("--enable-native-access=ALL-UNNAMED")
  maxHeapSize = "1g"
}

tasks.register<JavaExec>("patchClient") {
  group = "application"
  description = "Applies a patch manifest to the managed install and builds the runtime tree"

  mainClass.set("de.fiereu.openmmo.launcher.patch.PatchCli")
  classpath(sourceSets.main.get().runtimeClasspath)
  listOf("openmmo.root", "openmmo.manifest").forEach { name ->
    (project.findProperty(name) as String?)?.let { systemProperty(name, it) }
  }
  maxHeapSize = "1g"
}

listOf("classes", "processResources").forEach { taskName ->
  tasks.named(taskName) { dependsOn("copyPublicKeys", "copyPrivateKeyFeed") }
}

tasks.register<JavaExec>("extractNdsMaps") {
  group = "openmmo"
  description = "Extracts NDS-region map headers and warps from the local decomps"
  dependsOn("classes")
  mainClass.set("de.fiereu.openmmo.launcher.content.NdsMapExtractor")
  classpath(sourceSets.main.get().runtimeClasspath)
  args(
      rootProject.layout.projectDirectory.dir("decomp").asFile.absolutePath,
      layout.buildDirectory.dir("nds-maps").get().asFile.absolutePath,
  )
}

tasks.register<JavaExec>("reportWildLocations") {
  group = "openmmo"
  description = "Reports the per-season wild-locations section builds"
  dependsOn("classes")
  mainClass.set("de.fiereu.openmmo.launcher.content.WildLocationsSectionKt")
  classpath(sourceSets.main.get().runtimeClasspath)
}

tasks.register<JavaExec>("extractNdsCollision") {
  group = "openmmo"
  description = "Extracts NDS map collision grids from a ROM"
  dependsOn("classes")
  mainClass.set("de.fiereu.openmmo.launcher.content.NdsCollisionExtractor")
  classpath(sourceSets.main.get().runtimeClasspath)
  maxHeapSize = "2g"
  val rom = (project.findProperty("nds.rom") as String?) ?: ""
  val game = (project.findProperty("nds.game") as String?) ?: "hgss"
  args(rom, game, layout.buildDirectory.dir("nds-maps/$game").get().asFile.absolutePath)
}

tasks.register<JavaExec>("mergeMonstersJson") {
  group = "openmmo"
  description = "Adds the Expansion's species to data/pokemmo/monsters.json in the dump's schema"
  dependsOn(":codegen:generateExpansionPokemon", "classes")
  mainClass.set("de.fiereu.openmmo.launcher.content.MonstersJsonMerge")
  classpath(sourceSets.main.get().runtimeClasspath)
  maxHeapSize = "2g"
  args(
      rootProject.layout.projectDirectory.file("data/pokemmo/monsters.json").asFile.absolutePath,
      rootProject.layout.projectDirectory.dir("../pokeemerald-expansion").asFile.absolutePath,
  )
}
