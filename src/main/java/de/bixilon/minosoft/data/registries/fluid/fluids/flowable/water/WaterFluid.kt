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

package de.bixilon.minosoft.data.registries.fluid.fluids.flowable.water

import de.bixilon.kotlinglm.vec3.Vec3d
import de.bixilon.kotlinglm.vec3.Vec3i
import de.bixilon.kutil.primitive.BooleanUtil.decide
import de.bixilon.kutil.random.RandomUtil.chance
import de.bixilon.minosoft.data.entities.entities.player.local.LocalPlayerEntity
import de.bixilon.minosoft.data.registries.blocks.BlockState
import de.bixilon.minosoft.data.registries.blocks.properties.BlockProperties
import de.bixilon.minosoft.data.registries.effects.movement.MovementEffect
import de.bixilon.minosoft.data.registries.enchantment.armor.ArmorEnchantment
import de.bixilon.minosoft.data.registries.fluid.FluidFactory
import de.bixilon.minosoft.data.registries.fluid.fluids.Fluid
import de.bixilon.minosoft.data.registries.fluid.fluids.flowable.FlowableFluid
import de.bixilon.minosoft.data.registries.identified.AliasedIdentified
import de.bixilon.minosoft.data.registries.identified.Namespaces.minecraft
import de.bixilon.minosoft.data.registries.identified.ResourceLocation
import de.bixilon.minosoft.data.registries.registries.Registries
import de.bixilon.minosoft.gui.rendering.models.unbaked.fluid.fluids.WaterFluidModel
import de.bixilon.minosoft.gui.rendering.particle.types.render.texture.simple.water.UnderwaterParticle
import de.bixilon.minosoft.gui.rendering.util.VecUtil.plus
import de.bixilon.minosoft.gui.rendering.util.VecUtil.toVec3d
import de.bixilon.minosoft.protocol.network.connection.play.PlayConnection
import java.util.*
import kotlin.math.min

class WaterFluid(resourceLocation: ResourceLocation = this.identifier) : FlowableFluid(resourceLocation) {

    override fun getVelocityMultiplier(connection: PlayConnection, blockState: BlockState, blockPosition: Vec3i): Double {
        return VELOCITY_MULTIPLIER
    }

    override fun matches(other: Fluid): Boolean {
        return other is WaterFluid
    }

    override fun matches(other: BlockState?): Boolean {
        other ?: return false
        if (super.matches(other)) {
            return true
        }
        if (other.properties[BlockProperties.WATERLOGGED] == true) {
            return true
        }
        return false
    }

    override fun getHeight(state: BlockState): Float {
        val `super` = super.getHeight(state)
        if (`super` != 0.0f) {
            return `super`
        }
        if (state.properties[BlockProperties.WATERLOGGED] == true) {
            return 0.9f
        }
        return 0.0f
    }

    override fun travel(entity: LocalPlayerEntity, sidewaysSpeed: Float, forwardSpeed: Float, gravity: Double, falling: Boolean) {
        val y = entity.position.y
        var speedMultiplier = entity.isSprinting.decide(0.9, 0.8)

        var depthStriderLevel = min(entity.getEquipmentEnchant(ArmorEnchantment.DepthStrider), 3).toDouble()

        var speed = 0.02

        if (depthStriderLevel > 0) {
            if (!entity.onGround) {
                depthStriderLevel /= 2.0
            }

            speedMultiplier += (0.54600006 - speedMultiplier) * depthStriderLevel / 3.0
            speed += (entity.walkingSpeed - speed) * depthStriderLevel / 3.0
        }

        if (entity.effects[MovementEffect.DolphinsGrace] != null) {
            speedMultiplier *= 0.96
        }


        entity.accelerate(sidewaysSpeed, forwardSpeed, speed)

        val velocity = entity.velocity

        if (entity.horizontalCollision && entity.isClimbing) {
            velocity.y = 0.2
        }
        entity.velocity = velocity * Vec3d(speedMultiplier, 0.8, speedMultiplier)

        entity.velocity = updateMovement(entity, gravity, falling, entity.velocity)

        // ToDo: Do this magic, but check edged and not jump like a bunny
        // if (entity.horizontalCollision && !entity.collidesAt(entity.position + Vec3d(entity.velocity.x, entity.velocity.y + 0.6000000238418579 - entity.position.y + y, entity.velocity.z), true)) {
        //     entity.velocity.y = 0.30000001192092896
        // }
    }


    override fun randomTick(connection: PlayConnection, blockState: BlockState, blockPosition: Vec3i, random: Random) {
        super.randomTick(connection, blockState, blockPosition, random)

        // ToDo: if not sill and not falling
        if (random.chance(10)) {
            connection.world += UnderwaterParticle(connection, blockPosition.toVec3d + { random.nextDouble() })
        }
    }

    override fun createModel(): WaterFluidModel {
        return WaterFluidModel()
    }

    companion object : FluidFactory<WaterFluid>, AliasedIdentified {
        override val identifier = minecraft("water")
        override val identifiers = setOf(minecraft("flowing_water"))
        private const val VELOCITY_MULTIPLIER = 0.014

        override fun build(resourceLocation: ResourceLocation, registries: Registries) = WaterFluid()
    }
}
