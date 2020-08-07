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

package de.bixilon.minosoft.protocol.packets.clientbound.play;

import de.bixilon.minosoft.game.datatypes.SoundCategories;
import de.bixilon.minosoft.game.datatypes.entities.Location;
import de.bixilon.minosoft.logging.Log;
import de.bixilon.minosoft.protocol.packets.ClientboundPacket;
import de.bixilon.minosoft.protocol.protocol.InByteBuffer;
import de.bixilon.minosoft.protocol.protocol.PacketHandler;

public class PacketNamedSoundEffect implements ClientboundPacket {
    static final float pitchCalc = 100.0F / 63.0F;
    Location location;
    String sound;
    float volume;
    float pitch;
    SoundCategories category;

    @Override
    public boolean read(InByteBuffer buffer) {
        sound = buffer.readString();
        if (buffer.getProtocolId() < 95) {
            location = new Location(buffer.readInt() * 8, buffer.readInt() * 8, buffer.readInt() * 8); // ToDo: check if it is not * 4
        }
        if (buffer.getProtocolId() >= 95) {
            category = SoundCategories.byId(buffer.readVarInt());
        }
        if (buffer.getProtocolId() >= 95) {
            location = new Location(buffer.readFixedPointNumberInteger() * 4, buffer.readFixedPointNumberInteger() * 4, buffer.readFixedPointNumberInteger() * 4);
        }
        volume = buffer.readFloat();
        if (buffer.getProtocolId() < 201) {
            pitch = (buffer.readByte() * pitchCalc) / 100F;
        } else {
            pitch = buffer.readFloat();
        }
        return true;
        //ToDo: 17w15a
    }

    @Override
    public void log() {
        Log.protocol(String.format("Play sound effect (sound=%s, category=%s, volume=%s, pitch=%s, location=%s)", sound, category, volume, pitch, location));
    }

    @Override
    public void handle(PacketHandler h) {
        h.handle(this);
    }

    public Location getLocation() {
        return location;
    }

    /**
     * @return Pitch in Percent
     */
    public float getPitch() {
        return pitch;
    }

    public String getSound() {
        return sound;
    }

    public float getVolume() {
        return volume;
    }

    public SoundCategories getCategory() {
        return category;
    }
}
