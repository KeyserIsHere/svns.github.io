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

package de.bixilon.minosoft.gui.rendering.events

import de.bixilon.kotlinglm.mat4x4.Mat4
import de.bixilon.minosoft.gui.rendering.RenderContext

class CameraMatrixChangeEvent(
    context: RenderContext,
    viewMatrix: Mat4,
    projectionMatrix: Mat4,
    viewProjectionMatrix: Mat4,
) : RenderEvent(context) {
    val viewMatrix: Mat4 = viewMatrix
        get() = Mat4(field)

    val projectionMatrix: Mat4 = projectionMatrix
        get() = Mat4(field)

    val viewProjectionMatrix: Mat4 = viewProjectionMatrix
        get() = Mat4(field)
}
