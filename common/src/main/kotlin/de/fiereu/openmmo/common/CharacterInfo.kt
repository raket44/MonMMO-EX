package de.fiereu.openmmo.common

import de.fiereu.openmmo.common.enums.Direction
import java.time.LocalDateTime

data class CharacterInfo(
    val id: Long,
    val name: String,
    val namePrefix: String = "",
    val userId: Int,
    val rivalSex: Byte,
    /** Leading byte of the skin set on the wire. */
    val skinRegionSelectionIndex: Int = 0,
    val lastLogin: LocalDateTime,
    val createdAt: LocalDateTime,
    /** Seconds spent in the world, the trainer card's "Time played" (client eu6.zz / 3600 hours). */
    val playTimeSeconds: Int = 0,
    /** When the character last left the world ("Last Online"); null when it never has. Not on this wire. */
    val lastLogout: LocalDateTime? = null,
    val money: Int,
    val permissions: Int,
    /** The Sweet Scent Ocarina's pp left, out of 32 (client f/ZZ.Og1; f/ZO1.k70 uses it once it covers the move's cost). */
    val sweetScentPp: Int = 32,
    val remainingSafariSteps: Short,
    val remainingSafariBalls: Byte,
    val pcExtraSlots: Byte,
    val battleBoxExtraSlots: Byte,
    val templateAmount: Byte,
    val positionRegionId: Byte,
    val positionBankId: Byte,
    val positionMapId: Byte,
    val positionX: Short,
    val positionY: Short,
    /** Not on the wire. LoadEntity carries the facing the client draws. */
    val positionFacing: Direction = Direction.DOWN,
    val repelLeft: Short,
    val repelItemId: Short,
    val lureLeft: Short,
    val lureItemId: Short,
    /** Runtime warp destination for MAP_DYNAMIC warps (setdynamicwarp), or null if none is set. */
    val dynamicWarp: DynamicWarp? = null,
)
