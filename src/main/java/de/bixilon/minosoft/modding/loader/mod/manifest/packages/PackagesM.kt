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

package de.bixilon.minosoft.modding.loader.mod.manifest.packages

import de.bixilon.kutil.collections.CollectionUtil.toNull
import de.bixilon.minosoft.modding.loader.ModList
import de.bixilon.minosoft.modding.loader.ModLoader

data class PackagesM(
    val depends: Set<String> = setOf(),
    val provides: Set<String> = setOf(),
) {

    fun getMissingDependencies(mods: ModList): Set<String>? {
        val missing: MutableSet<String> = mutableSetOf()
        for (name in this.depends) {
            if (name !in mods && name !in ModLoader.mods && name !in provides) {
                missing += name
            }
        }
        return missing.toNull()
    }
}
