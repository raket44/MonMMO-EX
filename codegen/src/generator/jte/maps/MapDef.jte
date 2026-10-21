@import pirated.codegen.maps.ParsedMap
@import pirated.codegen.maps.RenderUtil

@param String packageName
@param String objectName
@param ParsedMap map
package ${packageName}

import pirated.common.Tile2D
import pirated.common.enums.Direction
import pirated.common.enums.EncounterMethod
import pirated.common.enums.EncounterType
import pirated.common.enums.Lighting
import pirated.common.enums.MapType
import pirated.common.enums.MovementType
import pirated.common.enums.Weather
import pirated.maps.BgEventDef
import pirated.maps.MapCoordScript
import pirated.maps.MapDef
import pirated.maps.MapFrameScript
import pirated.maps.NpcDef
import pirated.maps.WarpTile
import pirated.maps.WildEncounterSlot
import pirated.maps.WildEncounterTable
import pirated.net.game.packets.MapData

internal val ${objectName}: MapDef =
    MapDef(
        sourceName = "${map.getSourceName()}",
        regionId = ${map.getRegion()}.toByte(),
        bankId = ${map.getBank()}.toByte(),
        mapId = ${map.getIndex()}.toByte(),
        width = ${map.getWidth()},
        height = ${map.getHeight()},
        paletteIdx1 = ${map.getPaletteIdx1()},
        paletteIdx2 = ${map.getPaletteIdx2()},
        unknownShort = ${map.getMusicId()},
        unknownByte = ${map.getMapsecId()},
        borderTiles = ${RenderUtil.INSTANCE.borderTiles(map.getBorderTiles())},
        blockData = "${map.getBlockData()}",
        behaviorData = "${map.getBehaviorData()}",
        metatileBehaviorData = "${map.getMetatileBehaviorData()}",
        wildEncounters = ${RenderUtil.INSTANCE.encounters(map.getEncounters())},
        lighting = ${map.getLighting()},
        weather = ${map.getWeather()},
        mapType = ${map.getMapType()},
        encounterType = ${map.getEncounterType()},
        connections = ${RenderUtil.INSTANCE.connections(map.getConnections())},
        warps = ${RenderUtil.INSTANCE.warps(map.getWarps())},
        npcs = ${RenderUtil.INSTANCE.npcs(map.getVisibleNpcs())},
        bgEvents = ${RenderUtil.INSTANCE.bgEvents(map.getBgEvents())},
        onTransitionScript = "${map.getOnTransitionScript()}",
        onLoadScript = "${map.getOnLoadScript()}",
        onFrameScripts = ${RenderUtil.INSTANCE.frameScripts(map.getOnFrameScripts())},
        coordScripts = ${RenderUtil.INSTANCE.coordScripts(map.getCoordScripts())},
    )
