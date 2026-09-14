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
  /**
   * Event 1, the stat stage change (client f/w71, decoded 2026-09-06): [changeType] 0 for a plain
   * change (1 and 2 print special lines), the stat with bit 0x80 set so the line is printed, the
   * signed [stageDelta] (magnitude picks rose / sharply / drastically, sign picks fell), and a
   * fourth byte the animation takes as its direction: the delta again when the change [applied],
   * 0 when the stat could not go further ("won't go any higher"). A constant 0xFF there - what
   * the captures of stat DROPS showed - animated every rise as a fall.
   */
  data class StatChange(
      val stat: Byte,
      val stageDelta: Short,
      val changeType: Byte = 0,
      val applied: Boolean = true,
  ) : BattleEventBody

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
   * The Safari Zone's bait event, client kind -33 (f/h31): 0 "{00} is eating!" with the eating
   * animation, 1 "There's too many {00}! The bait would be wasted!", 2 "{00} can't eat more!",
   * 3 / 4 / 5 "{thrower} tossed a Mellow / Hearty / Giant Bait to {00}!" with the toss animation;
   * the thrower's name follows the kind byte for 3..5 only.
   */
  data class SafariBait(val kind: Byte, val thrower: String? = null) : BattleEventBody

  /**
   * Kind 76 (f/lW1) printing one of the client's own strings_en.xml strings by id: [shape u8]
   * [1 u8][flag u8][id s32] (the flag is f/lW1.Ff1, an index remap used by the bank form only). Shape 1 fills {00} with the name of the monster the event is attached to
   * (the same parameter the bait event names), shape 2 with the other side's, 3/7 both, 0 none.
   * The kind's other form ([shape][0][bank s16][index s16]) reads a text bank, but with the ROM
   * byte hard-coded to the Unova game in all 272 call sites, so only the string form is modelled.
   */
  data class ClientLine(val shape: Byte, val stringId: Int) : BattleEventBody

  /**
   * Kind -22 (f/wl, bytecode-verified 2026-09-10): [text UTF-16][entity s64]. The client prints its
   * string 16804143 with {00} = [text] straight from the packet, {01} = the entity's trainer's
   * monster and {02} = the first monster on that side, with no animation. Stock 16804143 is a
   * seasonal event line ("{00}'s {01} is being controlled by the {02}!"); the launcher restages it
   * as a bare "{00}" so the server can print any sentence with the player's name - FireRed's
   * "{player} used {ball}!" before a throw. [entityId] must be a monster on the field (the
   * client looks its side up and returns silently otherwise).
   */
  data class FreeLine(val text: String, val entityId: Long) : BattleEventBody

  /**
   * One of the client's fixed-shape battle lines, by sub-event id. Decoded from the client's
   * event factory (f/SF1) and each renderer's message bases against the ROM text bank the client
   * itself dumped (reference/client-31914/battle-strings.tsv). [values] are the body fields in
   * wire order; [BattleLine] fixes the shape.
   */
  data class Line(val line: BattleLine, val values: List<Int> = emptyList()) : BattleEventBody

  /**
   * Event 51, the ability activation (client f/sL): shows the "{00}'s {ability}" banner on the
   * monster's panel and, for abilities the client knows a line for (keyed by ability id: Limber,
   * Insomnia, Immunity, Water Veil, Magma Armor, Pressure, Mold Breaker, Trace...), prints it.
   * [kind] bits: 1 = [other] names a second monster, 2 = [moveId] names a move, 8 = [itemId] an
   * item. [abilityId] is the ability's ordinal (the client resolves its name from string
   * 210000 + id).
   */
  data class AbilityPopup(
      val abilityId: Int,
      val kind: Int = 0,
      val self: Long = 0,
      val other: Long = 0,
      val moveId: Int = 0,
      val itemId: Int = 0,
  ) : BattleEventBody

  /**
   * Event 31, Transform (r32645 f/cb, bytecode-verified 2026-09-13). Sent with entityA = the
   * monster copied and entityB = the one transforming; the client rebuilds the transformer as
   * [species] with these moves (pp min(move pp, 5)), ability, types and stages, redraws its
   * sprite and panel and prints "{00} transformed into {01}!". [personality] is on the wire only
   * for Spinda (327). [rarityFlags] is the record flag word (&9 shiny); [gender] -128 keeps the
   * transformer's own. [stages] packs eight nibbles, low first, of stage + 6: hp, attack,
   * defense, speed, sp. attack, sp. defense, accuracy, evasion. Types are PokemonType ordinals.
   */
  data class Transform(
      val species: Short,
      val form: Byte = 0,
      val personality: Int = 0,
      val moves: List<Short>,
      val abilityId: Short,
      val rarityFlags: Short = 0,
      val gender: Byte = -128,
      val stages: Int = NEUTRAL_STAGES,
      val type1: Byte,
      val type2: Byte,
  ) : BattleEventBody {
    companion object {
      const val SPINDA: Short = 327
      const val NEUTRAL_STAGES = 0x66666666
    }
  }

  /**
   * Event 33 (r32645 f/wz2, bytecode-verified 2026-09-13), with entityA = the target and entityB =
   * the attacker: move 46 (Roar) prints "{target} fled from battle!", any other move "{attacker}
   * blew away {target}!". Text only - ending the battle or bringing a replacement is up to the server.
   */
  data class BlownAway(val moveId: Short) : BattleEventBody
}

