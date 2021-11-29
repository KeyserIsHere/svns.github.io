/*
 * Minosoft
 * Copyright (C) 2021 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.gui.rendering.gui.hud.elements.hotbar

import de.bixilon.minosoft.data.registries.fluid.DefaultFluids
import de.bixilon.minosoft.gui.rendering.gui.elements.Element
import de.bixilon.minosoft.gui.rendering.gui.elements.Pollable
import de.bixilon.minosoft.gui.rendering.gui.elements.primitive.ImageElement
import de.bixilon.minosoft.gui.rendering.gui.hud.HUDRenderer
import de.bixilon.minosoft.gui.rendering.gui.mesh.GUIVertexConsumer
import de.bixilon.minosoft.gui.rendering.gui.mesh.GUIVertexOptions
import de.bixilon.minosoft.gui.rendering.util.vec.vec2.Vec2iUtil.EMPTY
import de.bixilon.minosoft.util.MMath.ceil
import glm_.vec2.Vec2i

class HotbarAirElement(hudRenderer: HUDRenderer) : Element(hudRenderer), Pollable {
    private val water = hudRenderer.connection.registries.fluidRegistry[DefaultFluids.WATER]!!
    private val airBubble = hudRenderer.atlasManager["minecraft:air_bubble"]!!
    private val poppingAirBubble = hudRenderer.atlasManager["minecraft:popping_air_bubble"]!!

    init {
        forceSilentApply()
    }

    private var previousBubbles = 0
    private var bubbles = 0
    private var poppingCount = 0

    override fun forceRender(offset: Vec2i, z: Int, consumer: GUIVertexConsumer, options: GUIVertexOptions?): Int {
        if (bubbles + poppingCount <= 0) {
            return 0
        }

        for (i in bubbles + poppingCount - 1 downTo 0) {
            var atlasElement = airBubble
            if (i < poppingCount) {
                atlasElement = poppingAirBubble
            }

            val image = ImageElement(hudRenderer, atlasElement)

            image.render(offset + Vec2i(i * BUBBLE_SIZE.x, 0), z, consumer, options)
        }

        return 1
    }

    override fun poll(): Boolean {
        val player = hudRenderer.connection.player

        val air = player.airSupply

        val submergedFluid = player.submergedFluid

        var bubbles = 0
        var poppingCount = 0

        if (submergedFluid == water || (air in 1 until FULL_AIR)) {
            bubbles = ((air - 2) / AIR_PER_BUBBLE.toFloat()).ceil // 2 ticks for the popping "animation"


            if (bubbles < 0) {
                bubbles = 0
            } else if (bubbles > MAX_BUBBLES) {
                bubbles = MAX_BUBBLES
            }

            poppingCount = (air / AIR_PER_BUBBLE.toFloat()).ceil - bubbles
        }


        if (this.bubbles != bubbles || this.poppingCount != poppingCount) {
            previousBubbles = this.bubbles
            this.bubbles = bubbles
            this.poppingCount = poppingCount
            return true
        }

        return false
    }

    override fun forceSilentApply() {
        _size = if (bubbles + poppingCount <= 0) {
            Vec2i.EMPTY
        } else {
            Vec2i(BUBBLE_SIZE.x * (bubbles + poppingCount), BUBBLE_SIZE.y)
        }
        cacheUpToDate = false
    }

    override fun tick() {
        apply()
    }

    companion object {
        private val BUBBLE_SIZE = Vec2i(8, 9)
        private const val FULL_AIR = 300
        private const val MAX_BUBBLES = 10
        private const val AIR_PER_BUBBLE = FULL_AIR / MAX_BUBBLES
    }
}