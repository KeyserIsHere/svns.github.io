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

package de.bixilon.minosoft.data.world.border

import de.bixilon.kotlinglm.vec2.Vec2d
import de.bixilon.kotlinglm.vec3.Vec3
import de.bixilon.kotlinglm.vec3.Vec3d
import de.bixilon.kotlinglm.vec3.Vec3i
import de.bixilon.kutil.math.interpolation.DoubleInterpolation.interpolateLinear
import de.bixilon.kutil.time.TimeUtil
import de.bixilon.minosoft.data.world.World
import de.bixilon.minosoft.gui.rendering.util.vec.vec2.Vec2dUtil.EMPTY

class WorldBorder {
    var center = Vec2d.EMPTY
    var radius = World.MAX_SIZE.toDouble()
    var warningTime = 0
    var warningBlocks = 0
    var portalBound = 0

    var state = WorldBorderState.STATIC
        private set

    private var lerpStart = -1L
    private var lerpEnd = -1L
    private var oldRadius = World.MAX_SIZE.toDouble()
    private var newRadius = World.MAX_SIZE.toDouble()

    fun isOutside(blockPosition: Vec3i): Boolean {
        return isOutside(blockPosition.x.toDouble(), blockPosition.z.toDouble())
    }

    fun isOutside(position: Vec3): Boolean {
        return isOutside(position.x.toDouble(), position.z.toDouble())
    }

    fun isOutside(position: Vec3d): Boolean {
        return isOutside(position.x, position.z)
    }

    fun isOutside(x: Double, z: Double): Boolean {
        if (x !in radius - center.x..radius + center.x) {
            return false
        }
        if (z !in radius - center.y..radius + center.y) {
            return false
        }
        return true
    }

    fun stopLerp() {
        lerpStart = -1L
    }

    fun lerp(oldRadius: Double, newRadius: Double, speed: Long) {
        val time = TimeUtil.millis
        lerpStart = time
        lerpEnd = time + speed
        this.oldRadius = oldRadius
        this.newRadius = newRadius
    }

    fun tick() {
        if (lerpStart < 0L) {
            return
        }
        val time = TimeUtil.millis
        if (lerpEnd > time) {
            state = WorldBorderState.STATIC
            lerpStart = -1
            return
        }
        val remaining = lerpEnd - time
        val delta = (lerpEnd - lerpStart)
        val oldRadius = radius
        val radius = interpolateLinear(remaining.toDouble() / delta.toDouble(), this.oldRadius, this.newRadius)
        this.radius = radius
        state = if (oldRadius > radius) {
            WorldBorderState.SHRINKING
        } else {
            WorldBorderState.GROWING
        }
        if (oldRadius == radius) {
            lerpStart = -1
            state = WorldBorderState.STATIC
        }
    }
}
