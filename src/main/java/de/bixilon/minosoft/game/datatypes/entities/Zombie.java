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

package de.bixilon.minosoft.game.datatypes.entities;

public class Zombie implements Entity {
    final int id;
    Location location;
    Velocity velocity;
    int yaw;
    int pitch;
    EntityMetaData data;

    public Zombie(int id) {
        this.id = id;
    }

    @Override
    public Entities getEntityType() {
        return Entities.ZOMBIE;
    }

    public int getId() {
        return id;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public void setLocation(Location location) {
        this.location = location;

    }

    @Override
    public Velocity getVelocity() {
        return velocity;
    }

    @Override
    public void setVelocity(Velocity velocity) {
        this.velocity = velocity;
    }

    @Override
    public int getYaw() {
        return 0;
    }

    @Override
    public void setYaw(int yaw) {
        this.yaw = yaw;

    }

    @Override
    public int getPitch() {
        return 0;
    }

    @Override
    public void setPitch(int pitch) {
        this.pitch = pitch;

    }

    @Override
    public float getWidth() {
        return 0;
    }

    @Override
    public float getHeight() {
        return 0;
    }

    @Override
    public EntityMetaData getMetaData() {
        return data;
    }

    @Override
    public void setMetaData(EntityMetaData data) {
        this.data = data;
    }
}
