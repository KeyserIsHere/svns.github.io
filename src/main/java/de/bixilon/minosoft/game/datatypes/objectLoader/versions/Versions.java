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

package de.bixilon.minosoft.game.datatypes.objectLoader.versions;

import com.google.common.collect.HashBiMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.bixilon.minosoft.Config;
import de.bixilon.minosoft.game.datatypes.Mappings;
import de.bixilon.minosoft.logging.Log;
import de.bixilon.minosoft.protocol.protocol.ConnectionStates;
import de.bixilon.minosoft.protocol.protocol.Packets;
import de.bixilon.minosoft.protocol.protocol.ProtocolDefinition;
import de.bixilon.minosoft.util.Util;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Versions {

    private static final
    HashMap<String, Mappings> mappingsHashMap = new HashMap<>();
    static VersionMapping legacyMapping;
    static final HashBiMap<Integer, Version> versionMap = HashBiMap.create();
    static final HashSet<Version> loadedVersion = new HashSet<>();

    static {
        mappingsHashMap.put("registries", Mappings.REGISTRIES);
        mappingsHashMap.put("blocks", Mappings.BLOCKS);
    }

    public static Version getVersionById(int protocolId) {
        return versionMap.get(protocolId);
    }

    public static void load(JsonObject json) {
        for (String protocolId : json.keySet()) {
            loadVersion(json, protocolId);
        }
    }

    public static void loadVersion(JsonObject json, String protocolIdString) {
        JsonObject versionJson = json.getAsJsonObject(protocolIdString);
        String versionName = versionJson.get("name").getAsString();
        int protocolId = Integer.parseInt(protocolIdString);
        if (versionMap.containsKey(protocolId)) {
            // already loaded, skip
            return;
        }

        HashMap<ConnectionStates, HashBiMap<Packets.Serverbound, Integer>> serverboundPacketMapping;
        HashMap<ConnectionStates, HashBiMap<Packets.Clientbound, Integer>> clientboundPacketMapping;
        if (versionJson.get("mapping").isJsonPrimitive()) {
            // inherits or copies mapping from an other version
            if (!versionMap.containsKey(protocolId)) {
                loadVersion(json, versionJson.get("mapping").getAsString());
            }
            Version parent = versionMap.get(versionJson.get("mapping").getAsInt());
            serverboundPacketMapping = parent.getServerboundPacketMapping();
            clientboundPacketMapping = parent.getClientboundPacketMapping();
        } else {
            JsonObject mappingJson = versionJson.getAsJsonObject("mapping");
            serverboundPacketMapping = new HashMap<>();

            for (JsonElement packetElement : mappingJson.getAsJsonArray("serverbound")) {
                String packetName = packetElement.getAsString();
                Packets.Serverbound packet = Packets.Serverbound.valueOf(packetName);
                if (!serverboundPacketMapping.containsKey(packet.getState())) {
                    serverboundPacketMapping.put(packet.getState(), HashBiMap.create());
                }
                serverboundPacketMapping.get(packet.getState()).put(packet, serverboundPacketMapping.get(packet.getState()).size());
            }
            clientboundPacketMapping = new HashMap<>();
            for (JsonElement packetElement : mappingJson.getAsJsonArray("clientbound")) {
                String packetName = packetElement.getAsString();
                Packets.Clientbound packet = Packets.Clientbound.valueOf(packetName);
                if (!clientboundPacketMapping.containsKey(packet.getState())) {
                    clientboundPacketMapping.put(packet.getState(), HashBiMap.create());
                }
                clientboundPacketMapping.get(packet.getState()).put(packet, clientboundPacketMapping.get(packet.getState()).size());
            }
        }
        Version version = new Version(versionName, protocolId, serverboundPacketMapping, clientboundPacketMapping);
        versionMap.put(version.getProtocolVersion(), version);
    }

    public static void loadVersionMappings(Mappings type, JsonObject data, int protocolId) {
        Version version = versionMap.get(protocolId);
        VersionMapping mapping;
        if (protocolId < ProtocolDefinition.FLATTING_VERSION_ID) {
            if (legacyMapping == null) {
                legacyMapping = new VersionMapping(version);
            }
            if (!legacyMapping.isFullyLoaded()) {
                legacyMapping.load(type, data);
            }
            mapping = legacyMapping;
        } else {
            mapping = version.getMapping();
            if (mapping == null) {
                mapping = new VersionMapping(version);
            }
            mapping.load(type, data);
        }
        version.setMapping(mapping);
        loadedVersion.add(version);
    }

    public static void loadVersionMappings(int protocolId) throws IOException {
        Version version;
        if (protocolId < ProtocolDefinition.FLATTING_VERSION_ID) {
            version = versionMap.get(ProtocolDefinition.PRE_FLATTENING_VERSION_ID);
        } else {
            version = versionMap.get(protocolId);
        }
        Log.verbose(String.format("Loading mappings for version %s...", version));
        long startTime = System.currentTimeMillis();
        for (Map.Entry<String, Mappings> mappingSet : mappingsHashMap.entrySet()) {
            JsonObject data = Util.readJsonFromFile(Config.homeDir + String.format("assets/mapping/%s/%s.json", version.getVersionName(), mappingSet.getKey())).getAsJsonObject("minecraft");
            loadVersionMappings(mappingSet.getValue(), data, protocolId);
        }

        Log.verbose(String.format("Loaded mappings for version %s in %dms (%s)", version, (System.currentTimeMillis() - startTime), version.getVersionName()));

    }

    public static void unloadUnnecessaryVersions(int necessary) {
        if (necessary >= ProtocolDefinition.FLATTING_VERSION_ID) {
            legacyMapping.unload();
            legacyMapping = null;
        }
        for (Version version : loadedVersion) {
            if (version.getProtocolVersion() == necessary) {
                continue;
            }
            version.getMapping().unload();
        }
    }

    public static Version getLowestVersionSupported() {
        return new Version("13w41b", 0, null, null);
    }
}