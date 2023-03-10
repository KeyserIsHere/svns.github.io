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
package de.bixilon.minosoft.data.direction

import de.bixilon.kotlinglm.func.rad
import de.bixilon.kotlinglm.mat4x4.Mat4
import de.bixilon.kotlinglm.vec2.Vec2
import de.bixilon.kotlinglm.vec3.Vec3
import de.bixilon.kotlinglm.vec3.Vec3d
import de.bixilon.kotlinglm.vec3.Vec3i
import de.bixilon.kotlinglm.vec3.swizzle.*
import de.bixilon.kutil.cast.CastUtil.unsafeNull
import de.bixilon.kutil.enums.EnumUtil
import de.bixilon.kutil.enums.ValuesEnum
import de.bixilon.kutil.exception.Broken
import de.bixilon.kutil.reflection.ReflectionUtil.forceSet
import de.bixilon.minosoft.data.Axes
import de.bixilon.minosoft.data.registries.blocks.BlockState
import de.bixilon.minosoft.data.registries.blocks.properties.serializer.BlockPropertiesSerializer
import de.bixilon.minosoft.data.text.formatting.color.ChatColors
import de.bixilon.minosoft.data.world.chunk.ChunkSection
import de.bixilon.minosoft.gui.rendering.util.vec.vec3.Vec3Util.get
import de.bixilon.minosoft.protocol.protocol.ProtocolDefinition
import kotlin.math.abs
import kotlin.reflect.jvm.javaField

