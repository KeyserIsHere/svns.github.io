/*
 * Minosoft
 * Copyright (C) 2020 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.protocol.packets.s2c.play;

import com.google.common.collect.HashBiMap;
import de.bixilon.minosoft.data.inventory.ItemStack;
import de.bixilon.minosoft.data.registries.ResourceLocation;
import de.bixilon.minosoft.data.registries.recipes.Ingredient;
import de.bixilon.minosoft.data.registries.recipes.Recipe;
import de.bixilon.minosoft.data.registries.recipes.RecipeTypes;
import de.bixilon.minosoft.protocol.network.connection.play.PlayConnection;
import de.bixilon.minosoft.protocol.packets.s2c.PlayS2CPacket;
import de.bixilon.minosoft.protocol.protocol.PlayInByteBuffer;
import de.bixilon.minosoft.util.logging.Log;

import static de.bixilon.minosoft.protocol.protocol.ProtocolVersions.V_19W03A;

public class PacketDeclareRecipes extends PlayS2CPacket {
    private final HashBiMap<ResourceLocation, Recipe> recipes = HashBiMap.create();

    public PacketDeclareRecipes(PlayInByteBuffer buffer) {
        int length = buffer.readVarInt();
        for (int i = 0; i < length; i++) {
            Recipe recipe;
            ResourceLocation resourceLocation;
            String typeName;
            if (buffer.getVersionId() >= V_19W03A) { // ToDo: find out version
                typeName = buffer.readString();
                resourceLocation = buffer.readResourceLocation();
            } else {
                resourceLocation = buffer.readResourceLocation();
                typeName = buffer.readString();
            }
            RecipeTypes type = RecipeTypes.byName(typeName);
            switch (type) {
                case SHAPELESS: {
                    String group = buffer.readString();
                    Ingredient[] ingredients = buffer.readIngredientArray();
                    ItemStack result = buffer.readItemStack();
                    recipe = new Recipe(type, group, ingredients, result);
                    break;
                }
                case SHAPED: {
                    int width = buffer.readVarInt();
                    int height = buffer.readVarInt();
                    String group = buffer.readString();
                    Ingredient[] ingredients = buffer.readIngredientArray(width * height);
                    ItemStack result = buffer.readItemStack();
                    recipe = new Recipe(width, height, type, group, ingredients, result);
                    break;
                }
                case SMELTING:
                case BLASTING:
                case SMOKING:
                case CAMPFIRE: {
                    String group = buffer.readString();
                    Ingredient ingredient = buffer.readIngredient();
                    ItemStack result = buffer.readItemStack();
                    float experience = buffer.readFloat();
                    int cookingTime = buffer.readVarInt();
                    recipe = new Recipe(type, group, ingredient, result, experience, cookingTime);
                    break;
                }
                case STONE_CUTTING: {
                    String group = buffer.readString();
                    Ingredient ingredient = buffer.readIngredient();
                    ItemStack result = buffer.readItemStack();
                    recipe = new Recipe(type, group, ingredient, result);
                    break;
                }
                case SMITHING: {
                    Ingredient base = buffer.readIngredient();
                    Ingredient addition = buffer.readIngredient();
                    ItemStack result = buffer.readItemStack();
                    recipe = new Recipe(type, base, addition, result);
                    break;
                }
                default:
                    recipe = new Recipe(type);
                    break;
            }
            this.recipes.put(resourceLocation, recipe);
        }
    }

    @Override
    public void handle(PlayConnection connection) {
        connection.getRecipes().registerCustomRecipes(getRecipes());
    }

    @Override
    public void log() {
        Log.protocol(String.format("[IN] Received declare recipe packet (recipeLength=%d)", this.recipes.size()));
    }

    public HashBiMap<ResourceLocation, Recipe> getRecipes() {
        return this.recipes;
    }
}