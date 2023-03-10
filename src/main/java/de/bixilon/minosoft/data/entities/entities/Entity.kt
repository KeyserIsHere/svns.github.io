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
package de.bixilon.minosoft.data.entities.entities

import de.bixilon.kotlinglm.func.common.floor
import de.bixilon.kotlinglm.vec2.Vec2
import de.bixilon.kotlinglm.vec3.Vec3
import de.bixilon.kotlinglm.vec3.Vec3d
import de.bixilon.kotlinglm.vec3.Vec3i
import de.bixilon.kutil.collections.CollectionUtil.lockMapOf
import de.bixilon.kutil.collections.CollectionUtil.synchronizedMapOf
import de.bixilon.kutil.collections.CollectionUtil.synchronizedSetOf
import de.bixilon.kutil.collections.map.LockMap
import de.bixilon.kutil.time.TimeUtil.millis
import de.bixilon.minosoft.data.container.EquipmentSlots
import de.bixilon.minosoft.data.container.stack.ItemStack
import de.bixilon.minosoft.data.direction.Directions
import de.bixilon.minosoft.data.entities.EntityAnimations
import de.bixilon.minosoft.data.entities.EntityRotation
import de.bixilon.minosoft.data.entities.Poses
import de.bixilon.minosoft.data.entities.data.EntityData
import de.bixilon.minosoft.data.entities.data.EntityDataField
import de.bixilon.minosoft.data.entities.entities.player.local.LocalPlayerEntity
import de.bixilon.minosoft.data.entities.entities.properties.StatusEffectProperty
import de.bixilon.minosoft.data.entities.entities.vehicle.boat.Boat
import de.bixilon.minosoft.data.physics.PhysicsEntity
import de.bixilon.minosoft.data.registries.blocks.types.FluidBlock
import de.bixilon.minosoft.data.registries.effects.attributes.EntityAttribute
import de.bixilon.minosoft.data.registries.effects.attributes.EntityAttributeModifier
import de.bixilon.minosoft.data.registries.effects.attributes.StatusEffectOperations
import de.bixilon.minosoft.data.registries.enchantment.Enchantment
import de.bixilon.minosoft.data.registries.entities.EntityType
import de.bixilon.minosoft.data.registries.fluid.fluids.Fluid
import de.bixilon.minosoft.data.registries.fluid.fluids.flowable.FlowableFluid
import de.bixilon.minosoft.data.registries.identified.ResourceLocation
import de.bixilon.minosoft.data.registries.item.items.armor.DefendingItem
import de.bixilon.minosoft.data.registries.particle.data.BlockParticleData
import de.bixilon.minosoft.data.registries.shapes.AABB
import de.bixilon.minosoft.data.text.ChatComponent
import de.bixilon.minosoft.data.text.formatting.color.ChatColors
import de.bixilon.minosoft.data.text.formatting.color.RGBColor
import de.bixilon.minosoft.gui.rendering.entity.EntityRenderer
import de.bixilon.minosoft.gui.rendering.entity.models.DummyModel
import de.bixilon.minosoft.gui.rendering.entity.models.EntityModel
import de.bixilon.minosoft.gui.rendering.input.camera.EntityPositionInfo
import de.bixilon.minosoft.gui.rendering.particle.types.render.texture.advanced.block.BlockDustParticle
import de.bixilon.minosoft.gui.rendering.util.VecUtil.empty
import de.bixilon.minosoft.gui.rendering.util.VecUtil.horizontal
import de.bixilon.minosoft.gui.rendering.util.VecUtil.plus
import de.bixilon.minosoft.gui.rendering.util.VecUtil.toVec3
import de.bixilon.minosoft.gui.rendering.util.vec.vec3.Vec3Util.interpolateLinear
import de.bixilon.minosoft.gui.rendering.util.vec.vec3.Vec3dUtil.EMPTY
import de.bixilon.minosoft.gui.rendering.util.vec.vec3.Vec3dUtil.blockPosition
import de.bixilon.minosoft.gui.rendering.util.vec.vec3.Vec3dUtil.floor
import de.bixilon.minosoft.gui.rendering.util.vec.vec3.Vec3iUtil.chunkPosition
import de.bixilon.minosoft.gui.rendering.util.vec.vec3.Vec3iUtil.inChunkPosition
import de.bixilon.minosoft.protocol.network.connection.play.PlayConnection
import de.bixilon.minosoft.protocol.protocol.ProtocolDefinition
import java.util.*
import kotlin.math.abs
import kotlin.math.max

