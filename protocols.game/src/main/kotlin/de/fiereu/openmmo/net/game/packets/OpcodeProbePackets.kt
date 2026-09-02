package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec

/**
 * Empty-bodied probes for the s2c opcodes the protocol has not mapped yet.
 *
 * The client's packet reader catches a failed parse per packet and logs "Reading failed for packet
 * {class}" (f/eR bytecode) with the CLASS it tried to decode - so firing an empty body at an
 * unknown opcode makes the client itself name the packet behind it. This is the runtime-probe arm
 * of verify-against-client, currently hunting the evolution-prompt packet whose c2s response (0x0B,
 * entity id + accepted flag) the protocol already carries.
 */
class OpcodeProbeCodec<T : Any>(private val make: () -> T) : PacketCodec<T>() {
  override fun CodecScope<T>.body(): T = make()
}

class Probe06Packet

class Probe6APacket

class Probe82Packet

class Probe8APacket

class Probe9FPacket

class ProbeAFPacket
