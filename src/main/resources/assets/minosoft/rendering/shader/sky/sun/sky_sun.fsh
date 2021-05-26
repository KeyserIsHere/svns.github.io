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

#version 330 core

out vec4 foutColor;

in vec4 finTintColor;
flat in uint finTextureIndex;
in vec3 finTextureCoordinates;

#include "minosoft:texture"


void main() {
    vec4 texelColor = getTexture(finTextureIndex, finTextureCoordinates);

    if (finTintColor.a == 1.0f && texelColor.a == 0) {
        discard;
    }
    // outColor = vec4(0.0f, 1.0f, 1.0f, 1.0f);
    foutColor = texelColor * finTintColor;
}
