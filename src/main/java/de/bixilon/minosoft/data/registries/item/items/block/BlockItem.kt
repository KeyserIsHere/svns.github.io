/*
 * Minosoft
 * Copyright (C) 2020-2023 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.data.registries.item.items.block

import de.bixilon.kotlinglm.vec3.Vec3i
import de.bixilon.kutil.cast.CastUtil.unsafeNull
import de.bixilon.kutil.concurrent.pool.DefaultThreadPool
import de.bixilon.minosoft.data.abilities.Gamemodes
import de.bixilon.minosoft.data.container.stack.ItemStack
import de.bixilon.minosoft.data.entities.entities.player.Hands
import de.bixilon.minosoft.data.registries.blocks.BlockState
import de.bixilon.minosoft.data.registries.blocks.types.Block
import de.bixilon.minosoft.data.registries.factory.clazz.MultiClassFactory
import de.bixilon.minosoft.data.registries.identified.ResourceLocation
import de.bixilon.minosoft.data.registries.item.factory.PixLyzerItemFactory
import de.bixilon.minosoft.data.registries.item.items.PixLyzerItem
import de.bixilon.minosoft.data.registries.registries.Registries
import de.bixilon.minosoft.gui.rendering.camera.target.targets.BlockTarget
import de.bixilon.minosoft.gui.rendering.input.interaction.InteractionResults
import de.bixilon.minosoft.gui.rendering.util.VecUtil.plusAssign
import de.bixilon.minosoft.protocol.network.connection.play.PlayConnection

open class BlockItem(
    resourceLocation: ResourceLocation,
    registries: Registries,
    data: Map<String, Any>,
) : PixLyzerItem(resourceLocation, registries, data) {
    val block: Block = unsafeNull()

    init {
        this::block.inject(data["block"])
    }

    override fun interactBlock(connection: PlayConnection, target: BlockTarget, hand: Hands, stack: ItemStack): InteractionResults {
        if (!connection.player.gamemode.canBuild) {
            return InteractionResults.ERROR
        }

        val placePosition = Vec3i(target.blockPosition)
        if (!target.blockState.material.replaceable) {
            placePosition += target.direction

            if (connection.world[placePosition]?.material?.replaceable == false) {
                return InteractionResults.PASS
            }
        }

        if (!connection.world.isPositionChangeable(placePosition)) {
            return InteractionResults.ERROR
        }

        if (connection.world.getBlockEntity(placePosition) != null && !connection.player.isSneaking) {
            return InteractionResults.PASS
        }


        var placeBlockState: BlockState = block.defaultState
        try {
            placeBlockState = block.getPlacementState(connection, target) ?: return InteractionResults.PASS
        } catch (exception: Throwable) {
            exception.printStackTrace()
        }

        val collisionShape = placeBlockState.collisionShape + placePosition
        if (connection.world.entities.isEntityIn(collisionShape)) {
            return InteractionResults.ERROR
        }


        DefaultThreadPool += { connection.world[placePosition] = placeBlockState }

        if (connection.player.gamemode != Gamemodes.CREATIVE) {
            stack.item.decreaseCount()
        }

        placeBlockState.block.soundGroup?.let { group ->
            group.place?.let { connection.world.playSoundEvent(it, placePosition, group.volume, group.pitch) }
        }
        return InteractionResults.SUCCESS
    }

    companion object : PixLyzerItemFactory<BlockItem>, MultiClassFactory<BlockItem> {
        override val ALIASES = setOf("AliasedBlockItem")

        override fun build(resourceLocation: ResourceLocation, registries: Registries, data: Map<String, Any>): BlockItem {
            return BlockItem(resourceLocation, registries, data)
        }
    }
}
