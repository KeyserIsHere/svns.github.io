/*
 * Minosoft
 * Copyright (C) 2021 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.protocol.packets.s2c.play.scoreboard.teams

import de.bixilon.minosoft.protocol.packets.s2c.PlayS2CPacket
import de.bixilon.minosoft.protocol.protocol.PlayInByteBuffer
import de.bixilon.minosoft.util.KUtil
import de.bixilon.minosoft.util.enum.ValuesEnum

object TeamsS2CF {

    fun createPacket(buffer: PlayInByteBuffer): PlayS2CPacket {
        val name = buffer.readString()
        return when (TeamActions[buffer.readUnsignedByte()]) {
            TeamActions.CREATE -> TeamCreateS2CP(name, buffer)
            TeamActions.REMOVE -> TeamRemoveS2CP(name)
            TeamActions.UPDATE -> TeamUpdateS2CP(name, buffer)
            TeamActions.MEMBER_ADD -> TeamMemberAddS2CP(name, buffer)
            TeamActions.MEMBER_REMOVE -> TeamMemberRemoveS2CP(name, buffer)
        }
    }

    enum class TeamActions {
        CREATE,
        REMOVE,
        UPDATE,
        MEMBER_ADD,
        MEMBER_REMOVE,
        ;

        companion object : ValuesEnum<TeamActions> {
            override val VALUES: Array<TeamActions> = values()
            override val NAME_MAP: Map<String, TeamActions> = KUtil.getEnumValues(VALUES)
        }
    }
}