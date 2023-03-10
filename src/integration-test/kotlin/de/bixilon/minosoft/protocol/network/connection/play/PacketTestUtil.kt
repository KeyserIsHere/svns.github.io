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

package de.bixilon.minosoft.protocol.network.connection.play

import de.bixilon.kutil.cast.CastUtil.unsafeCast
import de.bixilon.minosoft.protocol.network.network.client.test.TestNetwork
import de.bixilon.minosoft.protocol.packets.c2s.C2SPacket
import org.testng.Assert

object PacketTestUtil {

    fun PlayConnection.test(): TestNetwork {
        return network.unsafeCast()
    }

    fun PlayConnection.assertPacket(packet: C2SPacket) {
        Assert.assertEquals(test().take(), packet)
    }

    fun PlayConnection.assertNoPacket() {
        Assert.assertNull(test().take())
    }

    fun PlayConnection.assertOnlyPacket(packet: C2SPacket) {
        assertPacket(packet)
        assertNoPacket()
    }
}
