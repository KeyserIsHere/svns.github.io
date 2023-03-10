/*
 * Minosoft
 * Copyright (C) 2020-2023 Moritz Zwerger
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

import de.bixilon.kutil.collections.CollectionUtil.synchronizedMapOf
import de.bixilon.kutil.latch.CountUpAndDownLatch
import de.bixilon.minosoft.data.registries.identified.ResourceLocation
import de.bixilon.minosoft.gui.rendering.system.base.RenderSystem
import de.bixilon.minosoft.gui.rendering.system.base.shader.NativeShader
import de.bixilon.minosoft.gui.rendering.system.base.texture.SpriteAnimator
import de.bixilon.minosoft.gui.rendering.system.base.texture.StaticTextureArray
import de.bixilon.minosoft.gui.rendering.system.base.texture.TextureArrayStates
import de.bixilon.minosoft.gui.rendering.system.base.texture.TextureStates
import de.bixilon.minosoft.gui.rendering.system.base.texture.texture.AbstractTexture

class DummyStaticTextureArray(renderSystem: RenderSystem) : StaticTextureArray {
    override val textures: MutableMap<ResourceLocation, AbstractTexture> = synchronizedMapOf()
    override val animator: SpriteAnimator = SpriteAnimator(renderSystem)
    override val state: TextureArrayStates = TextureArrayStates.DECLARED

    override fun createTexture(resourceLocation: ResourceLocation, mipmaps: Boolean, default: () -> AbstractTexture): AbstractTexture {
        return textures.getOrPut(resourceLocation) { DummyTexture(resourceLocation) }
    }

    override fun preLoad(latch: CountUpAndDownLatch) {
        for (texture in textures.values) {
            (texture as DummyTexture).state = TextureStates.LOADED
        }
    }

    override fun load(latch: CountUpAndDownLatch) {
        animator.init()
    }

    override fun activate() {
    }

    override fun use(shader: NativeShader, name: String) {
    }
}