abstract class Entity(
    protected val connection: PlayConnection,
    val type: EntityType,
    val data: EntityData,
    position: Vec3d,
    var rotation: EntityRotation,
) : PhysicsEntity {
    protected val random = Random()
    open val equipment: LockMap<EquipmentSlots, ItemStack> = lockMapOf()
    val effects = StatusEffectProperty()
    val attributes: MutableMap<ResourceLocation, EntityAttribute> = synchronizedMapOf()

    val id: Int?
        get() = connection.world.entities.getId(this)
    open val uuid: UUID?
        get() = connection.world.entities.getUUID(this)

    @Deprecated(message = "Use connection.version", replaceWith = ReplaceWith("connection.version.versionId"))
    protected val versionId: Int get() = connection.version.versionId
    open var _attachedEntity: Int? = null

    var vehicle: Entity? = null
    var passengers: MutableSet<Entity> = synchronizedSetOf()
    val activelyRiding = false // ToDo: When player has a vehicle and movement is pressed

    override var velocity: Vec3d = Vec3d.EMPTY
    var movementMultiplier = Vec3d.EMPTY // ToDo: Used in cobwebs, etc
    protected open var velocityMultiplier: Double = 1.0

    var horizontalCollision = false
        protected set
    var verticalCollision = false
        protected set
    var fallDistance = 0.0

    open var hasCollisions = true

    override var onGround = false

    protected val defaultAABB: AABB
        get() {
            val halfWidth = dimensions.x / 2
            return AABB(Vec3(-halfWidth, 0.0f, -halfWidth), Vec3(halfWidth, dimensions.y, halfWidth))
        }

    open val dimensions = Vec2(type.width, type.height)

    open val eyeHeight: Float
        get() = dimensions.y * 0.85f

    var deltaMovement: Vec3d = Vec3d.EMPTY
    protected open var previousPosition: Vec3d = Vec3d(position)
    override var position: Vec3d = position
        set(value) {
            val delta = value - field
            if (delta != deltaMovement) {
                deltaMovement = delta
            }
            previousPosition = field
            field = value
            positionInfo.update()
        }

    var cameraPosition: Vec3 = position.toVec3
        private set

    var eyePosition: Vec3 = cameraPosition
        private set

    open val positionInfo = EntityPositionInfo(connection, this)


    open val spawnSprintingParticles: Boolean
        get() = isSprinting && !isSneaking // ToDo: Touching fluids

    protected var lastTickTime = -1L

    // fluids stuff
    val fluidHeights: MutableMap<ResourceLocation, Float> = synchronizedMapOf()
    var submergedFluid: Fluid? = null


    open var model: EntityModel<*>? = null

    fun forceMove(deltaPosition: Vec3d) {
        position = position + deltaPosition
    }

    fun getAttributeValue(name: ResourceLocation, baseValue: Double? = null): Double {
        // ToDo: Check order and verify value
        val entityAttribute = attributes[name]
        val realBaseValue = baseValue ?: entityAttribute?.baseValue ?: type.attributes[name] ?: 1.0
        var ret = realBaseValue

        fun addToValue(modifier: EntityAttributeModifier, amplifier: Int) {
            val instanceValue = modifier.amount * amplifier
            when (modifier.operation) {
                StatusEffectOperations.MULTIPLY_TOTAL -> ret *= 1.0 + instanceValue
                StatusEffectOperations.ADDITION -> ret += instanceValue
                StatusEffectOperations.MULTIPLY_BASE -> ret += realBaseValue * (instanceValue + 1.0)
            }
        }

        entityAttribute?.let {
            for (instance in it.modifiers.values) {
                addToValue(instance, 1)
            }
        }

        return ret
    }

    fun attachTo(vehicleId: Int?) {
        _attachedEntity = vehicleId
    }

    fun forceSetRotation(rotation: EntityRotation) {
        this.rotation = rotation
    }

    fun setHeadRotation(headYaw: Int) {
        // ToDo
    }

    private fun getEntityFlag(bitMask: Int): Boolean {
        return data.getBitMask(FLAGS_DATA, bitMask, 0x00)
    }

    @get:SynchronizedEntityData
    val isOnFire: Boolean
        get() = getEntityFlag(0x01)

    @get:SynchronizedEntityData
    open val isSneaking: Boolean
        get() = getEntityFlag(0x02)

    @get:SynchronizedEntityData
    open val isSprinting: Boolean
        get() = getEntityFlag(0x08)

    val isSwimming: Boolean
        get() = getEntityFlag(0x10)

    @get:SynchronizedEntityData
    val isInvisible: Boolean
        get() = getEntityFlag(0x20)

    @get:SynchronizedEntityData
    val hasGlowingEffect: Boolean
        get() = getEntityFlag(0x20)

    val isFlyingWithElytra: Boolean
        get() = getEntityFlag(0x80)

    @get:SynchronizedEntityData
    val airSupply: Int
        get() = data.get(AIR_SUPPLY_DATA, 300)

    @get:SynchronizedEntityData
    val customName: ChatComponent?
        get() = data.get(CUSTOM_NAME_DATA, null)

    @get:SynchronizedEntityData
    val isCustomNameVisible: Boolean
        get() = data.get(CUSTOM_NAME_VISIBLE_DATA, false)

    @get:SynchronizedEntityData
    val isSilent: Boolean
        get() = data.get(SILENT_DATA, false)

    @get:SynchronizedEntityData
    open val hasGravity: Boolean
        get() = !data.get(NO_GRAVITY_DATA, false)

    @get:SynchronizedEntityData
    open val pose: Poses?
        get() {
            return when {
                isFlyingWithElytra -> Poses.ELYTRA_FLYING
                isSwimming -> Poses.SWIMMING
                isSneaking -> Poses.SNEAKING
                else -> data.get(POSE_DATA, Poses.STANDING)
            }
        }

    @get:SynchronizedEntityData
    val ticksFrozen: Int
        get() = data.get(TICKS_FROZEN_DATA, 0)

    val canStep: Boolean
        get() = !isSneaking

    val belowBlockPosition: Vec3i
        get() {
            val position = (position - BELOW_POSITION_MINUS).floor
            if (connection.world[position] == null) {
                // ToDo: check for fences
            }
            return position
        }

    override val aabb: AABB
        get() = defaultAABB + position


    open var cameraAABB: AABB = AABB.EMPTY
        protected set

    open val hitboxColor: RGBColor
        get() = when {
            isInvisible -> ChatColors.GREEN
            else -> ChatColors.WHITE
        }


    @Synchronized
    fun tryTick() {
        val currentTime = millis()

        if (currentTime - lastTickTime >= ProtocolDefinition.TICK_TIME) {
            tick()
            postTick()
            lastTickTime = currentTime
        }
    }

    open fun draw(time: Long) {
        cameraPosition = interpolateLinear((time - lastTickTime) / ProtocolDefinition.TICK_TIMEf, Vec3(previousPosition), Vec3(position))
        eyePosition = cameraPosition + Vec3(0.0f, eyeHeight, 0.0f)
        cameraAABB = defaultAABB + cameraPosition
    }

    open val pushableByFluids: Boolean = false

    open fun tick() {
        effects.tick()
        previousPosition = position
        if (spawnSprintingParticles) {
            spawnSprintingParticles()
        }
    }

    open fun postTick() {
        updateFluidStates()
    }

    private fun spawnSprintingParticles() {
        val blockPosition = Vec3i(position.x.floor, (position.y - 0.2).floor, position.z.floor)
        val blockState = connection.world[blockPosition] ?: return

        // ToDo: Don't render particles for invisible blocks

        val velocity = Vec3d(velocity)

        connection.world += BlockDustParticle(
            connection = connection,
            position = position + Vec3d.horizontal(
                { (random.nextDouble() * 0.5) * dimensions.x },
                0.1
            ),
            velocity = Vec3d(velocity.x * -4.0, 1.5, velocity.z * -4.0),
            data = BlockParticleData(
                blockState = blockState,
                type = connection.registries.particleType[BlockDustParticle]!!,
            )
        )
    }

    fun getEquipmentEnchant(enchantment: Enchantment?): Int {
        enchantment ?: return 0
        var maxLevel = 0
        this.equipment.lock.acquire()
        for ((slot, equipment) in this.equipment.unsafe) {
            equipment.enchanting.enchantments[enchantment]?.let {
                if (it > maxLevel) {
                    maxLevel = it
                }
            }
        }
        this.equipment.lock.release()
        return maxLevel
    }

    open fun setObjectData(data: Int) = Unit

    override fun toString(): String {
        return type.toString()
    }

    fun fall(deltaY: Double) {
        if (onGround) {
            // ToDo: On block landing (particles, sounds, etc)
            this.fallDistance = 0.0
            return
        }
        this.fallDistance = this.fallDistance - deltaY
    }

    fun move(delta: Vec3d = velocity) {
        val positionBefore = position
        val wasInBorder = !connection.world.border.isOutside(positionBefore)
        if (!hasCollisions) {
            forceMove(delta)
            return
        }

        var movement = Vec3d(delta)

        // ToDo: Check for piston movement

        if (!movementMultiplier.empty) {
            movement = movement * movementMultiplier
            movementMultiplier = Vec3d.EMPTY
            velocity = Vec3d.EMPTY
        }

        if (this is LocalPlayerEntity) {
            movement = connection.collisionDetector.sneak(this, movement)
        }

        var collisionMovement = connection.collisionDetector.collide(null, movement, aabb, true)


        val targetPosition = positionBefore + collisionMovement
        val inBorder = !connection.world.border.isOutside(targetPosition)
        if (wasInBorder && !inBorder) {
            val border = connection.world.border
            val xDirection = if (collisionMovement.x < 0) -1 else 1
            val zDirection = if (collisionMovement.z < 0) -1 else 1
            collisionMovement = Vec3d(
                xDirection * minOf(abs(collisionMovement.x), border.diameter / 2 - abs(targetPosition.x) - abs(border.center.x), 0.0),
                collisionMovement.y,
                zDirection * minOf(abs(collisionMovement.z), border.diameter / 2 - abs(targetPosition.z) - abs(border.center.y), 0.0)
            )
        }
        forceMove(collisionMovement)


        horizontalCollision = collisionMovement.x != movement.x || collisionMovement.z != movement.z
        verticalCollision = collisionMovement.y != movement.y
        this.onGround = verticalCollision && movement.y < 0.0f


        fall(collisionMovement.y)

        var velocityChanged = false
        if (movement.y != collisionMovement.y) {
            if (movement.y < 0.0 && collisionMovement.y != 0.0) {
                val landingPosition = belowBlockPosition
                val landingBlockState = connection.world[belowBlockPosition]

                val previousVelocity = Vec3d(velocity)
                landingBlockState?.block?.onEntityLand(connection, this, landingPosition, landingBlockState)

                velocityChanged = velocity != previousVelocity
            }

            if (!velocityChanged) {
                velocity.y = 0.0
            }
        }

        if (!velocityChanged) {
            if (movement.x != collisionMovement.x) {
                velocity.x = 0.0
            }

            if (movement.z != collisionMovement.z) {
                velocity.z = 0.0
            }
        }



        if (onGround && canStep) {
            // ToDo: Play step sound
        }

        // ToDo: Check for move effect (sounds)

        // block collision handling
        val aabb = aabb.shrink(0.001)
        for (blockPosition in aabb.blockPositions) {
            val chunk = connection.world[blockPosition.chunkPosition] ?: continue
            val blockState = chunk[blockPosition.inChunkPosition] ?: continue
            blockState.block.onEntityCollision(connection, this, blockState, blockPosition)
        }

        val velocityMultiplier = velocityMultiplier
        velocity.x *= velocityMultiplier
        velocity.z *= velocityMultiplier
    }

    protected fun applyGravity(force: Boolean = false) {
        if (hasGravity || force) {
            velocity.y += -0.04
        }
    }


    private fun updateFluidState(fluid: ResourceLocation): Boolean {
        val aabb = aabb.shrink()

        var height = 0.0f
        var inFluid = false
        val velocity = Vec3d.EMPTY
        var checks = 0

        for ((blockPosition, blockState) in connection.world[aabb]) {
            if (blockState.block !is FluidBlock) {
                continue
            }

            if (blockState.block.fluid.identifier != fluid) {
                continue
            }
            val fluidHeight = blockPosition.y + blockState.block.fluid.getHeight(blockState)

            if (fluidHeight < aabb.min.y) {
                continue
            }

            inFluid = true

            height = max(fluidHeight - aabb.min.y.toFloat(), height)

            if (!pushableByFluids) {
                continue
            }

            val blockFluid = blockState.block.fluid

            if (blockFluid !is FlowableFluid) {
                continue
            }
            val fluidVelocity = blockFluid.getVelocity(connection, blockState, blockPosition)

            if (height < 0.4) {
                fluidVelocity *= height
            }

            velocity += (fluidVelocity * blockFluid.getVelocityMultiplier(connection, blockState, blockPosition))
            checks++
        }

        if (velocity.length() > 0.0) {
            if (checks > 0) {
                velocity /= checks
            }

            if (abs(this.velocity.x) < 0.003 && abs(this.velocity.z) < 0.003 && velocity.length() < 0.0045) {
                velocity.normalizeAssign()
                velocity *= 0.0045
            }

            this.velocity = (this.velocity + velocity)
        }

        if (height > 0.0) {
            fluidHeights[fluid] = height
        }
        return inFluid
    }


    private fun updateFluidStates() {
        fluidHeights.clear()
        if (vehicle is Boat) {
            return // ToDo
        }

        for (fluid in connection.registries.fluid) {
            updateFluidState(fluid.identifier)
        }

        this.submergedFluid = _getSubmergedFluid()
    }

    private fun _getSubmergedFluid(): Fluid? {
        // ToDo: Boat
        val eyeHeight = eyePosition.y - 0.1111111119389534

        val eyePosition = (Vec3d(position.x, eyeHeight, position.z)).blockPosition
        val blockState = connection.world[eyePosition] ?: return null
        if (blockState.block !is FluidBlock) {
            return null
        }
        val up = connection.world[eyePosition + Directions.UP]?.block

        if ((up is FluidBlock && up.fluid == blockState.block.fluid) || blockState.block.fluid.getHeight(blockState) > eyeHeight) {
            return blockState.block.fluid
        }
        return null
    }

    val protectionLevel: Float
        get() {
            var protectionLevel = 0.0f

            this.equipment.lock.acquire()
            for (equipment in equipment.unsafe.values) {
                val item = equipment.item.item

                if (item is DefendingItem) {
                    protectionLevel += item.defense
                }
            }
            this.equipment.lock.release()

            return protectionLevel
        }

    open fun onAttack(attacker: Entity) = true

    open fun createModel(renderer: EntityRenderer): EntityModel<*>? {
        return DummyModel(renderer, this).apply { this@Entity.model = this }
    }

    open fun handleAnimation(animation: EntityAnimations) = Unit


    companion object {
        private val FLAGS_DATA = EntityDataField("ENTITY_FLAGS")
        private val AIR_SUPPLY_DATA = EntityDataField("ENTITY_AIR_SUPPLY")
        private val CUSTOM_NAME_DATA = EntityDataField("ENTITY_CUSTOM_NAME")
        private val CUSTOM_NAME_VISIBLE_DATA = EntityDataField("ENTITY_CUSTOM_NAME_VISIBLE")
        private val SILENT_DATA = EntityDataField("ENTITY_SILENT")
        private val NO_GRAVITY_DATA = EntityDataField("ENTITY_NO_GRAVITY")
        private val POSE_DATA = EntityDataField("ENTITY_POSE")
        private val TICKS_FROZEN_DATA = EntityDataField("ENTITY_TICKS_FROZEN")
        private val BELOW_POSITION_MINUS = Vec3(0, 0.2f, 0)
    }
}