enum class Directions(
    val horizontalId: Int,
    val vector: Vec3i,
) {
    DOWN(-1, Vec3i(0, -1, 0)),
    UP(-1, Vec3i(0, 1, 0)),
    NORTH(2, Vec3i(0, 0, -1)),
    SOUTH(0, Vec3i(0, 0, 1)),
    WEST(1, Vec3i(-1, 0, 0)),
    EAST(3, Vec3i(1, 0, 0));

    val negative = ordinal % 2 == 0

    val vectorf = Vec3(vector)
    val vectord = Vec3d(vector)

    inline val axis: Axes get() = Axes[this] // ToDo
    val debugColor = ChatColors[ordinal]

    val rotatedMatrix: Mat4 by lazy {
        when (this) {
            DOWN -> Mat4().translateAssign(Vec3(0.5f)).rotateAssign(180.0f.rad, Vec3(1, 0, 0)).translateAssign(Vec3(-0.5f))
            UP -> Mat4().translateAssign(Vec3(0.5f)).rotateAssign((-180.0f).rad, Vec3(1, 0, 0)).translateAssign(Vec3(-0.5f)) // ToDo
            NORTH -> Mat4()
            SOUTH -> Mat4().translateAssign(Vec3(0.5f)).rotateAssign(180.0f.rad, Vec3(0, 1, 0)).translateAssign(Vec3(-0.5f))
            WEST -> Mat4().translateAssign(Vec3(0.5f)).rotateAssign((-270.0f).rad, Vec3(0, 1, 0)).translateAssign(Vec3(-0.5f))
            EAST -> Mat4().translateAssign(Vec3(0.5f)).rotateAssign((-90.0f).rad, Vec3(0, 1, 0)).translateAssign(Vec3(-0.5f))
        }
    }

    val inverted: Directions = unsafeNull()

    private fun invert(): Directions {
        val ordinal = ordinal
        return if (ordinal % 2 == 0) {
            byId(ordinal + 1)
        } else {
            byId(ordinal - 1)
        }
    }

    operator fun get(axis: Axes): Int {
        return vector[axis]
    }

    fun rotateYC(): Directions {
        return when (this) {
            NORTH -> EAST
            SOUTH -> WEST
            WEST -> NORTH
            EAST -> SOUTH
            else -> Broken("Rotation: $this")
        }
    }

    fun rotateYCC(): Directions {
        return when (this) {
            NORTH -> WEST
            SOUTH -> EAST
            WEST -> SOUTH
            EAST -> NORTH
            else -> Broken("Rotation: $this")
        }
    }

    fun getPositions(from: Vec3, to: Vec3): Array<Vec3> {
        return when (this) {
            DOWN -> arrayOf(Vec3(from.x, from.y, to.z), Vec3(to.x, from.y, to.z), Vec3(to.x, from.y, from.z), from)
            UP -> arrayOf(Vec3(from.x, to.y, from.z), Vec3(to.x, to.y, from.z), to, Vec3(from.x, to.y, to.z))
            NORTH -> arrayOf(Vec3(to.x, to.y, from.z), Vec3(from.x, to.y, from.z), from, Vec3(to.x, from.y, from.z))
            SOUTH -> arrayOf(Vec3(from.x, to.y, to.z), to, Vec3(to.x, from.y, to.z), Vec3(from.x, from.y, to.z))
            WEST -> arrayOf(Vec3(from.x, to.y, from.z), Vec3(from.x, to.y, to.z), Vec3(from.x, from.y, to.z), from)
            EAST -> arrayOf(to, Vec3(to.x, to.y, from.z), Vec3(to.x, from.y, from.z), Vec3(to.x, from.y, to.z))
        }
    }

    fun getSize(rotated: Directions, from: Vec3, to: Vec3): Pair<Vec2, Vec2> {
        var pair = when (this) {
            DOWN, UP -> Pair(from.xz, to.xz)
            NORTH, SOUTH -> Pair(from.xy, to.xy)
            WEST, EAST -> Pair(from.yz, to.yz)
        }
        if (rotated.negative != negative) {
            pair = Pair(Vec2(1.0f) - pair.first, Vec2(1.0f) - pair.second)

            pair = Pair(
                Vec2(minOf(pair.first.x, pair.second.x), minOf(pair.first.y, pair.second.y)),
                Vec2(maxOf(pair.first.x, pair.second.x), maxOf(pair.first.y, pair.second.y)),
            )
        }

        return pair
    }

    fun getFallbackUV(from: Vec3, to: Vec3): Pair<Vec2, Vec2> {
        return when (this) {
            DOWN, UP -> Pair(from.xz, to.xz)
            SOUTH, NORTH -> Pair(Vec2(1) - to.xy, Vec2(1) - from.xy)
            WEST, EAST -> Pair(Vec2(1) - to.zy, Vec2(1) - from.zy)
        }
    }

    fun getUVMultiplier(from: Vec3, to: Vec3): Vec2 {
        return when (this) {
            DOWN -> from.zx - to.zx
            UP -> from.xz - to.xz
            NORTH -> from.xy - to.xy
            SOUTH -> from.yx - to.yx
            EAST -> from.zy - to.zy
            WEST -> from.yz - to.yz
        }
    }

    fun getBlock(x: Int, y: Int, z: Int, section: ChunkSection, neighbours: Array<ChunkSection?>): BlockState? {
        return when (this) {
            DOWN -> {
                if (y == 0) {
                    neighbours[Directions.O_DOWN]?.blocks?.unsafeGet(x, ProtocolDefinition.SECTION_MAX_Y, z)
                } else {
                    section.blocks.unsafeGet(x, y - 1, z)
                }
            }

            UP -> {
                if (y == ProtocolDefinition.SECTION_MAX_Y) {
                    neighbours[Directions.O_UP]?.blocks?.unsafeGet(x, 0, z)
                } else {
                    section.blocks.unsafeGet(x, y + 1, z)
                }
            }

            NORTH -> {
                if (z == 0) {
                    neighbours[Directions.O_NORTH]?.blocks?.unsafeGet(x, y, ProtocolDefinition.SECTION_MAX_Z)
                } else {
                    section.blocks.unsafeGet(x, y, z - 1)
                }
            }

            SOUTH -> {
                if (z == ProtocolDefinition.SECTION_MAX_Z) {
                    neighbours[Directions.O_SOUTH]?.blocks?.unsafeGet(x, y, 0)
                } else {
                    section.blocks.unsafeGet(x, y, z + 1)
                }
            }

            WEST -> {
                if (x == 0) {
                    neighbours[Directions.O_WEST]?.blocks?.unsafeGet(ProtocolDefinition.SECTION_MAX_X, y, z)
                } else {
                    section.blocks.unsafeGet(x - 1, y, z)
                }
            }

            EAST -> {
                if (x == ProtocolDefinition.SECTION_MAX_X) {
                    neighbours[Directions.O_EAST]?.blocks?.unsafeGet(0, y, z)
                } else {
                    section.blocks.unsafeGet(x + 1, y, z)
                }
            }
        }
    }


    companion object : BlockPropertiesSerializer, ValuesEnum<Directions> {
        const val O_DOWN = 0 // Directions.DOWN.ordinal
        const val O_UP = 1 // Directions.UP.ordinal
        const val O_NORTH = 2 // Directions.NORTH.ordinal
        const val O_SOUTH = 3 // Directions.SOUTH.ordinal
        const val O_WEST = 4 // Directions.WEST.ordinal
        const val O_EAST = 5 // Directions.EAST.ordinal

        const val SIZE = 6
        const val SIZE_SIDES = 4
        const val SIDE_OFFSET = 2
        override val VALUES = values()
        override val NAME_MAP: Map<String, Directions> = EnumUtil.getEnumValues(VALUES)
        val SIDES = arrayOf(NORTH, SOUTH, WEST, EAST)
        val PRIORITY_SIDES = arrayOf(WEST, EAST, NORTH, SOUTH)
        private val HORIZONTAL = arrayOf(SOUTH, WEST, NORTH, EAST)

        override fun deserialize(value: Any): Directions {
            return NAME_MAP[value] ?: throw IllegalArgumentException("No such property: $value")
        }

        override fun get(name: String): Directions {
            if (name.lowercase() == "bottom") {
                return DOWN
            }
            return super.get(name)
        }

        @JvmStatic
        fun byId(id: Int): Directions {
            return VALUES[id]
        }

        private const val MIN_ERROR = 0.0001f

        fun byDirection(direction: Vec3): Directions {
            var minDirection = VALUES[0]
            var minError = 2.0f
            for (testDirection in VALUES) {
                val error = (testDirection.vectorf - direction).length()
                if (error < MIN_ERROR) {
                    return testDirection
                } else if (error < minError) {
                    minError = error
                    minDirection = testDirection
                }
            }
            return minDirection
        }

        fun byDirection(direction: Vec3d): Directions {
            return byDirection(Vec3(direction))
        }

        fun byHorizontal(value: Int): Directions {
            return HORIZONTAL[abs(value % HORIZONTAL.size)]
        }


        init {
            val field = Directions::inverted.javaField!!
            for (direction in VALUES) {
                field.forceSet(direction, direction.invert())
            }
        }
    }
}
