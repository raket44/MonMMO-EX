package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.server.game.storage.StoredCharacter
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Export/import character state as tweakable JSON. */
@Singleton
class LocalSaveService @Inject constructor() {

  private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
  }

  fun export(char: StoredCharacter, file: File) {
    file.writeText(json.encodeToString(SaveBlob.from(char)))
  }

  fun import(char: StoredCharacter, file: File): StoredCharacter {
    val blob = json.decodeFromString<SaveBlob>(file.readText())
    return char.copy(
        items = blob.items.entries.associate { it.key.toInt() to it.value }.toMutableMap(),
        storyFlags = blob.storyFlags.toMutableSet(),
        storyVars = blob.storyVars.toMutableMap(),
    )
  }

  @Serializable
  data class SaveBlob(
      val name: String,
      val regionId: Int,
      val items: Map<String, Int>,
      val storyFlags: Set<String>,
      val storyVars: Map<String, Int>,
      val party: List<String>,
  ) {
    companion object {
      fun from(char: StoredCharacter) =
          SaveBlob(
              name = char.info.name,
              regionId = char.info.positionRegionId.toInt(),
              items = char.items.entries.associate { it.key.toString() to it.value },
              storyFlags = char.storyFlags.toSet(),
              storyVars = char.storyVars.toMap(),
              party =
                  char.pokemon.map { p ->
                    "${p.dexId},${p.level},${p.hp},${p.nature.name}," +
                        p.moves.joinToString("|") { "${it.id}:${it.pp}" }
                  },
          )
    }
  }
}
