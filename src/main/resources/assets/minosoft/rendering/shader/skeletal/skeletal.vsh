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

#version 330 core

layout (location = 0) in vec3 vinPosition;
layout (location = 1) in vec2 vinUV;
layout (location = 2) in float vinTransform;
layout (location = 3) in float vinIndexLayerAnimation;// texture index (0xF0000000), texture layer (0x0FFFF000), animation index (0x00000FFF)
layout (location = 4) in float vinFlags;

#include "minosoft:animation/header_vertex"

flat out uint finFlags;

uniform mat4 uViewProjectionMatrix;

layout (std140) uniform uSkeletalBuffer
{
    mat4 uSkeletalTransforms[TRANSFORMS];
};
uniform uint uLight;


#include "minosoft:animation/buffer"
#include "minosoft:light"

#include "minosoft:animation/main_vertex"

void main() {
    vec4 position = uSkeletalTransforms[floatBitsToUint(vinTransform)] * vec4(vinPosition, 1.0f);
    gl_Position = uViewProjectionMatrix * position;
    finTintColor = getLight(uLight & 0xFFu);
    finFragmentPosition = position.xyz;
    finFlags = floatBitsToUint(vinFlags);

    run_animation();
}
