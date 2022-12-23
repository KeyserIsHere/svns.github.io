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

package de.bixilon.minosoft.gui.rendering.system.dummy.texture

import de.bixilon.kotlinglm.vec2.Vec2
import de.bixilon.kotlinglm.vec2.Vec2i
import de.bixilon.minosoft.gui.rendering.system.base.texture.dynamic.DynamicStateChangeCallback
import de.bixilon.minosoft.gui.rendering.system.base.texture.dynamic.DynamicTexture
import de.bixilon.minosoft.gui.rendering.system.base.texture.dynamic.DynamicTextureState
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

object DummyDynamicTexture : DynamicTexture {
    override val uuid: UUID = UUID(0L, 0L)
    override val size: Vec2i = Vec2i(1, 1)
    override val usages: AtomicInteger = AtomicInteger(0)
    override val state: DynamicTextureState = DynamicTextureState.LOADED

    override fun addListener(callback: DynamicStateChangeCallback) {
    }

    override fun removeListener(callback: DynamicStateChangeCallback) {
    }

    override fun transformUV(end: Vec2?): Vec2 {
        return end ?: Vec2(1.0f)
    }

    override fun transformUV(end: FloatArray?): FloatArray {
        return end ?: floatArrayOf(1.0f, 1.0f)

    }

    override val shaderId: Int = 0
}
