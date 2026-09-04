package de.fiereu.openmmo.net.game.packets.battle

import de.fiereu.bytecodec.*

/** The body of a nested battle action event, selected by its [BattleEventType]. */
sealed interface BattleEventBody {
  /** Event 0, BattleHpUpdate: the entity's resulting hp. */
  data class HpUpdate(val currentHp: Short) : BattleEventBody

  /**
   * Event 1, BattleStatChange. [stat] is the wire stat id (1 atk, 2 def, 3 spd, ...) and
   * [stageDelta] the signed number of stages (-1 down one, +2 up two). [changeType] is 0 for a
   * normal stage change.
   */
  data class StatChange(val stat: Byte, val stageDelta: Short, val changeType: Byte = 0) :
      BattleEventBody

  /** Event 5, BattlePokemonFainted. [playFaintAnimation] plays the faint-in-place animation. */
  data class Faint(val playFaintAnimation: Boolean) : BattleEventBody

  /** Event 4, meaning unknown. It carries no body. Effectiveness rides in the outcome word. */
  data object EffectivenessMessage : BattleEventBody

  /**
   * Event 107, BattleEvolution: the client rebuilds the battle mon as [species] (form [form]),
   * plays the evolve sequence and announces it. Decompiled off the client's event reader (f/SF1
   * case 107 -> f/BK: short species, one discarded byte, short form, two stat shorts fed to the
   * entity rebuild - sent as the evolved current and max hp).
   */
  data class Evolution(
      val species: Short,
      val form: Short = 0,
      val currentHp: Short,
      val maxHp: Short,
  ) : BattleEventBody

  /** Event 0x40, BattleMoveFailed. [moveId] names the move in the "it failed" message. */
  data class MoveFailed(val moveId: Short) : BattleEventBody

  /**
   * Event 2, the non-volatile status change (client f/Et0): [kind] 0 sets the target's status
   * byte to [status] and prints the inflicted line when a status appears or the cured line when
   * it clears (the client compares the old and new bytes itself); [status] uses the record's
   * encoding (sleep turns in the low bits, poison 8, burn 16, freeze 32, paralysis 64, toxic 128).
   * [aux] is the trailing short the client stores unused on this path.
   */
  data class StatusChange(val status: Byte, val kind: Byte = 0, val aux: Short = 0) :
      BattleEventBody

  /**
   * Event 12, the field weather (client f/W31): one byte looked up through the client's weather
   * enum (f/xG1.U00). 0 clears the weather.
   */
  data class WeatherChange(val weather: Byte) : BattleEventBody

  /**
   * Event 92 (client f/G61 -> QL1.Ww0): fades the target's sprite out while it is out of reach
   * (Fly, Dig, Dive, Bounce) and back in when it strikes. One boolean.
   */
  data class Visibility(val hidden: Boolean) : BattleEventBody

  /**
   * One of the client's fixed-shape battle lines, by sub-event id. Decoded from the client's
   * event factory (f/SF1) and each renderer's message bases against the ROM text bank the client
   * itself dumped (reference/client-31914/battle-strings.tsv). [values] are the body fields in
   * wire order; [BattleLine] fixes the shape.
   */
  data class Line(val line: BattleLine, val values: List<Int> = emptyList()) : BattleEventBody
}

/** Field shapes of the fixed battle lines. */
enum class LineShape {
  EMPTY,
  BYTE,
  SHORT,
  BYTE_SHORT,
  SHORT_BYTE_SHORT,
  BYTE_SHORT_SHORT,
}

/**
 * Battle sub-events that print one line (and, where noted, move an hp bar). The comment is the
 * client's text for the first variant; `{00}` is the target's name.
 */
