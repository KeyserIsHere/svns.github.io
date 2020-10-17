/*
 * Codename Minosoft
 * Copyright (C) 2020 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *  This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.modding.event.events;

import de.bixilon.minosoft.data.world.BlockPosition;
import de.bixilon.minosoft.modding.event.events.annotations.MinimumProtocolVersion;
import de.bixilon.minosoft.protocol.network.Connection;
import de.bixilon.minosoft.protocol.packets.clientbound.play.PacketBlockBreakAnimation;

@MinimumProtocolVersion(protocolId = 32)
public class BlockBreakAnimationEvent extends CancelableEvent {
    private final int entityId;
    private final BlockPosition position;
    private final byte stage;

    public BlockBreakAnimationEvent(Connection connection, int entityId, BlockPosition position, byte stage) {
        super(connection);
        this.entityId = entityId;
        this.position = position;
        this.stage = stage;
    }

    public BlockBreakAnimationEvent(Connection connection, PacketBlockBreakAnimation pkg) {
        super(connection);
        this.entityId = pkg.getEntityId();
        this.position = pkg.getPosition();
        this.stage = pkg.getStage();
    }

    public int getEntityId() {
        return entityId;
    }

    public BlockPosition getPosition() {
        return position;
    }

    public byte getStage() {
        return stage;
    }
}