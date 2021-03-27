/*
 * Minosoft
 * Copyright (C) 2020 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.data.entities.entities.animal;

import de.bixilon.minosoft.data.entities.EntityMetaDataFields;
import de.bixilon.minosoft.data.entities.EntityRotation;
import de.bixilon.minosoft.data.entities.entities.EntityMetaDataFunction;
import de.bixilon.minosoft.protocol.network.Connection;
import glm_.vec3.Vec3;

import java.util.UUID;

public class Bee extends Animal {

    public Bee(Connection connection, int entityId, UUID uuid, Vec3 position, EntityRotation rotation) {
        super(connection, entityId, uuid, position, rotation);
    }

    private boolean getBeeFlag(int bitMask) {
        return this.metaData.getSets().getBitMask(EntityMetaDataFields.BEE_FLAGS, bitMask);
    }

    @EntityMetaDataFunction(name = "Is angry")
    public boolean isAngry() {
        return getBeeFlag(0x02);
    }

    @EntityMetaDataFunction(name = "Has stung")
    public boolean hasStung() {
        return getBeeFlag(0x04);
    }

    @EntityMetaDataFunction(name = "has Nectar")
    public boolean hasNectar() {
        return getBeeFlag(0x08);
    }

    @EntityMetaDataFunction(name = "Remaining angar time")
    public int getRemainingAngerTimer() {
        return this.metaData.getSets().getInt(EntityMetaDataFields.BEE_REMAINING_ANGER_TIME);
    }
}
