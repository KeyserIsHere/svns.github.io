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

package de.bixilon.minosoft.gui.rendering.system.base.texture

import de.bixilon.kutil.latch.CountUpAndDownLatch
import de.bixilon.minosoft.data.registries.identified.ResourceLocation
import de.bixilon.minosoft.gui.rendering.system.base.texture.texture.AbstractTexture
import de.bixilon.minosoft.gui.rendering.system.base.texture.texture.PNGTexture

interface StaticTextureArray : TextureArray {
    val textures: MutableMap<ResourceLocation, AbstractTexture>
    val animator: SpriteAnimator
    val state: TextureArrayStates

    fun get(resourceLocation: ResourceLocation): AbstractTexture? {
        return textures[resourceLocation]
    }

    fun createTexture(resourceLocation: ResourceLocation, mipmaps: Boolean = true, default: () -> AbstractTexture = { PNGTexture(resourceLocation, generateMipMaps = mipmaps) }): AbstractTexture

    fun preLoad(latch: CountUpAndDownLatch)
}
