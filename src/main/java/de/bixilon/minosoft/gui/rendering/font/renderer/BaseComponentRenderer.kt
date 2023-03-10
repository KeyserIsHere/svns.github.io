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

package de.bixilon.minosoft.gui.rendering.font.renderer

import de.bixilon.kotlinglm.vec2.Vec2i
import de.bixilon.minosoft.data.text.BaseComponent
import de.bixilon.minosoft.gui.rendering.RenderContext
import de.bixilon.minosoft.gui.rendering.font.WorldGUIConsumer
import de.bixilon.minosoft.gui.rendering.gui.elements.Element
import de.bixilon.minosoft.gui.rendering.gui.mesh.GUIVertexConsumer
import de.bixilon.minosoft.gui.rendering.gui.mesh.GUIVertexOptions

object BaseComponentRenderer : ChatComponentRenderer<BaseComponent> {

    override fun render(initialOffset: Vec2i, offset: Vec2i, size: Vec2i, element: Element, context: RenderContext, consumer: GUIVertexConsumer?, options: GUIVertexOptions?, renderInfo: TextRenderInfo, text: BaseComponent): Boolean {
        for (part in text.parts) {
            if (ChatComponentRenderer.render(initialOffset, offset, size, element, context, consumer, options, renderInfo, part)) {
                return true
            }
        }
        return false
    }

    override fun render3dFlat(context: RenderContext, offset: Vec2i, scale: Float, maxSize: Vec2i, consumer: WorldGUIConsumer, text: BaseComponent, light: Int) {
        for (part in text.parts) {
            ChatComponentRenderer.render3dFlat(context, offset, scale, maxSize, consumer, part, light)
        }
    }

    override fun calculatePrimitiveCount(text: BaseComponent): Int {
        var count = 0
        for (part in text.parts) {
            count += ChatComponentRenderer.calculatePrimitiveCount(part)
        }
        return count
    }
}
