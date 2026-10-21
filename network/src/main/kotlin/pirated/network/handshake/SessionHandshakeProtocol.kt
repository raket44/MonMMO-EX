package pirated.network.handshake

import pirated.network.Protocol

object SessionHandshakeProtocol : Protocol() {
  init {
    bidi(0x00u, ClientHelloCodec, ClientHelloPacket::class)
    bidi(0x01u, ServerHelloCodec, ServerHelloPacket::class)
    bidi(0x02u, ClientReadyCodec, ClientReadyPacket::class)
  }
}
