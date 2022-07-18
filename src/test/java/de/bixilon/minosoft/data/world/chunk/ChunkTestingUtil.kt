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

import de.bixilon.kotlinglm.vec2.Vec2i
import de.bixilon.kutil.cast.CastUtil.unsafeCast
import de.bixilon.kutil.concurrent.lock.thread.ThreadLock
import de.bixilon.kutil.reflection.ReflectionUtil.setValue
import de.bixilon.kutil.watcher.DataWatcher
import de.bixilon.minosoft.data.registries.blocks.BlockState
import de.bixilon.minosoft.data.registries.blocks.light.LightProperties
import de.bixilon.minosoft.data.registries.blocks.light.SolidProperty
import de.bixilon.minosoft.data.registries.blocks.types.Block
import de.bixilon.minosoft.data.registries.dimension.DimensionProperties
import de.bixilon.minosoft.data.registries.materials.Material
import de.bixilon.minosoft.data.registries.materials.PushReactions
import de.bixilon.minosoft.data.registries.registries.Registries
import de.bixilon.minosoft.data.registries.shapes.VoxelShape
import de.bixilon.minosoft.data.world.World
import de.bixilon.minosoft.data.world.chunk.light.ChunkLight
import de.bixilon.minosoft.data.world.positions.ChunkPosition
import de.bixilon.minosoft.gui.rendering.util.vec.vec2.Vec2iUtil.EMPTY
import de.bixilon.minosoft.modding.event.master.EventMaster
import de.bixilon.minosoft.protocol.network.connection.Connection
import de.bixilon.minosoft.protocol.network.connection.play.PlayConnection
import de.bixilon.minosoft.util.KUtil.minosoft
import org.junit.jupiter.api.Test
import org.objenesis.ObjenesisStd
import kotlin.reflect.jvm.javaField

const val SECTIONS = 16

object ChunkTestingUtil {

    fun createConnection(): PlayConnection {
        val connection = ObjenesisStd().newInstance(PlayConnection::class.java)

        Connection::class.java.getDeclaredField("eventMaster").setValue(connection, EventMaster())
        return connection
    }

    fun createWorld(): World {
        val objenesis = ObjenesisStd()
        val world = objenesis.newInstance(World::class.java)
        world::dimension.javaField!!.setValue(world, DataWatcher(DimensionProperties(hasSkyLight = true)))
        world::connection.javaField!!.setValue(world, createConnection())

        return world
    }

    fun createEmptyChunk(position: ChunkPosition): Chunk {
        val objenesis = ObjenesisStd()
        val chunk = objenesis.newInstance(Chunk::class.java)
        Chunk::highestSection.javaField!!.setValue(chunk, SECTIONS)
        Chunk::lock.javaField!!.setValue(chunk, ThreadLock())
        Chunk::chunkPosition.javaField!!.setValue(chunk, position)
        Chunk::world.javaField!!.setValue(chunk, createWorld())
        Chunk::connection.javaField!!.setValue(chunk, chunk.world.connection)
        Chunk::light.javaField!!.setValue(chunk, ChunkLight(chunk))
        chunk.sections = arrayOfNulls(SECTIONS)

        return chunk
    }

    fun createChunkWithNeighbours(): Chunk {
        val chunk = createEmptyChunk(Vec2i.EMPTY)
        val neighbours: Array<Chunk?> = arrayOfNulls(8)
        var index = 0
        for (x in -1..1) {
            for (z in -1..1) {
                if (x == 0 && z == 0) {
                    continue
                }
                neighbours[index++] = createEmptyChunk(Vec2i(x, z))
            }
        }

        chunk.neighbours = neighbours.unsafeCast()
        return chunk
    }

    fun ChunkSection.fill(state: BlockState) {
        for (index in 0 until 4096) {
            blocks.unsafeSet(index, state)
        }
    }

    fun Chunk.fillBottom(state: BlockState) {
        getOrPut(0, false)!!.fill(state)
    }

    @Test
    fun testChunkTestingUtil() {
        createChunkWithNeighbours()
    }

    fun createBlock(name: String, luminance: Int, lightProperties: LightProperties): Block {
        val block = Block(minosoft(name), Registries(), mapOf())
        val material = Material(minosoft("dummy"), null, PushReactions.NORMAL, false, false, false, false, false, false, true)
        val state = BlockState(block, material = material, collisionShape = VoxelShape.EMPTY, outlineShape = VoxelShape.EMPTY, hardness = 1.0f, requiresTool = false, isSolid = true, luminance = luminance, lightProperties = lightProperties)
        block::states.javaField!!.setValue(block, setOf(state))
        block::defaultState.javaField!!.setValue(block, state)

        return block
    }

    fun createSolidBlock(): Block {
        return createBlock("solid", 0, SolidProperty)
    }

    fun createSolidLight(): Block {
        return createBlock("solid_light", 15, SolidProperty)
    }
}