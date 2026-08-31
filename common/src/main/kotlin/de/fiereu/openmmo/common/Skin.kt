package de.fiereu.openmmo.common

import de.fiereu.openmmo.common.enums.SkinSlot

data class Skin(
    val slot: SkinSlot,
    val type: UShort?,
    val color: UByte?,
    /**
     * The client skin set's per-slot "extra" byte (f.N50.wn0): picks an addon's alternate form
     * (Noble Steed -> Noble Steed (Alt), Ur0.AL1's table). 0 = base form.
     */
    val variant: UByte = 0u,
)
