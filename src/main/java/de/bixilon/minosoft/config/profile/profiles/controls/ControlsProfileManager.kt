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

package de.bixilon.minosoft.config.profile.profiles.controls

import com.fasterxml.jackson.databind.JavaType
import de.bixilon.kutil.cast.CastUtil.unsafeCast
import de.bixilon.kutil.collections.CollectionUtil.synchronizedBiMapOf
import de.bixilon.kutil.collections.map.bi.AbstractMutableBiMap
import de.bixilon.kutil.observer.map.bi.BiMapObserver.Companion.observedBiMap
import de.bixilon.minosoft.config.profile.GlobalProfileManager
import de.bixilon.minosoft.config.profile.ProfileManager
import de.bixilon.minosoft.modding.event.master.GlobalEventMaster
import de.bixilon.minosoft.util.KUtil.toResourceLocation
import de.bixilon.minosoft.util.json.Jackson
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid
import java.util.concurrent.locks.ReentrantLock

object ControlsProfileManager : ProfileManager<ControlsProfile> {
    override val mapper = Jackson.MAPPER.copy()
    override val namespace = "minosoft:controls".toResourceLocation()
    override val latestVersion get() = 1
    override val saveLock = ReentrantLock()
    override val profileClass = ControlsProfile::class.java
    override val jacksonProfileType: JavaType = Jackson.MAPPER.typeFactory.constructType(profileClass)
    override val icon = FontAwesomeSolid.KEYBOARD


    override val profiles: AbstractMutableBiMap<String, ControlsProfile> by observedBiMap(synchronizedBiMapOf())

    override var selected: ControlsProfile = null.unsafeCast()
        set(value) {
            field = value
            GlobalProfileManager.selectProfile(this, value)
            GlobalEventMaster.fire(ControlsProfileSelectEvent(value))
        }

    override fun createProfile(description: String?) = ControlsProfile(description ?: "Default controls profile")
}