private val TransformBodyCodec: Codec<BattleEventBody> =
    object : PacketCodec<BattleEventBody>() {
      override fun CodecScope<BattleEventBody>.body(): BattleEventBody {
        val species = field(S16LE) { (it as BattleEventBody.Transform).species }
        val form = field(S8) { (it as BattleEventBody.Transform).form }
        val personality =
            if (species == BattleEventBody.Transform.SPINDA) field(S32LE) { (it as BattleEventBody.Transform).personality } else 0
        val moves = List(4) { i -> field(S16LE) { (it as BattleEventBody.Transform).moves.getOrElse(i) { 0 } } }
        val ability = field(S16LE) { (it as BattleEventBody.Transform).abilityId }
        val rarity = field(S16LE) { (it as BattleEventBody.Transform).rarityFlags }
        val gender = field(S8) { (it as BattleEventBody.Transform).gender }
        val stages = field(S32LE) { (it as BattleEventBody.Transform).stages }
        val type1 = field(S8) { (it as BattleEventBody.Transform).type1 }
        val type2 = field(S8) { (it as BattleEventBody.Transform).type2 }
        return BattleEventBody.Transform(species, form, personality, moves, ability, rarity, gender, stages, type1, type2)
      }
    }

private val BlownAwayBodyCodec: Codec<BattleEventBody> =
    object : PacketCodec<BattleEventBody>() {
      override fun CodecScope<BattleEventBody>.body(): BattleEventBody {
        val moveId = field(S16LE) { (it as BattleEventBody.BlownAway).moveId }
        return BattleEventBody.BlownAway(moveId)
      }
    }

private val AbilityPopupBodyCodec: Codec<BattleEventBody> =
    object : PacketCodec<BattleEventBody>() {
      override fun CodecScope<BattleEventBody>.body(): BattleEventBody {
        val kind = field(S8) { (it as BattleEventBody.AbilityPopup).kind.toByte() }.toInt()
        val ability = field(S16LE) { (it as BattleEventBody.AbilityPopup).abilityId.toShort() }.toInt()
        val self = field(S64LE) { (it as BattleEventBody.AbilityPopup).self }
        // Bytecode-verified (f/SF1 event 51 -> f/sL): the rest is conditional on the kind bits. Bit 1
        // adds the second monster; bit 2 adds the move, else bit 8 adds the item (one short, never
        // both); bit 4 adds one more short the server never sets. Writing every field regardless
        // left surplus bytes that the client parsed as the next event - Intimidate on a horde's
        // third switch-in ended in a buffer underflow on the client (2026-09-06).
        val other = if (kind and 1 != 0) field(S64LE) { (it as BattleEventBody.AbilityPopup).other } else 0L
        val moveId = if (kind and 2 != 0) field(S16LE) { (it as BattleEventBody.AbilityPopup).moveId.toShort() }.toInt() else 0
        val itemId = if (kind and 2 == 0 && kind and 8 != 0) field(S16LE) { (it as BattleEventBody.AbilityPopup).itemId.toShort() }.toInt() else 0
        if (kind and 4 != 0) field(S16LE) { 0 }
        return BattleEventBody.AbilityPopup(ability, kind, self, other, moveId, itemId)
      }
    }