enum class BattleLine(val id: Int, val shape: LineShape) {
  /** 3 f/jK: "{00} was hurt by its burn!" with the new hp. */
  BURN_DAMAGE(3, LineShape.SHORT),
  /** 4 f/s20: "{00} flinched and couldn't move!" */
  FLINCHED(4, LineShape.EMPTY),
  /** 5 f/TG1: true "{00} was frozen solid!" with the freeze animation, false "{00} thawed out!" */
  FREEZE(5, LineShape.BYTE),
  /** 6 f/W0: "{00} was hurt by poison!" with the new hp. */
  POISON_DAMAGE(6, LineShape.SHORT),
  /** 7 f/Kz: false "{00} is fast asleep.", true "{00} woke up!" */
  SLEEP(7, LineShape.BYTE),
  /** 8 f/uO0: false "{00} is paralyzed! It can't move!" */
  PARALYZED(8, LineShape.BYTE),
  /** 10 f/L0: 0 became confused, 1 snapped out, 2 is confused and hurt itself, 3 is confused, 4 already. */
  CONFUSION(10, LineShape.BYTE),
  /** 13 f/uC: weather byte (sandstorm 3, else hail) and the new hp: "{00} is buffeted by the sandstorm!" */
  WEATHER_DAMAGE(13, LineShape.BYTE_SHORT),
  /** 14 f/j21: new hp, packed position of the healed monster (0xFF none), its hp: "sapped by Leech Seed!" */
  LEECH_SEED_DRAIN(14, LineShape.SHORT_BYTE_SHORT),
  /** 18 f/mv: 0 and the new hp: "{00} is afflicted by the curse!" */
  CURSE_DAMAGE(18, LineShape.BYTE_SHORT),
  /** 19 f/LPt4: new hp: "{00} is locked in a nightmare!" */
  NIGHTMARE_DAMAGE(19, LineShape.SHORT),
  /** 22 f/AN: 0 "{00} endured the hit!" */
  ENDURED(22, LineShape.BYTE),
  /** 24 f/N00: 1 hurt by the trapping move (hp, move id), 2 freed from it. */
  TRAP(24, LineShape.BYTE_SHORT_SHORT),
  /** 27 f/qw: 0 "{00} was seeded!" */
  SEEDED(27, LineShape.BYTE),
  /** 34 f/q21: "{00} grew drowsy!" */
  DROWSY(34, LineShape.EMPTY),
  /** 37 f/K60: "{00} can no longer escape!" */
  NO_ESCAPE(37, LineShape.EMPTY),
  /** 38 f/cV0: "{00} planted its roots!" */
  ROOTED(38, LineShape.EMPTY),
  /** 39 f/VJ0: new hp: "{00} absorbed nutrients with its roots!" */
  ROOT_HEAL(39, LineShape.SHORT),
  /** 73 f/eh1: move id: "{00} was identified!" */
  IDENTIFIED(73, LineShape.SHORT),
  /** 126 f/wT0: "{00}'s stat changes were removed!" */
  STATS_CLEARED(126, LineShape.EMPTY);

  companion object {
    fun ofId(id: Int): BattleLine? = entries.firstOrNull { it.id == id }
  }
}

private class LineBodyCodec(private val line: BattleLine) : PacketCodec<BattleEventBody>() {
  override fun CodecScope<BattleEventBody>.body(): BattleEventBody {
    fun value(i: Int): (BattleEventBody) -> Int = { (it as BattleEventBody.Line).values.getOrElse(i) { 0 } }
    val values =
        when (line.shape) {
          LineShape.EMPTY -> emptyList()
          LineShape.BYTE -> listOf(field(S8) { value(0)(it).toByte() }.toInt())
          LineShape.SHORT -> listOf(field(S16LE) { value(0)(it).toShort() }.toInt())
          LineShape.BYTE_SHORT ->
              listOf(
                  field(S8) { value(0)(it).toByte() }.toInt(),
                  field(S16LE) { value(1)(it).toShort() }.toInt())
          LineShape.SHORT_BYTE_SHORT ->
              listOf(
                  field(S16LE) { value(0)(it).toShort() }.toInt(),
                  field(S8) { value(1)(it).toByte() }.toInt(),
                  field(S16LE) { value(2)(it).toShort() }.toInt())
          LineShape.BYTE_SHORT_SHORT ->
              listOf(
                  field(S8) { value(0)(it).toByte() }.toInt(),
                  field(S16LE) { value(1)(it).toShort() }.toInt(),
                  field(S16LE) { value(2)(it).toShort() }.toInt())
        }
    return BattleEventBody.Line(line, values)
  }
}

private val VisibilityBodyCodec: Codec<BattleEventBody> =
    object : PacketCodec<BattleEventBody>() {
      override fun CodecScope<BattleEventBody>.body(): BattleEventBody {
        val hidden = field(Bool) { (it as BattleEventBody.Visibility).hidden }
        return BattleEventBody.Visibility(hidden)
      }
    }

private val WeatherChangeBodyCodec: Codec<BattleEventBody> =
    object : PacketCodec<BattleEventBody>() {
      override fun CodecScope<BattleEventBody>.body(): BattleEventBody {
        val weather = field(S8) { (it as BattleEventBody.WeatherChange).weather }
        return BattleEventBody.WeatherChange(weather)
      }
    }

