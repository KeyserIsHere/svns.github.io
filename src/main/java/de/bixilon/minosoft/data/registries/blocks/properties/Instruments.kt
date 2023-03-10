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

package de.bixilon.minosoft.data.registries.blocks.properties

import de.bixilon.minosoft.data.registries.blocks.properties.serializer.BlockPropertiesSerializer
import java.util.*

enum class Instruments(
    vararg val aliases: String,
) {
    HARP,
    BASE_DRUM("basedrum"),
    SNARE,
    HAT,
    BASS,
    FLUTE,
    BELL,
    GUITAR,
    CHIME,
    XYLOPHONE,
    IRON_XYLOPHONE,
    COW_BELL,
    DIDGERIDOO,
    BIT,
    BANJO,
    PLING,

    ZOMBIE,
    SKELETON,
    CREEPER,
    DRAGON,
    WITHER_SKELETON,
    PIGLIN,
    CUSTOM_HEAD,
    ;

    companion object : BlockPropertiesSerializer {
        private val NAME_MAP: Map<Any, Instruments>

        init {
            val names: MutableMap<Any, Instruments> = mutableMapOf()

            for (value in values()) {
                names[value.name.lowercase(Locale.getDefault())] = value
                for (alias in value.aliases) {
                    names[alias] = value
                }
            }

            NAME_MAP = names
        }

        override fun deserialize(value: Any): Instruments {
            return NAME_MAP[value] ?: throw IllegalArgumentException("No such property: $value")
        }
    }
}