/** Field shapes of the fixed battle lines. */
enum class LineShape {
  EMPTY,
  BYTE,
  SHORT,
  BYTE_SHORT,
  SHORT_SHORT,
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
  /** 22 f/AN: kind 0 "endured the hit", 1 Focus Band, 2 the ability (Sturdy), 3 Focus Sash "hung on using its {01}". */
  ENDURED(22, LineShape.BYTE),
  /** 24 f/N00: 1 hurt by the trapping move, 2 freed from it; then the MOVE id, then the new hp (bytecode-verified order). */
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
  STATS_CLEARED(126, LineShape.EMPTY),
  /** 52 f/I51: item id; clears the status itself and prints "{00}'s {01} cured its poison!" etc. */
  ITEM_CURED_STATUS(52, LineShape.SHORT),
  /** 82 f/mO1: item id: "{00} restored its status using its {01}!" */
  ITEM_RESTORED_STATUS(82, LineShape.SHORT),
  /**
   * 55: ITEM id, then the new hp: "{00} restored its health using its {01}!" (r32645 f/ko1 reads two
   * shorts into f/fw0(item, hp), bytecode 2026-09-14). Sent hp-first, the berry's hp printed as an
   * item (22 = Parlyz Heal) and the item id became the hp the bar animated towards.
   */
  ITEM_HEAL(55, LineShape.SHORT_SHORT),
  /** 83 f/Ph1: new hp, item id: "{00} restored a little HP using its {01}!" */
  ITEM_HEAL_SMALL(83, LineShape.SHORT_SHORT),
  /** 114 f/we0: kind (0 own item, 1 the other's), item id: "{00} is hurt by its {01}!" */
  ITEM_HURT(114, LineShape.BYTE_SHORT),
  /** 88 f/Bq: "{00} is hurt by its Life Orb!" (the item is fixed in the client). */
  LIFE_ORB_HURT(88, LineShape.EMPTY),
  /** 116 f/VQ0: item id: "{00}'s {01} let it move first!" */
  ITEM_MOVED_FIRST(116, LineShape.SHORT),
  /** 70 f/Bc: item id: "{00} found one {01}!" */
  ITEM_FOUND(70, LineShape.SHORT),
  /** 90 f/Pt0: item id: "{00} stole and ate its target's {01}!" */
  ITEM_STOLE_ATE(90, LineShape.SHORT),
  /** 96 f/Xn1: item id: "{00}'s {01} was burnt up!" */
  ITEM_BURNT(96, LineShape.SHORT),
  /** 71 f/vg0: item id: "{00} knocked off {01}'s {02}!" (attacker and target from the packet). */
  ITEM_KNOCKED_OFF(71, LineShape.SHORT),
  /** 99 f/sN0: item id: "{00} received {02} from {01}!" */
  ITEM_RECEIVED(99, LineShape.SHORT),
  /** 68 f/lM0: two shorts: "{00} switched items with its target!" / "obtained one {01}". */
  ITEM_SWAPPED(68, LineShape.SHORT_SHORT);

  companion object {
    fun ofId(id: Int): BattleLine? = entries.firstOrNull { it.id == id }
  }
}

/** [BattleLine.TRAP]'s kind that carries the new hp. */
private const val TRAP_HURT = 1

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
          LineShape.SHORT_SHORT ->
              listOf(
                  field(S16LE) { value(0)(it).toShort() }.toInt(),
                  field(S16LE) { value(1)(it).toShort() }.toInt())
          LineShape.SHORT_BYTE_SHORT ->
              listOf(
                  field(S16LE) { value(0)(it).toShort() }.toInt(),
                  field(S8) { value(1)(it).toByte() }.toInt(),
                  field(S16LE) { value(2)(it).toShort() }.toInt())
          LineShape.BYTE_SHORT_SHORT -> {
            val kind = field(S8) { value(0)(it).toByte() }.toInt()
            val second = field(S16LE) { value(1)(it).toShort() }.toInt()
            // r32645 f/ko1 case 24 reads the trap's hp short only for kind 1 (hurt); a "freed"
            // line (kind 2) ends after the move id, so writing the hp there shifts every later byte.
            if (line == BattleLine.TRAP && kind != TRAP_HURT) listOf(kind, second)
            else listOf(kind, second, field(S16LE) { value(2)(it).toShort() }.toInt())
          }
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
        // Bytecode-verified (f/SF1 event 2 -> f/Et0): the short follows only when the status byte
        // is 1, and kind 3 adds an entity id (never sent). Writing it always left two surplus bytes.
        val aux = if (status.toInt() == 1) field(S16LE) { (it as BattleEventBody.StatusChange).aux } else 0
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
        // Low seven bits name the stat (client f/RC0 order). Bit 0x80 SUPPRESSES the stat animation
        // (client SF1 event 1 animates only when it is clear; the text line prints either way), so it
        // stays clear - the retail Growl capture has it clear too.
        val stat = field(S8) { ((it as BattleEventBody.StatChange).stat.toInt() and STAT_INDEX_MASK).toByte() }
        val stages = field(S8) { (it as BattleEventBody.StatChange).stageDelta.toByte() }
        // The animation's direction byte: the delta when the change landed, 0 when it could not.
        val direction = field(S8) { val c = it as BattleEventBody.StatChange; if (c.applied) c.stageDelta.toByte() else 0 }
        return BattleEventBody.StatChange(
            (stat.toInt() and STAT_INDEX_MASK).toByte(), stages.toShort(), changeType, direction.toInt() != 0)
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
private val SafariBaitBodyCodec: Codec<BattleEventBody> =
    object : PacketCodec<BattleEventBody>() {
      override fun CodecScope<BattleEventBody>.body(): BattleEventBody {
        val kind = field(S8) { (it as BattleEventBody.SafariBait).kind }
        val thrower =
            if (kind in 3..5) field(Utf16LeNullTerminated) { (it as BattleEventBody.SafariBait).thrower.orEmpty() } else null
        return BattleEventBody.SafariBait(kind, thrower)
      }
    }