private val StatusChangeBodyCodec: Codec<BattleEventBody> =
    object : PacketCodec<BattleEventBody>() {
      override fun CodecScope<BattleEventBody>.body(): BattleEventBody {
        val kind = field(S8) { (it as BattleEventBody.StatusChange).kind }
        val status = field(S8) { (it as BattleEventBody.StatusChange).status }
        val aux = field(S16LE) { (it as BattleEventBody.StatusChange).aux }
        return BattleEventBody.StatusChange(status, kind, aux)
      }
    }

private val HpUpdateBodyCodec: Codec<BattleEventBody> =
    object : PacketCodec<BattleEventBody>() {
      override fun CodecScope<BattleEventBody>.body(): BattleEventBody {
        val currentHp = field(S16LE) { (it as BattleEventBody.HpUpdate).currentHp }
        return BattleEventBody.HpUpdate(currentHp)
      }
    }

private const val STAT_INDEX_MASK = 0x7F

private val StatChangeBodyCodec: Codec<BattleEventBody> =
    object : PacketCodec<BattleEventBody>() {
      override fun CodecScope<BattleEventBody>.body(): BattleEventBody {
        val changeType = field(S8) { (it as BattleEventBody.StatChange).changeType }
        // Only the low seven bits name the stat. The top bit is a separate emphasis flag, and the
        // direction rides in the signed stage count rather than here.
        val stat = field(S8) { (it as BattleEventBody.StatChange).stat }
        val stages = field(S8) { (it as BattleEventBody.StatChange).stageDelta.toByte() }
        // 0xFF in every capture.
        reserved(0xFF)
        return BattleEventBody.StatChange(
            (stat.toInt() and STAT_INDEX_MASK).toByte(), stages.toShort(), changeType)
      }
    }

private val FaintBodyCodec: Codec<BattleEventBody> =
    object : PacketCodec<BattleEventBody>() {
      override fun CodecScope<BattleEventBody>.body(): BattleEventBody {
        val playFaintAnimation = field(Bool) { (it as BattleEventBody.Faint).playFaintAnimation }
        return BattleEventBody.Faint(playFaintAnimation)
      }
    }

private val EffectivenessMessageBodyCodec: Codec<BattleEventBody> =
    object : PacketCodec<BattleEventBody>() {
      override fun CodecScope<BattleEventBody>.body(): BattleEventBody =
          BattleEventBody.EffectivenessMessage
    }

private val EvolutionBodyCodec: Codec<BattleEventBody> =
    object : PacketCodec<BattleEventBody>() {
      override fun CodecScope<BattleEventBody>.body(): BattleEventBody {
        val species = field(S16LE) { (it as BattleEventBody.Evolution).species }
        // The client reads and discards one byte here.
        reserved(0)
        val form = field(S16LE) { (it as BattleEventBody.Evolution).form }
        val currentHp = field(S16LE) { (it as BattleEventBody.Evolution).currentHp }
        val maxHp = field(S16LE) { (it as BattleEventBody.Evolution).maxHp }
        return BattleEventBody.Evolution(species, form, currentHp, maxHp)
      }
    }

private val MoveFailedBodyCodec: Codec<BattleEventBody> =
    object : PacketCodec<BattleEventBody>() {
      override fun CodecScope<BattleEventBody>.body(): BattleEventBody {
        val moveId = field(S16LE) { (it as BattleEventBody.MoveFailed).moveId }
        return BattleEventBody.MoveFailed(moveId)
      }
    }

/**
 * A nested battle action event's type, the client's event id. Each type holds the codec for its
 * body. The enum grows as we capture more of the client's event types.
 */
enum class BattleEventType(val id: Int, val codec: Codec<BattleEventBody>) {
  HP_UPDATE(id = 0, codec = HpUpdateBodyCodec),
  STAT_CHANGE(id = 1, codec = StatChangeBodyCodec),
  STATUS_CHANGE(id = 2, codec = StatusChangeBodyCodec),
  WEATHER_CHANGE(id = 12, codec = WeatherChangeBodyCodec),
  VISIBILITY(id = 92, codec = VisibilityBodyCodec),
  EFFECTIVENESS_MESSAGE(id = 4, codec = EffectivenessMessageBodyCodec),
  POKEMON_FAINTED(id = 5, codec = FaintBodyCodec),
  MOVE_FAILED(id = 0x40, codec = MoveFailedBodyCodec),
  EVOLUTION(id = 107, codec = EvolutionBodyCodec);

