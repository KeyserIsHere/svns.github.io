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

package de.bixilon.minosoft.config.profile.profiles.rendering.overlay.fire

import de.bixilon.minosoft.config.profile.delegate.primitive.BooleanDelegate
import de.bixilon.minosoft.config.profile.profiles.rendering.RenderingProfile

class FireC(profile: RenderingProfile) {

    /**
     * Enables the fire/burning overlay
     */
    var enabled by BooleanDelegate(profile, true, "")

    /**
     * Enables the overlay if in creative
     */
    var creative by BooleanDelegate(profile, true, "")

    /**
     * Enables the fire overlay if in lava
     */
    var lava by BooleanDelegate(profile, true, "")
}
