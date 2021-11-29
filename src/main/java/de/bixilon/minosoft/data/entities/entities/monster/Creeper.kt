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
package de.bixilon.minosoft.data.entities.entities.monster

import de.bixilon.minosoft.data.entities.EntityMetaDataFields
import de.bixilon.minosoft.data.entities.EntityRotation
import de.bixilon.minosoft.data.entities.entities.EntityMetaDataFunction
import de.bixilon.minosoft.data.registries.ResourceLocation
import de.bixilon.minosoft.data.registries.entities.EntityFactory
import de.bixilon.minosoft.data.registries.entities.EntityType
import de.bixilon.minosoft.protocol.network.connection.play.PlayConnection
import glm_.vec3.Vec3d

class Creeper(connection: PlayConnection, entityType: EntityType, position: Vec3d, rotation: EntityRotation) : Monster(connection, entityType, position, rotation) {

    @get:EntityMetaDataFunction(name = "State")
    val state: Int
        get() = entityMetaData.sets.getInt(EntityMetaDataFields.CREEPER_STATE)

    @get:EntityMetaDataFunction(name = "Is charged")
    val isCharged: Boolean
        get() = entityMetaData.sets.getBoolean(EntityMetaDataFields.CREEPER_IS_CHARGED)

    @get:EntityMetaDataFunction(name = "Is ignited")
    val isIgnited: Boolean
        get() = entityMetaData.sets.getBoolean(EntityMetaDataFields.CREEPER_IS_IGNITED)


    companion object : EntityFactory<Creeper> {
        override val RESOURCE_LOCATION: ResourceLocation = ResourceLocation("creeper")

        override fun build(connection: PlayConnection, entityType: EntityType, position: Vec3d, rotation: EntityRotation): Creeper {
            return Creeper(connection, entityType, position, rotation)
        }
    }
}