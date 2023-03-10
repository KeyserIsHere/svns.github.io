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

package de.bixilon.minosoft.gui.rendering.input.key

import de.bixilon.kutil.primitive.BooleanUtil.decide
import de.bixilon.minosoft.config.key.KeyActions
import de.bixilon.minosoft.config.key.KeyBinding
import de.bixilon.minosoft.config.key.KeyCodes
import de.bixilon.minosoft.gui.rendering.RenderContext
import de.bixilon.minosoft.gui.rendering.system.base.PolygonModes
import de.bixilon.minosoft.util.KUtil.format
import de.bixilon.minosoft.util.KUtil.toResourceLocation

object DefaultKeyCombinations {

    fun registerAll(context: RenderContext) {
        val inputHandler = context.inputHandler
        val window = context.window
        val connection = context.connection

        inputHandler.registerKeyCallback("minosoft:enable_debug_polygon".toResourceLocation(),
            KeyBinding(
                KeyActions.MODIFIER to setOf(KeyCodes.KEY_F4),
                KeyActions.STICKY to setOf(KeyCodes.KEY_P),
            )) {
            val nextMode = it.decide(PolygonModes.LINE, PolygonModes.FILL)
            context.framebufferManager.world.polygonMode = nextMode
            connection.util.sendDebugMessage("Polygon mode: ${nextMode.format()}")
        }

        inputHandler.registerKeyCallback("minosoft:take_screenshot".toResourceLocation(),
            KeyBinding(
                KeyActions.PRESS to setOf(KeyCodes.KEY_F2),
                ignoreConsumer = true,
            )) { context.screenshotTaker.takeScreenshot() }

        inputHandler.registerKeyCallback("minosoft:pause_incoming_packets".toResourceLocation(),
            KeyBinding(
                KeyActions.MODIFIER to setOf(KeyCodes.KEY_F4),
                KeyActions.STICKY to setOf(KeyCodes.KEY_I),
                ignoreConsumer = true,
            )) {
            connection.util.sendDebugMessage("Pausing incoming packets: ${it.format()}")
            connection.network.pauseReceiving(it)
        }

        inputHandler.registerKeyCallback("minosoft:pause_outgoing_packets".toResourceLocation(),
            KeyBinding(
                KeyActions.MODIFIER to setOf(KeyCodes.KEY_F4),
                KeyActions.STICKY to setOf(KeyCodes.KEY_O),
                ignoreConsumer = true,
            )) {
            connection.util.sendDebugMessage("Pausing outgoing packets: ${it.format()}")
            connection.network.pauseSending(it)
        }

        inputHandler.registerKeyCallback("minosoft:toggle_fullscreen".toResourceLocation(),
            KeyBinding(
                KeyActions.PRESS to setOf(KeyCodes.KEY_F11),
                ignoreConsumer = true,
            )) {
            window.fullscreen = !window.fullscreen
        }
    }
}