  companion object {
    fun ofId(id: Int): BattleEventType = entries.first { it.id == id }

    fun ofBody(body: BattleEventBody): BattleEventType =
        when (body) {
          is BattleEventBody.HpUpdate -> HP_UPDATE
          is BattleEventBody.StatChange -> STAT_CHANGE
          is BattleEventBody.EffectivenessMessage -> EFFECTIVENESS_MESSAGE
          is BattleEventBody.Faint -> POKEMON_FAINTED
          is BattleEventBody.MoveFailed -> MOVE_FAILED
          is BattleEventBody.Evolution -> EVOLUTION
          is BattleEventBody.StatusChange -> STATUS_CHANGE
          is BattleEventBody.WeatherChange -> WEATHER_CHANGE
          is BattleEventBody.Visibility -> VISIBILITY
          is BattleEventBody.Line -> error("lines are written by id, not by type")
        }
  }
}

/** The wire id and codec of a body: the typed enum for the decoded events, the line table otherwise. */
private fun bodyId(body: BattleEventBody): Int =
    if (body is BattleEventBody.Line) body.line.id else BattleEventType.ofBody(body).id

private fun bodyCodec(id: Int): Codec<BattleEventBody> {
  BattleLine.ofId(id)?.let { return LineBodyCodec(it) }
  return BattleEventType.ofId(id).codec
}

private const val FLAG_ENTITY_A = 0x01
private const val FLAG_ENTITY_B = 0x02

/**
 * Header flag bit the hp-update renderer (f/hD1, `DZ.K91(8)`) treats as "quiet": the bar moves
 * but neither the faint line nor "HP was restored" is printed.
 */
const val EVENT_FLAG_QUIET_HP = 0x08

/**
 * One nested battle action event. The variable header is a type id, a flags byte, then the entity
 * ids the low two flag bits select. [entityA] and [entityB] are present only when their bit is
 * set; [flags] carries any further header bits (see [EVENT_FLAG_CRITICAL_HIT]).
 */
data class BattleActionEvent(
    val entityA: Long?,
    val entityB: Long?,
    val body: BattleEventBody,
    val flags: Int = 0,
)

private val BattleActionEventCodec: Codec<BattleActionEvent> =
    object : PacketCodec<BattleActionEvent>() {
      override fun CodecScope<BattleActionEvent>.body(): BattleActionEvent {
        val typeId = field(U8) { bodyId(it.body) }
        val aux =
            field(U8) {
              var flags = it.flags and (FLAG_ENTITY_A or FLAG_ENTITY_B).inv()
              if (it.entityA != null) flags = flags or FLAG_ENTITY_A
              if (it.entityB != null) flags = flags or FLAG_ENTITY_B
              flags
            }
        val entityA = optionalField((aux and FLAG_ENTITY_A) != 0, S64LE) { it.entityA }
        val entityB = optionalField((aux and FLAG_ENTITY_B) != 0, S64LE) { it.entityB }
        val body = field(bodyCodec(typeId)) { it.body }
        return BattleActionEvent(entityA, entityB, body, aux and (FLAG_ENTITY_A or FLAG_ENTITY_B).inv())
      }
    }

/** One mon a move affected: its entity, its move short, and the events applied to it. */
data class BattleEffectTarget(
    val entityId: Long,
    val targetMove: Short,
    val subEvents: List<BattleActionEvent>,
)

object BattleEffectTargetCodec : PacketCodec<BattleEffectTarget>() {
  override fun CodecScope<BattleEffectTarget>.body(): BattleEffectTarget {
    val entityId = field(S64LE) { it.entityId }
    val targetMove = field(S16LE) { it.targetMove }
    val subEvents = field(BattleActionEventCodec.listPrefixed(U8)) { it.subEvents }
    return BattleEffectTarget(entityId, targetMove, subEvents)
  }
}

data class BattleEntityMoveEventPacket(
    val sourceEntity: Long,
    val sourceMove: Short,
    val kind: Byte,
    val targets: List<BattleEffectTarget>,
)

object BattleEntityMoveEventPacketCodec : PacketCodec<BattleEntityMoveEventPacket>() {
  override fun CodecScope<BattleEntityMoveEventPacket>.body(): BattleEntityMoveEventPacket {
    val sourceEntity = field(S64LE) { it.sourceEntity }
    val sourceMove = field(S16LE) { it.sourceMove }
    val kind = field(S8) { it.kind }
    val targets = field(BattleEffectTargetCodec.listPrefixed(U8)) { it.targets }
    return BattleEntityMoveEventPacket(sourceEntity, sourceMove, kind, targets)
  }
}
