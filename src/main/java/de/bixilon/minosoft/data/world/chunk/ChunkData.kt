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

package de.bixilon.minosoft.data.world.chunk

import de.bixilon.kotlinglm.vec3.Vec3i
import de.bixilon.minosoft.data.entities.block.BlockEntity
import de.bixilon.minosoft.data.world.biome.source.BiomeSource
import de.bixilon.minosoft.data.world.container.BlockSectionDataProvider

class ChunkData(
    var blocks: Array<BlockSectionDataProvider?>? = null,
    var blockEntities: Map<Vec3i, BlockEntity>? = null,
    var biomeSource: BiomeSource? = null,
    var light: Array<ByteArray?>? = null,
    var bottomLight: ByteArray? = null,
    var topLight: ByteArray? = null,
) {

    @Synchronized
    fun replace(data: ChunkData) {
        data.blocks?.let { this.blocks = it }
        data.blockEntities?.let { this.blockEntities = it }
        data.biomeSource?.let { this.biomeSource = it }
        data.light?.let { this.light = it }
        data.bottomLight?.let { this.bottomLight = it }
        data.topLight?.let { this.topLight = it }
    }
}
