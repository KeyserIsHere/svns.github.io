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

package de.bixilon.minosoft.gui.rendering.system.opengl.texture.dynamic

import de.bixilon.kotlinglm.vec2.Vec2
import de.bixilon.kotlinglm.vec2.Vec2i
import de.bixilon.kutil.concurrent.lock.simple.SimpleLock
import de.bixilon.minosoft.gui.rendering.system.base.texture.dynamic.DynamicStateChangeCallback
import de.bixilon.minosoft.gui.rendering.system.base.texture.dynamic.DynamicTexture
import de.bixilon.minosoft.gui.rendering.system.base.texture.dynamic.DynamicTextureState
import de.bixilon.minosoft.gui.rendering.util.vec.vec2.Vec2iUtil.EMPTY
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class OpenGLDynamicTexture(
    override val uuid: UUID,
    shaderId: Int,
) : DynamicTexture {
    var data: Array<ByteBuffer>? = null
    private val callbacks: MutableSet<DynamicStateChangeCallback> = mutableSetOf()
    private val callbackLock = SimpleLock()
    override val usages = AtomicInteger()
    override var state: DynamicTextureState = DynamicTextureState.WAITING
        set(value) {
            if (field == value) {
                return
            }
            field = value
            callbackLock.acquire()
            for (callback in callbacks) {
                callback.onStateChange(this, value)
            }
            callbackLock.release()
        }
    override var size: Vec2i = Vec2i.EMPTY

    override var shaderId: Int = shaderId
        get() {
            if (state == DynamicTextureState.UNLOADED) {
                throw IllegalStateException("Texture was garbage collected!")
            }
            if (usages.get() == 0) {
                throw IllegalStateException("Texture could be garbage collected every time!")
            }
            if (state == DynamicTextureState.LOADING) {
                throw IllegalStateException("Texture was not loaded yet!")
            }
            return field
        }

    override fun toString(): String {
        return uuid.toString()
    }

    override fun transformUV(end: Vec2?): Vec2 {
        return end ?: Vec2(1.0f)
    }

    override fun transformUV(end: FloatArray?): FloatArray {
        return end ?: floatArrayOf(1.0f, 1.0f)
    }

    override fun addListener(callback: DynamicStateChangeCallback) {
        callbackLock.lock()
        callbacks += callback
        callbackLock.unlock()
    }

    override fun removeListener(callback: DynamicStateChangeCallback) {
        callbackLock.lock()
        callbacks -= callback
        callbackLock.unlock()
    }
}

