package pirated.server.game.services

import pirated.common.enums.ChatType
import pirated.common.enums.Language
import pirated.net.game.packets.ChatMessagePacket

fun notice(message: String): ChatMessagePacket =
    ChatMessagePacket(
        type = ChatType.GAME_NOTIFICATIONS,
        language = Language.EN,
        message = message,
        sender = "",
    )
