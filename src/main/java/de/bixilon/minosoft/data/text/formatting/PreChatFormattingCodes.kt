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
package de.bixilon.minosoft.data.text.formatting

enum class PreChatFormattingCodes(
    override val char: Char,
    override val ansi: String,
) : ChatFormattingCode {
    OBFUSCATED('k', "\u001b[5m"),
    BOLD('l', "\u001b[1m"),
    STRIKETHROUGH('m', "\u001b[9m"),
    UNDERLINED('n', "\u001b[4m"),
    ITALIC('o', "\u001b[3m"),
    ;

    override fun toString(): String {
        return ansi
    }
}