private val FreeLineBodyCodec: Codec<BattleEventBody> =
    object : PacketCodec<BattleEventBody>() {
      override fun CodecScope<BattleEventBody>.body(): BattleEventBody {
        val text = field(Utf16LeNullTerminated) { (it as BattleEventBody.FreeLine).text }
        val entityId = field(S64LE) { (it as BattleEventBody.FreeLine).entityId }
        return BattleEventBody.FreeLine(text, entityId)
      }
    }

private val ClientLineBodyCodec: Codec<BattleEventBody> =
    object : PacketCodec<BattleEventBody>() {
      override fun CodecScope<BattleEventBody>.body(): BattleEventBody {
        // f/ko1 kind 76 reads exactly three bytes (shape, form, flag) and then the int string id when
        // the form is non-zero. A fourth byte here made every packet carrying the line underflow on
        // the client ("Buffer underflow for wF1 0x33"), dropping the whole group (2026-09-14).
        val shape = field(S8) { (it as BattleEventBody.ClientLine).shape }
        val form = field(S8) { 1 }
        require(form.toInt() == 1) { "kind 76 bank-line form is not modelled" }
        field(S8) { 1 }
        val id = field(S32LE) { (it as BattleEventBody.ClientLine).stringId }
        return BattleEventBody.ClientLine(shape, id)
      }
    }

enum class BattleEventType(val id: Int, val codec: Codec<BattleEventBody>) {
  HP_UPDATE(id = 0, codec = HpUpdateBodyCodec),
  STAT_CHANGE(id = 1, codec = StatChangeBodyCodec),
  STATUS_CHANGE(id = 2, codec = StatusChangeBodyCodec),
  WEATHER_CHANGE(id = 12, codec = WeatherChangeBodyCodec),
  VISIBILITY(id = 92, codec = VisibilityBodyCodec),
  ABILITY_POPUP(id = 51, codec = AbilityPopupBodyCodec),
  EFFECTIVENESS_MESSAGE(id = 4, codec = EffectivenessMessageBodyCodec),
  POKEMON_FAINTED(id = 5, codec = FaintBodyCodec),
  MOVE_FAILED(id = 0x40, codec = MoveFailedBodyCodec),
  EVOLUTION(id = 107, codec = EvolutionBodyCodec),
  SAFARI_BAIT(id = -33, codec = SafariBaitBodyCodec),
  CLIENT_LINE(id = 76, codec = ClientLineBodyCodec),
  FREE_LINE(id = -22, codec = FreeLineBodyCodec),
  TRANSFORM(id = 31, codec = TransformBodyCodec),
  BLOWN_AWAY(id = 33, codec = BlownAwayBodyCodec);

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
          is BattleEventBody.AbilityPopup -> ABILITY_POPUP
          is BattleEventBody.SafariBait -> SAFARI_BAIT
          is BattleEventBody.ClientLine -> CLIENT_LINE
          is BattleEventBody.FreeLine -> FREE_LINE
          is BattleEventBody.Transform -> TRANSFORM
          is BattleEventBody.BlownAway -> BLOWN_AWAY
          is BattleEventBody.Line -> error("lines are written by id, not by type")
        }
  }
}

/** The wire id and codec of a body: the typed enum for the decoded events, the line table otherwise. */
private fun bodyId(body: BattleEventBody): Int =
    if (body is BattleEventBody.Line) body.line.id else BattleEventType.ofBody(body).id

/**
 * A line writes with its own line codec and a typed body with its type's; reading prefers the
 * typed table (the client's dedicated bodies) and falls back to the line table. Ids may appear in
 * both when a line and a typed body share a client event.
 */
private class BodyCodec(private val id: Int) : Codec<BattleEventBody> {
  override fun read(buf: de.fiereu.bytecodec.ReadBuffer): BattleEventBody {
    BattleEventType.entries.firstOrNull { it.id == id }?.let { return it.codec.read(buf) }
    return LineBodyCodec(BattleLine.ofId(id) ?: error("unknown battle event $id")).read(buf)
  }

  override fun write(buf: de.fiereu.bytecodec.WriteBuffer, value: BattleEventBody) {
    if (value is BattleEventBody.Line) LineBodyCodec(value.line).write(buf, value)
    else BattleEventType.ofBody(value).codec.write(buf, value)
  }
}

private fun bodyCodec(id: Int): Codec<BattleEventBody> = BodyCodec(id)

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
        // The client switches on a SIGNED kind byte (f/SF1 tableswitch -42..127): -33 is the bait event.
        val typeId = field(S8) { bodyId(it.body).toByte() }.toInt()
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
