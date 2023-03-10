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

package de.bixilon.minosoft.data.bossbar

import de.bixilon.kutil.bit.BitByte.isBitMask

data class BossbarFlags(
    val darkSky: Boolean,
    val dragonBar: Boolean,
    val fog: Boolean,
) {
    constructor(flags: Int) : this(darkSky = flags.isBitMask(DARK_SKY_MASK), dragonBar = flags.isBitMask(DRAGON_BAR_MASK), fog = flags.isBitMask(FOG_MASK))

    companion object {
        const val DARK_SKY_MASK = 0x01
        const val DRAGON_BAR_MASK = 0x02
        const val FOG_MASK = 0x04
    }
}
