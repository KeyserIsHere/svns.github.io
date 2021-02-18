/*
 * Minosoft
 * Copyright (C) 2020 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft;

public enum ShutdownReasons {
    UNKNOWN(1),
    REQUESTED_BY_USER(0),
    ALL_FINE(0),
    CRITICAL_EXCEPTION(1),
    NO_ACCOUNT_SELECTED(1),
    CLI_WRONG_PARAMETER(1),
    CLI_HELP(0),
    LAUNCHER_FXML_LOAD_ERROR(1);

    private final int exitCode;

    ShutdownReasons(int exitCode) {
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return this.exitCode;
    }
}
