package de.fiereu.openmmo.trainer

import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.trainer.generated.GeneratedHoennTrainers
import de.fiereu.openmmo.trainer.generated.GeneratedKantoTrainers
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** A monster on a trainer's team. Empty [moveIds] means the level up moveset is used. */
data class TrainerMon(
    val dexId: Int,
    val level: Int,
    val iv: Int,
    val heldItem: Int,
    val moveIds: List<Int>,
)

data class TrainerDef(
    val id: Int,
    val name: String,
    val trainerClass: Int,
    val doubleBattle: Boolean,
    /** What the class pays per level of its last monster. */
    val prizeRate: Int,
    val party: List<TrainerMon>,
    /** The original pret constant, for example TRAINER_YOUNGSTER_BEN. */
    val constant: String = "",
    /** Base trainer plus rematch stages. Null entries preserve FireRed's SKIP slots. */
    val rematchIds: List<Int?> = emptyList(),
)

private data class TrainerKey(val region: Region, val id: Int)

private data class TrainerConstantKey(val region: Region, val constant: String)

/**
 * The trainers from the decomp, keyed by region and trainer id. The two regions number their
 * trainers from zero, so the region is part of the key.
 */
@Singleton
class TrainerRegistry @Inject constructor() {

  private val trainers = ConcurrentHashMap<TrainerKey, TrainerDef>()
  private val trainersByConstant = ConcurrentHashMap<TrainerConstantKey, TrainerDef>()

  init {
    GeneratedHoennTrainers.loadInto(this)
    GeneratedKantoTrainers.loadInto(this)
  }

  fun register(region: Region, def: TrainerDef) {
    val previous = trainers.put(TrainerKey(region, def.id), def)
    // Generated data, so a clash means the parser produced two trainers for one id and the second
    // would quietly replace the first.
    check(previous == null) {
      "Two $region trainers share id ${def.id}: ${previous?.name}, ${def.name}"
    }
    if (def.constant.isNotEmpty()) {
      val previousConstant = trainersByConstant.put(TrainerConstantKey(region, def.constant), def)
      check(previousConstant == null) {
        "Two $region trainers share constant ${def.constant}: " +
            "${previousConstant?.name}, ${def.name}"
      }
    }
  }

  fun get(region: Region, id: Int): TrainerDef? = trainers[TrainerKey(region, id)]

  fun get(region: Region, constant: String): TrainerDef? =
      trainersByConstant[TrainerConstantKey(region, constant)]

  fun size(): Int = trainers.size
}
