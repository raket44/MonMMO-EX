package pirated.launcher.launch

import pirated.launcher.client.ArchiveClient
import pirated.launcher.client.ClientSync
import pirated.launcher.client.Downloader
import pirated.launcher.client.FeedClient
import pirated.launcher.client.Feeds
import pirated.launcher.client.ManagedInstall
import pirated.launcher.client.Platform
import pirated.launcher.client.SyncProgress
import pirated.launcher.patch.ArchivePatcher
import pirated.launcher.patch.PatchAssets
import pirated.launcher.patch.PatchEngine
import pirated.launcher.patch.PatchManifest
import pirated.launcher.patch.PatchManifestParser
import pirated.launcher.patch.RuntimeTree
import java.net.http.HttpClient
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

sealed interface LaunchStage {
  data object Resolving : LaunchStage

  data class Syncing(val progress: SyncProgress) : LaunchStage

  data object DeltaPatching : LaunchStage

  data object Patching : LaunchStage

  data object Starting : LaunchStage
}

class UnsupportedRevisionException(revision: Int) :
    Exception(
        "Pirated Version has no patch set or delta patch for client revision $revision yet. " +
            "Patching a client whose layout changed would corrupt it, so this is a hard stop.")

class LauncherPipeline(
    private val install: ManagedInstall,
    private val manifests: (Int) -> PatchManifest?,
    private val assets: PatchAssets = PatchAssets.none(),
    private val http: HttpClient = ArchiveClient.defaultHttpClient(),
    private val platform: Platform = Platform.current(),
    private val archiveOrigin: String = ArchiveOrigin.configured,
    private val archiveRawOrigin: String = ArchiveOrigin.configuredRaw,
    private val highestRevision: () -> Int? = { null },
) {

  suspend fun run(onStage: (LaunchStage) -> Unit = {}): Process {
    onStage(LaunchStage.Resolving)
    install.create()
    val feeds: Feeds = FeedClient(http).load()
    val currentRevision = feeds.main.revision

    val directManifest = manifests(currentRevision)
    val sync = ClientSync(install, Downloader(http), platform)

    val (activeManifest, activeUpdateFeed) =
        if (directManifest != null) {
          sync.sync(feeds) { onStage(LaunchStage.Syncing(it)) }
          Pair(directManifest, feeds.update)
        } else {
          val targetRevision =
              highestRevision() ?: throw UnsupportedRevisionException(currentRevision)
          val targetManifest =
              manifests(targetRevision) ?: throw UnsupportedRevisionException(targetRevision)

          val archiveClient = ArchiveClient(http, archiveOrigin, archiveRawOrigin)
          val targetUpdateFeed = archiveClient.fetchUpdateFeed(targetRevision)

          val isUpToDate = sync.plan(targetUpdateFeed).isUpToDate
          if (!isUpToDate) {
            if (!archiveClient.hasDelta(currentRevision, targetRevision)) {
              throw UnsupportedRevisionException(currentRevision)
            }
            sync.syncAll(feeds) { onStage(LaunchStage.Syncing(it)) }

            onStage(LaunchStage.DeltaPatching)
            val updatedFeed =
                ArchivePatcher(install, archiveClient, platform)
                    .apply(currentRevision, targetRevision, feeds.update)
            Pair(targetManifest, updatedFeed)
          } else {
            Pair(targetManifest, targetUpdateFeed)
          }
        }

    onStage(LaunchStage.Patching)
    val runtime: RuntimeTree =
        PatchEngine(install, assets, keys(), executableName(platform))
            .apply(activeManifest, activeUpdateFeed, feedPatches() + loginHostPatch())

    onStage(LaunchStage.Starting)
    return GameLaunch(install, platform).start(runtime)
  }

  /**
   * The feed origin publishes the server key, so rotating it does not need a new launcher. Only the
   * feed key itself stays baked in, because it is what proves the rest genuine.
   */
  private fun keys(): Map<String, String> =
      GeneratedKeys.values() +
          (GeneratedKeys.GAME_PUBLIC to
              ServerKeys(http, FeedOrigin.configured, GeneratedKeys.feedPublicKey())
                  .gamePublicKeyBase64())

  companion object {
    fun manifestsIn(directory: Path): (Int) -> PatchManifest? = { revision ->
      val file = directory.resolve("manifest-$revision.toml")
      if (Files.isRegularFile(file)) PatchManifestParser.parse(Files.readAllBytes(file)) else null
    }

    fun highestSupportedRevisionIn(directory: Path): () -> Int? = {
      if (Files.isDirectory(directory)) {
        val regex = Regex("""manifest-(\d+)\.toml""")
        Files.list(directory).use { stream ->
          stream
              .map { it.fileName.toString() }
              .toList()
              .mapNotNull { name -> regex.matchEntire(name)?.groupValues?.get(1)?.toIntOrNull() }
              .maxOrNull()
        }
      } else {
        null
      }
    }
  }
}
