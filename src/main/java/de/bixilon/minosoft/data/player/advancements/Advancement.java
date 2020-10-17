/*
 * Codename Minosoft
 * Copyright (C) 2020 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *  This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.data.player.advancements;

import java.util.ArrayList;

public class Advancement {
    final String parentName;
    final AdvancementDisplay display;
    final ArrayList<String> criteria;
    final ArrayList<String[]> requirements;

    public Advancement(String parentName, AdvancementDisplay display, ArrayList<String> criteria, ArrayList<String[]> requirements) {
        this.parentName = parentName;
        this.display = display;
        this.criteria = criteria;
        this.requirements = requirements;
    }

    public String getParentName() {
        return parentName;
    }

    public AdvancementDisplay getDisplay() {
        return display;
    }

    public ArrayList<String> getCriteria() {
        return criteria;
    }

    public ArrayList<String[]> getRequirements() {
        return requirements;
    }
}