/*
 * Minosoft
 * Copyright (C) 2020-2022 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */
package de.bixilon.minosoft.protocol.packets.s2c.status

import de.bixilon.kutil.time.TimeUtil
import de.bixilon.kutil.time.TimeUtil.nanos
import de.bixilon.minosoft.protocol.network.connection.status.StatusConnection
import de.bixilon.minosoft.protocol.network.connection.status.StatusConnectionStates
import de.bixilon.minosoft.protocol.packets.factory.LoadPacket
import de.bixilon.minosoft.protocol.packets.s2c.StatusS2CPacket
import de.bixilon.minosoft.protocol.protocol.InByteBuffer
import de.bixilon.minosoft.protocol.protocol.ProtocolStates
import de.bixilon.minosoft.protocol.status.StatusPong
import de.bixilon.minosoft.util.logging.Log
import de.bixilon.minosoft.util.logging.LogLevels
import de.bixilon.minosoft.util.logging.LogMessageType

@LoadPacket(state = ProtocolStates.STATUS)
class PongS2CP(buffer: InByteBuffer) : StatusS2CPacket {
    val pingId: Long = buffer.readLong()

    override fun handle(connection: StatusConnection) {
        val ping = connection.ping ?: return
        if (ping.pingId != pingId) {
            Log.log(LogMessageType.NETWORK_PACKETS_IN, LogLevels.WARN) { "Unknown status pong (pingId=$pingId, expected=${ping.pingId})" }
            // return ToDo: feather-rs is sending a wrong ping id back
        }
        val latency = nanos() - ping.nanos
        connection.network.disconnect()
        connection.pong = StatusPong(pingId, latency)
        connection.state = StatusConnectionStates.PING_DONE
    }

    override fun log(reducedLog: Boolean) {
        Log.log(LogMessageType.NETWORK_PACKETS_IN, LogLevels.VERBOSE) { "Status pong (pingId=$pingId)" }
    }
}
