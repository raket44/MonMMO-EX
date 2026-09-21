package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.net.game.packets.PokemonContainerPacket
import de.fiereu.openmmo.net.game.packets.ServerMessagePacket
import de.fiereu.openmmo.net.game.packets.ServerNoticePacket
import de.fiereu.openmmo.server.game.storage.CharacterStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends candidate packets so an admin can watch what the retail client does with them - the
 * runtime-probe arm of verify-against-client. Currently hunting: which ServerNotice type renders as
 * an on-screen popup (for item pickups), which ServerMessage string ids exist, and whether a PC
 * container push opens the client's own storage UI.
 */
@Singleton
class DeveloperProbeCommand
@Inject
constructor(
    private val characterStore: CharacterStore,
    private val dialogService: de.fiereu.openmmo.server.game.services.DialogService,
    private val mapLoadService: de.fiereu.openmmo.server.game.services.MapLoadService,
) : ChatCommand {
  override val name = "probe"
  override val usage =
      "/probe <transport <n>|notice <type> [text]|msg <id> [argType] [text]|dialog <textId>|menu <kind>|storyflag <region> <id> [value]|pc|pcwin|world <action> <subject> [shorts]|door <open> <x> <y> [kind] [arg]|entityaction <action>>"
  override val description = "sends one candidate packet to see how the client renders it"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    when (ctx.args.getOrNull(0)?.lowercase()) {
      // Re-sends your own LoadEntity with a chosen transportation byte - the runtime probe for
      // finding which value means bike / surf / run-enabled. We always send 0; the retail server
      // clearly uses others.
      // Fires an empty body at every unmapped s2c opcode; the CLIENT log then names the packet
      // class behind each ("Reading failed for packet f/XX"), mapping the ids in one shot.
      // One probe per call: a batch of them desynced the compressed stream. The client log
      // names the packet class it tried to parse for the id ("/probe op 6a").
      "op" -> {
        val packet =
            when (ctx.args.getOrNull(1)?.lowercase()) {
              "06" -> de.fiereu.openmmo.net.game.packets.Probe06Packet()
              "6a" -> de.fiereu.openmmo.net.game.packets.Probe6APacket()
              "82" -> de.fiereu.openmmo.net.game.packets.Probe82Packet()
              "8a" -> de.fiereu.openmmo.net.game.packets.Probe8APacket()
              "9f" -> de.fiereu.openmmo.net.game.packets.Probe9FPacket()
              "af" -> de.fiereu.openmmo.net.game.packets.ProbeAFPacket()
              else -> {
                ctx.reply("/probe op <06|6a|82|8a|9f|af>")
                return
              }
            }
        ctx.session.send(packet)
        ctx.reply("Probed - check what happened on screen and in console.log")
        return
      }
      "transport" -> {
        val value = ctx.args.getOrNull(1)?.toIntOrNull()
        if (value == null || value !in 0..255) {
          ctx.reply("/probe transport <0-255>")
          return
        }
        val stored = characterStore.getCharacter(ctx.characterId)
        if (stored == null) {
          ctx.reply("No character.")
          return
        }
        // The client ignores a re-sent LoadEntity for an entity it already holds, so the value is
        // remembered and shows on the next fresh spawn - walk through any door to see it.
        ctx.state.transportOverride = value
        ctx.session.send(
            mapLoadService.createLoadEntity(
                stored.info,
                ctx.state.facingDirection,
                ctx.state.elevation,
                party = stored.pokemon,
                skins = stored.skins,
                railLine = ctx.state.pendingRailLine,
                transportation = value,
            ))
        ctx.reply(
            "transportation=$value armed - walk through any door (or relog) and it applies on " +
                "the fresh spawn.")
      }
      // Applies a mount (type, id) to the player LIVE - LoadEntity's flags&0x02 pair reaches
      // f.E41.mX1 even on the existing entity. Known: (1,201)=Regice, (4,201)=Bulbasaur.
      // "/probe mount -1 -1" dismounts.
      "mount" -> {
        val type = ctx.args.getOrNull(1)?.toIntOrNull()
        val id = ctx.args.getOrNull(2)?.toIntOrNull()
        if (type == null || id == null) {
          ctx.reply("/probe mount <type> <id>  (-1 -1 dismounts)")
          return
        }
        ctx.state.mountType = type
        ctx.state.mountId = id
        val stored = characterStore.getCharacter(ctx.characterId) ?: return
        ctx.session.send(
            mapLoadService.createLoadEntity(
                stored.info,
                ctx.state.facingDirection,
                ctx.state.elevation,
                party = stored.pokemon,
                skins = stored.skins,
                railLine = ctx.state.pendingRailLine,
                mountType = type,
                mountId = id,
            ))
        ctx.state.moveIgnoreUntil = System.currentTimeMillis() + 600
        ctx.reply("Mount applied: type=$type id=$id")
      }
      // The bicycle hypothesis, complete: SkinSlot.BIKE is a paper-doll LAYER drawn only in the
      // RIDING pose (the 80x80 frame set), selected by the spawn transportation bits. This sets
      // both: the BIKE skin to <type> and transportation=2 (ride bit 1), applying on the next
      // fresh spawn - walk through any door. "/probe ride off" clears both.
      "ride" -> {
        // INSTANT, no doors: EntitySpriteChange (0x90) applies a SkinSet to the LOCAL player
        // live. Its trailing byte (E41.fZ0/IL0.an0) turned out to be the GENDER, not a stance -
        // the client's own debug dump labels it so (f/LA0) - and the stored one is sent now.
        // /probe ride <skinType> - "off" clears.
        val stored = characterStore.getCharacter(ctx.characterId) ?: return
        if (ctx.args.getOrNull(1) == "off") {
          characterStore.setSkin(
              ctx.characterId, de.fiereu.openmmo.common.enums.SkinSlot.BIKE, null)
          characterStore.flushCharacterAsync(ctx.characterId)
          val after = characterStore.getCharacter(ctx.characterId) ?: return
          ctx.session.send(
              de.fiereu.openmmo.net.game.packets.EntitySpriteChangePacket(
                  entityId = ctx.characterId,
                  staged = false,
                  appearance =
                      de.fiereu.openmmo.net.game.codecs.SkinSet(
                          after.info.skinRegionSelectionIndex, after.skins),
                  gender = after.info.rivalSex,
              ))
          ctx.reply("Ride cleared instantly.")
          return
        }
        val type = ctx.args.getOrNull(1)?.toIntOrNull()
        if (type == null || type !in 0..1023) {
          ctx.reply("/probe ride <skinType 0-1023 | off>")
          return
        }
        characterStore.setSkin(
            ctx.characterId,
            de.fiereu.openmmo.common.enums.SkinSlot.BIKE,
            de.fiereu.openmmo.common.Skin(
                de.fiereu.openmmo.common.enums.SkinSlot.BIKE, type.toUShort(), 0u))
        characterStore.flushCharacterAsync(ctx.characterId)
        val after = characterStore.getCharacter(ctx.characterId) ?: return
        ctx.session.send(
            de.fiereu.openmmo.net.game.packets.EntitySpriteChangePacket(
                entityId = ctx.characterId,
                staged = false,
                appearance =
                    de.fiereu.openmmo.net.game.codecs.SkinSet(
                        after.info.skinRegionSelectionIndex, after.skins),
                gender = after.info.rivalSex,
            ))
        ctx.reply("Ride applied instantly: bike type=$type")
      }
      // s2c 0xBA, MapTileObjectSlotSet: "put an object in slot <slot> of block <x,y,z>", or clear
      // it. The best candidate for the Pokemon Center heal - the nurse's ROM routine is
      // GetPartySize then CMD_12F <count> (place that many balls), and CMD_23A right after
      // HealPokemon (clear them) - and this is the only decoded packet shaped like a per-slot
      // placement. Never sent by the server, so nothing here can regress; it is here to be watched.
      // "/probe slot <blockX> <blockY> <blockZ> <slot> <present 0|1> [objX] [objY] [objZ] [relative]"
      "slot" -> {
        val bx = ctx.args.getOrNull(1)?.toIntOrNull()
        val by = ctx.args.getOrNull(2)?.toIntOrNull()
        val bz = ctx.args.getOrNull(3)?.toIntOrNull()
        val slot = ctx.args.getOrNull(4)?.toIntOrNull()
        val present = (ctx.args.getOrNull(5)?.toIntOrNull() ?: 1) != 0
        if (bx == null || by == null || bz == null || slot == null) {
          ctx.reply("/probe slot <blockX> <blockY> <blockZ> <slot> <present 0|1> [objX] [objY] [objZ] [relative]")
          return
        }
        val ox = ctx.args.getOrNull(6)?.toIntOrNull() ?: 0
        val oy = ctx.args.getOrNull(7)?.toIntOrNull() ?: 0
        val oz = ctx.args.getOrNull(8)?.toIntOrNull() ?: 0
        val rel = (ctx.args.getOrNull(9)?.toIntOrNull() ?: 1) != 0
        ctx.session.send(
            de.fiereu.openmmo.net.game.packets.MapTileObjectSlotSetPacket(
                bx.toByte(), by.toByte(), bz.toByte(), slot.toByte(), present,
                if (present) ox.toShort() else null,
                if (present) oy.toShort() else null,
                if (present) oz.toByte() else null,
                if (present) rel else null))
        ctx.reply("Sent slot block=($bx,$by,$bz) slot=$slot present=$present obj=($ox,$oy,$oz) rel=$rel")
      }
      // s2c 0xBE, WorldOverlayObjectSet: an overlay object by id with an action byte, a type id and
      // a tile. The other candidate for the heal balls; action 0 clears it.
      // "/probe overlay <objectId> <action> [byteA] [byteB] [typeId] [flag] [x] [y]"
      "overlay" -> {
        val id = ctx.args.getOrNull(1)?.toIntOrNull()
        val action = ctx.args.getOrNull(2)?.toIntOrNull()
        if (id == null || action == null) {
          ctx.reply("/probe overlay <objectId> <action> [byteA] [byteB] [typeId] [flag] [x] [y]")
          return
        }
        val on = action != 0
        fun arg(i: Int, d: Int) = ctx.args.getOrNull(i)?.toIntOrNull() ?: d
        ctx.session.send(
            de.fiereu.openmmo.net.game.packets.WorldOverlayObjectSetPacket(
                id.toByte(), action.toByte(),
                if (on) arg(3, 0).toByte() else null,
                if (on) arg(4, 0).toByte() else null,
                if (on) arg(5, 0).toShort() else null,
                if (on) arg(6, 0).toByte() else null,
                if (on) arg(7, ctx.state.x.toInt()).toShort() else null,
                if (on) arg(8, ctx.state.y.toInt()).toShort() else null))
        ctx.reply("Sent overlay id=$id action=$action")
      }
      // s2c 0xB6 (client f/Ty0), the world-action packet: action byte (cases 0-10, 12-15, 32-35),
      // subject byte, then shorts. The field-move summon animation (ball throw, stand-in popup)
      // must be one of its cases; this fires one so a player can watch what each does.
      // "/probe world <action> <subject> [short ...]"
      "world" -> {
        val action = ctx.args.getOrNull(1)?.toIntOrNull()
        val subject = ctx.args.getOrNull(2)?.toIntOrNull() ?: 0
        if (action == null) {
          ctx.reply("/probe world <action> <subject> [short ...]")
          return
        }
        val args = ctx.args.drop(3).mapNotNull { it.toIntOrNull()?.toShort() }
        ctx.session.send(
            de.fiereu.openmmo.net.game.packets.WorldActionDispatchPacket(action.toByte(), subject.toByte(), args))
        ctx.reply("Sent world action=$action subject=$subject args=$args")
      }
      // s2c 0x1F (client f/wi7), the door animation: "/probe door <open 0|1> <x> <y> [kind] [arg]".
      // kind defaults to the current region, arg to 0 - both go straight into the client's door
      // routine and are not understood yet, so this is how they get found.
      "door" -> {
        val open = ctx.args.getOrNull(1)?.toIntOrNull()
        val x = ctx.args.getOrNull(2)?.toIntOrNull()
        val y = ctx.args.getOrNull(3)?.toIntOrNull()
        if (open == null || x == null || y == null) {
          ctx.reply("/probe door <open 0|1> <x> <y> [kind] [arg]")
          return
        }
        val kind = ctx.args.getOrNull(4)?.toIntOrNull() ?: ctx.state.regionId
        val arg = ctx.args.getOrNull(5)?.toIntOrNull() ?: 0
        ctx.session.send(
            de.fiereu.openmmo.net.game.packets.DoorAnimationPacket(kind.toByte(), open.toShort(), x.toShort(), y.toShort(), arg.toShort()))
        ctx.reply("Sent door open=$open at ($x, $y) kind=$kind arg=$arg")
      }
      // s2c 0xB0 (client f/om1): entity uid + action byte, handed to the active scene.
      // "/probe entityaction <action>" targets your own entity.
      "entityaction" -> {
        val action = ctx.args.getOrNull(1)?.toIntOrNull()
        if (action == null) {
          ctx.reply("/probe entityaction <action>")
          return
        }
        ctx.session.send(
            de.fiereu.openmmo.net.game.packets.SceneEntityActionDispatchPacket(ctx.characterId, action.toByte()))
        ctx.reply("Sent entity action=$action on yourself")
      }
      "notice" -> {
        val type = ctx.args.getOrNull(1)?.toIntOrNull()
        if (type == null) {
          ctx.reply(usage)
          return
        }
        val text = ctx.args.drop(2).joinToString(" ").ifEmpty { "Probe notice type $type" }
        ctx.session.send(ServerNoticePacket(type.toShort(), 0, text))
        ctx.reply("Sent ServerNoticePacket type=$type.")
      }
      // The client-self-population experiment: "/probe npcs off" suppresses this session's npc
      // spawn packets; walk through any door and look. Empty map = NPCs are server-fed;
      // populated map = the client spawns its own from ROM data. "/probe npcs on" restores.
      "npcs" -> {
        val mode = ctx.args.getOrNull(1)?.lowercase()
        if (mode != "on" && mode != "off") {
          ctx.reply("/probe npcs <on|off>")
          return
        }
        ctx.state.suppressNpcSpawns = mode == "off"
        ctx.reply(
            if (mode == "off")
                "NPC spawn packets suppressed - walk through a door and see who is left."
            else "NPC spawn packets restored - re-enter the map to repopulate.")
      }
      // Raw 0x2A story-flag send, bypassing the key mapping: the tool for decoding what each
      // whitelisted client id renders (drawbridge state? gate opening? fly spot?). Stand where
      // the effect would show, flip the id, watch. Ids outside the client's whitelist are
      // rejected client-side with a logged WARN, so probing is safe but pointless there.
      "storyflag" -> {
        val region = ctx.args.getOrNull(1)?.toIntOrNull()
        val id = ctx.args.getOrNull(2)?.toIntOrNull()
        val value = ctx.args.getOrNull(3)?.toIntOrNull() ?: 1
        if (region == null || id == null) {
          ctx.reply("/probe storyflag <region> <id> [value=1]")
          return
        }
        ctx.session.send(
            de.fiereu.openmmo.net.game.packets.StoryFlagUpdatePacket(region.toByte(), id, value))
        val listed = de.fiereu.openmmo.server.game.services.ClientStoryWhitelist.accepts(region, id)
        ctx.reply("Sent 0x2A region=$region id=$id value=$value (whitelisted=$listed)")
      }
      "msg" -> {
        val stringId = ctx.args.getOrNull(1)?.toIntOrNull()
        if (stringId == null) {
          ctx.reply(usage)
          return
        }
        // Optional single argument for the {00} placeholder: /probe msg 6063 5 Potion sends a
        // type-5 string arg, /probe msg 3015 9 3 a type-9 int arg.
        val argType = ctx.args.getOrNull(2)?.toIntOrNull()
        val argText = ctx.args.drop(3).joinToString(" ")
        val args =
            if (argType == null) emptyList()
            else
                listOf(
                    de.fiereu.openmmo.net.game.packets.ServerMessageArg(
                        argId = 0,
                        type = argType,
                        hasExtra = false,
                        extra = 0,
                        longValue = if (argType == 30) argText.toLongOrNull() ?: 0L else null,
                        intValue =
                            if (argType == 9 || argType == 10 || argType == 17)
                                argText.toIntOrNull() ?: 0
                            else null,
                        stringValue = if (argType == 5 || argType == 18) argText else null,
                        shortValues =
                            if (argType !in setOf(5, 18, 28, 30, 9, 10, 17))
                                listOf((argText.toIntOrNull() ?: 0).toShort())
                            else null,
                    ))
        ctx.session.send(ServerMessagePacket(stringId, args, true, null))
        ctx.reply("Sent ServerMessagePacket stringId=$stringId args=${args.size}.")
      }
      "dialog" -> {
        val textId = ctx.args.getOrNull(1)?.toIntOrNull()
        if (textId == null) {
          ctx.reply(usage)
          return
        }
        // Shows the ROM text behind a dialog id, to check whether a specific id renders.
        dialogService.showAndWait(ctx.session, ctx.state, textId, actionType = 3, entityId = -1)
      }
      "menu" -> {
        val kind = ctx.args.getOrNull(1)?.toIntOrNull() ?: 0
        val promptType = ctx.args.getOrNull(2)?.toIntOrNull()
        ctx.session.send(
            de.fiereu.openmmo.net.game.packets.MenuPromptOpenPacket(
                kind.toByte(), promptType?.toByte(), null, null))
        ctx.reply("Sent MenuPromptOpenPacket kind=$kind promptType=$promptType.")
      }
      "gm" -> {
        // The GmPanel packets are all server-to-client - the staff panel may simply appear when
        // the server pushes a variant and entries at it.
        val variant = ctx.args.getOrNull(1)?.toIntOrNull() ?: 0
        ctx.session.send(de.fiereu.openmmo.net.game.packets.GmPanelVariantPacket(variant.toByte()))
        ctx.session.send(
            de.fiereu.openmmo.net.game.packets.GmPanelEntryPacket(0, "Probe entry", "value", 1))
        ctx.reply("Sent GmPanelVariant=$variant plus one panel entry.")
      }
      "perms" -> {
        // The client receives permissions AND 0xFF (CharacterInfoCodec) - the built-in staff
        // menu is gated on bits in that byte. Probe values to find which bit unlocks it.
        val value = ctx.args.getOrNull(1)?.toIntOrNull()
        if (value == null) {
          ctx.reply(usage)
          return
        }
        val stored = characterStore.getCharacter(ctx.characterId) ?: return
        characterStore.updateCharacter(stored.info.copy(permissions = value))
        characterStore.flushCharacterAsync(ctx.characterId)
        ctx.reply(
            "Permissions now $value (client byte ${value and 0xFF}). Relog to let the client " +
                "re-read them.")
      }
      "learn" -> {
        // Replays the move-learn screen for the first party member, to see the client render it
        // outside a battle (r32645 reads the offered moves as a counted list, f/lg.Rl0).
        val moveId = (ctx.args.getOrNull(1)?.toIntOrNull() ?: 55).toShort()
        val mon = characterStore.getCharacter(ctx.characterId)?.pokemon?.firstOrNull()
        if (mon == null) {
          ctx.reply("No party pokemon to prompt for.")
          return
        }
        ctx.session.send(
            de.fiereu.openmmo.net.game.packets.battle.moves.MoveLearnPromptPacket(mon.id, listOf(moveId)))
        ctx.reply("Sent MoveLearnPromptPacket for ${mon.id} offering move $moveId.")
      }
      "pcinv" -> {
        val header = ctx.args.getOrNull(1)?.toIntOrNull() ?: 0
        ctx.session.send(
            de.fiereu.openmmo.net.game.packets.StorageInventoryPacket(
                header = header.toShort(),
                totalA = 250,
                totalB = 250,
                valueC = 0,
                usedA = 0,
                usedB = 0,
                listA = emptyList(),
                listB = emptyList(),
                listC = emptyList(),
                listD = emptyList(),
            ))
        ctx.reply("Sent StorageInventoryPacket header=$header.")
      }
      "pcwin" -> {
        val kind = ctx.args.getOrNull(1)?.toIntOrNull() ?: 0
        ctx.session.send(
            de.fiereu.openmmo.net.game.packets.StorageContextWindowPacket(
                kind.toByte(), emptyList()))
        ctx.reply("Sent StorageContextWindowPacket kind=$kind.")
      }
      "pc" -> {
        val stored = characterStore.getCharacter(ctx.characterId) ?: return
        de.fiereu.openmmo.net.game.packets.containerPackets(PokemonContainer.PC, stored.boxed).forEach { p -> ctx.session.send(p) }
        ctx.reply("Sent PC container with ${stored.pcStorage.size} pokemon.")
      }
      else -> ctx.reply(usage)
    }
  }
}
