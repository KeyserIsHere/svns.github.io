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

package de.bixilon.minosoft.game.datatypes;

import de.bixilon.minosoft.game.datatypes.blocks.Block;
import de.bixilon.minosoft.game.datatypes.entities.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Collection of ChunkColumns
 */
public class World {
    public final HashMap<ChunkLocation, Chunk> chunks;
    public final HashMap<Integer, Entity> entities;
    final String name;
    boolean hardcore;
    boolean raining;

    public World(String name) {
        this.name = name;
        chunks = new HashMap<>();
        entities = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public Chunk getChunk(ChunkLocation loc) {
        return chunks.get(loc);
    }

    public Block getBlock(BlockPosition pos) {
        ChunkLocation loc = pos.getChunkLocation();
        if (getChunk(loc) != null) {
            return getChunk(loc).getBlock(pos.getX() % 16, pos.getX(), pos.getZ() % 16);
        }
        return Block.AIR;
    }

    public void setBlock(BlockPosition pos, Block block) {
        if (getChunk(pos.getChunkLocation()) != null) {
            getChunk(pos.getChunkLocation()).setBlock(pos.getX() % 16, pos.getX(), pos.getZ() % 16, block);
        }
        // do nothing if chunk is unloaded
    }

    public void unloadChunk(ChunkLocation location) {
        chunks.remove(location);
    }

    public void setChunk(ChunkLocation location, Chunk chunk) {
        chunks.replace(location, chunk);
    }

    public void setChunks(HashMap<ChunkLocation, Chunk> chunkMap) {
        for (Map.Entry<ChunkLocation, Chunk> set : chunkMap.entrySet()) {
            chunks.replace(set.getKey(), set.getValue());
        }
    }

    public boolean isHardcore() {
        return hardcore;
    }

    public void setHardcore(boolean hardcore) {
        this.hardcore = hardcore;
    }

    public boolean isRaining() {
        return raining;
    }

    public void setRaining(boolean raining) {
        this.raining = raining;
    }
}
