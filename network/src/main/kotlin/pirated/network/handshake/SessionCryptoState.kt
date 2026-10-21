package pirated.network.handshake

import pirated.network.checksum.Checksum
import pirated.network.checksum.ChecksumFactory
import pirated.network.cipher.AesCtrSessionCipher
import pirated.network.cipher.SessionCipher
import java.security.PrivateKey
import java.security.PublicKey

internal class SessionCryptoState(
    val cipher: SessionCipher,
    val incomingChecksum: Checksum,
    val outgoingChecksum: Checksum,
) {
  companion object {
    fun derive(
        localPrivate: PrivateKey,
        remotePublic: PublicKey,
        role: AesCtrSessionCipher.Role,
        checksumSize: Int,
    ): SessionCryptoState {
      val secret = AesCtrSessionCipher.ecdh(localPrivate, remotePublic)
      val cipher = AesCtrSessionCipher.derive(secret, role)
      val (outSeed, inSeed) = AesCtrSessionCipher.directionalSeeds(secret, role)
      return SessionCryptoState(
          cipher = cipher,
          incomingChecksum = ChecksumFactory.create(checksumSize, inSeed),
          outgoingChecksum = ChecksumFactory.create(checksumSize, outSeed),
      )
    }
  }
}
