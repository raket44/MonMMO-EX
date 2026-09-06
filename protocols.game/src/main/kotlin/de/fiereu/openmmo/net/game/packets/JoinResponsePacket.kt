package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.*

data class JoinResponsePacket(
    val canJoin: Boolean,
    val stats: GameStats? = null,
    val time: TimeInfo? = null,
) {
  data class GameStats(val playtime: Int, val rewardPoints: Int, val balance: Int)

  data class TimeInfo(val serverDayStartSecond: Int, val serverCurrentSecond: Int)

  companion object {
    fun reject(): JoinResponsePacket = JoinResponsePacket(false)

    fun accept(stats: GameStats, time: TimeInfo? = null): JoinResponsePacket =
        JoinResponsePacket(true, stats, time)

    fun acceptNow(playtime: Int, rewardPoints: Int, balance: Int): JoinResponsePacket {
      val dayStart = WorldClock.dayStartSecond()
      val now = WorldClock.nowSecond()
      return JoinResponsePacket(
          canJoin = true,
          stats = GameStats(playtime, rewardPoints, balance),
          time = TimeInfo(dayStart, now),
      )
    }
  }
}

private fun nowTimeInfo(): JoinResponsePacket.TimeInfo =
    JoinResponsePacket.TimeInfo(
        serverDayStartSecond = WorldClock.dayStartSecond(),
        serverCurrentSecond = WorldClock.nowSecond(),
    )

object JoinResponsePacketCodec : PacketCodec<JoinResponsePacket>() {
  override fun CodecScope<JoinResponsePacket>.body(): JoinResponsePacket {
    val canJoin = field(Bool, JoinResponsePacket::canJoin)
    if (!canJoin) return JoinResponsePacket(false)
    field(Utf16LeNullTerminated) { "" }
    field(S8) { 0 }
    val playtime =
        field(S32LE) {
          (it.stats
                  ?: throw MalformedPacketException("stats must be provided when canJoin is true"))
              .playtime
        }
    val rewardPoints = field(S32LE) { it.stats!!.rewardPoints }
    val balance = field(S32LE) { it.stats!!.balance }
    var resolvedTime: JoinResponsePacket.TimeInfo? = null
    val dayStart =
        field(S32LE) {
          val t = it.time ?: nowTimeInfo()
          resolvedTime = t
          t.serverDayStartSecond
        }
    val now = field(S32LE) { resolvedTime!!.serverCurrentSecond }
    return JoinResponsePacket(
        canJoin = true,
        stats = JoinResponsePacket.GameStats(playtime, rewardPoints, balance),
        time = JoinResponsePacket.TimeInfo(dayStart, now),
    )
  }
}
