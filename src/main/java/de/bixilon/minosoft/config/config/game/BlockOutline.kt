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

package de.bixilon.minosoft.config.config.game

import com.squareup.moshi.Json
import de.bixilon.minosoft.data.text.ChatColors
import de.bixilon.minosoft.data.text.RGBColor

data class BlockOutline(
    @Json(name = "collision_boxes") val collisionBoxes: Boolean = false,
    @Json(name = "disable_z_buffer") val disableZBuffer: Boolean = false,
    @Json(name = "outline_color") val outlineColor: RGBColor = ChatColors.RED,
    @Json(name = "collision_color") val collisionColor: RGBColor = ChatColors.BLUE,
)
