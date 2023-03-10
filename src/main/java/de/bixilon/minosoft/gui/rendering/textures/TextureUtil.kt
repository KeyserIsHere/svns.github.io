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

package de.bixilon.minosoft.gui.rendering.textures

import de.bixilon.kotlinglm.vec2.Vec2i
import de.bixilon.minosoft.data.registries.identified.ResourceLocation
import de.bixilon.minosoft.gui.rendering.system.base.texture.StaticTextureArray
import de.bixilon.minosoft.gui.rendering.system.base.texture.TextureData
import de.bixilon.minosoft.gui.rendering.system.base.texture.TextureTransparencies
import de.bixilon.minosoft.gui.rendering.system.base.texture.texture.AbstractTexture
import de.bixilon.minosoft.gui.rendering.world.mesh.SingleWorldMesh
import de.bixilon.minosoft.gui.rendering.world.mesh.WorldMesh
import de.bixilon.minosoft.util.KUtil.toResourceLocation
import de.matthiasmann.twl.utils.PNGDecoder
import org.lwjgl.BufferUtils
import org.lwjgl.system.MemoryUtil
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.InputStream
import javax.imageio.ImageIO

object TextureUtil {

    fun ResourceLocation.texture(): ResourceLocation {
        var path = ""

        if (!this.path.startsWith("textures/")) {
            path += "textures/"
        }
        path += this.path

        if (!path.contains(".")) {
            // ending
            path += ".png"
        }

        return "$namespace:$path".toResourceLocation()
    }

    fun TextureTransparencies.getMesh(mesh: WorldMesh): SingleWorldMesh {
        return when (this) {
            TextureTransparencies.OPAQUE -> mesh.opaqueMesh
            TextureTransparencies.TRANSLUCENT -> mesh.translucentMesh
            TextureTransparencies.TRANSPARENT -> mesh.transparentMesh
        }!!
    }

    fun resolveTextures(textureArray: StaticTextureArray, textures: Map<String, String>): Map<String, AbstractTexture> {
        val resolvedTextures: MutableMap<String, AbstractTexture> = mutableMapOf()

        fun resolveTexture(key: String, value: String): AbstractTexture {
            resolvedTextures[key]?.let { return it }

            val variable = value.removePrefix("#")
            var texture: AbstractTexture? = null
            if (variable.length != value.length) {
                // resolve variable first
                texture = resolveTexture(variable, textures[variable]!!)
            }

            if (texture == null) {
                texture = textureArray.createTexture(value.toResourceLocation().texture())
            }

            resolvedTextures[key] = texture
            return texture
        }



        for ((key, value) in textures) {
            resolveTexture(key, value)
        }
        return resolvedTextures
    }

    /**
     * Other way of reading a texture if the first (preferred one) method does not work
     * Only happens to some weird textures (seen in PureBDCraft)
     * Ignores the alpha channel
     */
    private fun InputStream.readTexture2(): TextureData {
        val image: BufferedImage = ImageIO.read(this)
        val rgb = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)

        val byteOutput = ByteArrayOutputStream()
        val dataOutput = DataOutputStream(byteOutput)

        for (color in rgb) {
            dataOutput.writeInt((color shl 8) or 0xFF)
        }

        val buffer = MemoryUtil.memAlloc(byteOutput.size())
        buffer.put(byteOutput.toByteArray())

        return TextureData(Vec2i(image.width, image.height), buffer)
    }

    fun InputStream.readTexture(): TextureData {
        return try {
            val decoder = PNGDecoder(this)
            val data = BufferUtils.createByteBuffer(decoder.width * decoder.height * PNGDecoder.Format.RGBA.numComponents)
            decoder.decode(data, decoder.width * PNGDecoder.Format.RGBA.numComponents, PNGDecoder.Format.RGBA)

            TextureData(Vec2i(decoder.width, decoder.height), data)
        } catch (exception: Throwable) {
            this.reset()
            readTexture2()
        }
    }
}
