/*
 * Minosoft
 * Copyright (C) 2021 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.gui.rendering.input.interaction

import de.bixilon.minosoft.config.key.KeyAction
import de.bixilon.minosoft.config.key.KeyBinding
import de.bixilon.minosoft.config.key.KeyCodes
import de.bixilon.minosoft.data.abilities.Gamemodes
import de.bixilon.minosoft.data.inventory.ItemStack
import de.bixilon.minosoft.data.player.Hands
import de.bixilon.minosoft.data.registries.blocks.BlockState
import de.bixilon.minosoft.data.registries.effects.DefaultStatusEffects
import de.bixilon.minosoft.data.registries.enchantment.DefaultEnchantments
import de.bixilon.minosoft.data.registries.fluid.DefaultFluids
import de.bixilon.minosoft.data.registries.items.tools.MiningToolItem
import de.bixilon.minosoft.gui.rendering.RenderWindow
import de.bixilon.minosoft.modding.event.events.BlockBreakAckEvent
import de.bixilon.minosoft.modding.event.invoker.CallbackEventInvoker
import de.bixilon.minosoft.protocol.packets.c2s.play.ArmSwingC2SP
import de.bixilon.minosoft.protocol.packets.c2s.play.PlayerActionC2SP
import de.bixilon.minosoft.protocol.protocol.ProtocolDefinition
import de.bixilon.minosoft.util.KUtil
import de.bixilon.minosoft.util.KUtil.synchronizedMapOf
import de.bixilon.minosoft.util.KUtil.toResourceLocation
import glm_.pow
import glm_.vec3.Vec3i

class BreakInteractionHandler(
    val renderWindow: RenderWindow,
) {
    private val connection = renderWindow.connection

    private var breakPosition: Vec3i? = null
    private var breakBlockState: BlockState? = null
    var breakProgress = Double.NEGATIVE_INFINITY
        private set
    val breakingBlock: Boolean
        get() = breakPosition != null

    private var breakSelectedSlot: Int = -1
    private var breakItemInHand: ItemStack? = null

    private var breakSent = 0L
    private var lastSwing = 0L
    private var creativeLastHoldBreakTime = 0L

    private var acknowledgedBreakStarts: MutableMap<Vec3i, BlockState?> = synchronizedMapOf()

    private val efficiencyEnchantment = connection.registries.enchantmentRegistry[DefaultEnchantments.EFFICIENCY]
    private val aquaAffinityEnchantment = connection.registries.enchantmentRegistry[DefaultEnchantments.AQUA_AFFINITY]

    private val hasteStatusEffect = connection.registries.statusEffectRegistry[DefaultStatusEffects.HASTE]
    private val miningFatigueStatusEffect = connection.registries.statusEffectRegistry[DefaultStatusEffects.MINING_FATIGUE]

    private fun clearDigging() {
        breakPosition = null
        breakBlockState = null
        breakProgress = Double.NEGATIVE_INFINITY

        breakSelectedSlot = -1
        breakItemInHand = null
    }

    private fun cancelDigging() {
        breakPosition?.let {
            connection.sendPacket(PlayerActionC2SP(PlayerActionC2SP.Actions.CANCELLED_DIGGING, it))
            clearDigging()
        }
    }

    private fun swingArm() {
        val currentTime = KUtil.time
        if (currentTime - lastSwing <= ProtocolDefinition.TICK_TIME) {
            return
        }
        lastSwing = currentTime
        connection.sendPacket(ArmSwingC2SP(Hands.MAIN))
    }

    private fun checkBreaking(isKeyDown: Boolean, deltaTime: Double): Boolean {
        val currentTime = KUtil.time

        if (!isKeyDown) {
            creativeLastHoldBreakTime = 0L
            cancelDigging()
            return false
        }

        if (!connection.player.gamemode.canBreak) {
            cancelDigging()
            return false
        }
        val raycastHit = renderWindow.inputHandler.camera.blockTarget

        if (raycastHit == null) {
            cancelDigging()
            return false
        }

        if (raycastHit.distance >= connection.player.reachDistance) {
            cancelDigging()
            return false
        }

        // check if we look at another block or our inventory changed
        if (breakPosition != raycastHit.blockPosition || breakBlockState != raycastHit.blockState || breakSelectedSlot != connection.player.selectedHotbarSlot || breakItemInHand !== connection.player.inventory.getHotbarSlot()) {
            cancelDigging()
        }


        fun startDigging() {
            if (breakPosition != null) {
                return
            }
            connection.sendPacket(PlayerActionC2SP(PlayerActionC2SP.Actions.START_DIGGING, raycastHit.blockPosition, raycastHit.hitDirection))

            breakPosition = raycastHit.blockPosition
            breakBlockState = raycastHit.blockState
            breakProgress = 0.0

            breakSelectedSlot = connection.player.selectedHotbarSlot
            breakItemInHand = connection.player.inventory.getHotbarSlot()
        }

        fun finishDigging() {
            connection.sendPacket(PlayerActionC2SP(PlayerActionC2SP.Actions.FINISHED_DIGGING, raycastHit.blockPosition, raycastHit.hitDirection))
            clearDigging()
            connection.world.setBlockState(raycastHit.blockPosition, null)

            raycastHit.blockState.breakSoundEvent?.let {
                connection.world.playSoundEvent(it, raycastHit.blockPosition, volume = raycastHit.blockState.soundEventVolume, pitch = raycastHit.blockState.soundEventPitch)
            }
        }

        val canStartBreaking = currentTime - breakSent >= ProtocolDefinition.TICK_TIME


        val canInstantBreak = connection.player.baseAbilities.creative || connection.player.gamemode == Gamemodes.CREATIVE

        if (canInstantBreak) {
            if (!canStartBreaking) {
                return true
            }
            // creative
            if (currentTime - creativeLastHoldBreakTime <= ProtocolDefinition.TICK_TIME * 5) {
                return true
            }
            swingArm()
            startDigging()
            finishDigging()
            creativeLastHoldBreakTime = currentTime
            breakSent = currentTime
            return true
        }

        if (breakPosition == null && !canStartBreaking) {
            return true
        }

        breakSent = currentTime

        startDigging()

        swingArm()

        // thanks to https://minecraft.fandom.com/wiki/Breaking#Calculation

        val breakItemInHand = breakItemInHand

        val isToolEffective = breakItemInHand?.item?.let {
            return@let if (it is MiningToolItem) {
                it.isEffectiveOn(connection, raycastHit.blockState)
            } else {
                false
            }
        } ?: false
        val isBestTool = !raycastHit.blockState.requiresTool || isToolEffective

        var speedMultiplier = breakItemInHand?.let { it.item.getMiningSpeedMultiplier(connection, raycastHit.blockState, it) } ?: 1.0f

        if (isToolEffective) {
            breakItemInHand?.enchantments?.get(efficiencyEnchantment)?.let {
                speedMultiplier += it.pow(2) + 1.0f
            }
        }

        connection.player.activeStatusEffects[hasteStatusEffect]?.let {
            speedMultiplier *= (0.2f * (it.amplifier + 1)) + 1.0f
        }

        connection.player.activeStatusEffects[miningFatigueStatusEffect]?.let {
            speedMultiplier *= when (it.amplifier) {
                0 -> 0.3f
                1 -> 0.09f
                2 -> 0.0027f
                else -> 8.1E-4f
            }
        }

        if (connection.player.submergedFluid?.resourceLocation == DefaultFluids.WATER && connection.player.getEquipmentEnchant(aquaAffinityEnchantment) == 0) {
            speedMultiplier /= 5.0f
        }

        if (!connection.player.onGround) {
            speedMultiplier /= 5.0f
        }

        var damage = speedMultiplier / raycastHit.blockState.hardness

        damage /= if (isBestTool) {
            30
        } else {
            100
        }

        when {
            damage > 1.0f -> {
                breakProgress = 1.0
            }
            damage <= 0.0f -> {
                breakProgress = 0.0
            }
            else -> {
                val ticks = 1.0f / damage
                val seconds = (ticks / ProtocolDefinition.TICKS_PER_SECOND)
                val progress = ((1.0f / seconds) * deltaTime)
                // Log.log(LogMessageType.OTHER, LogLevels.WARN){ "Breaking progress at $breakPosition, total=$breakProgress, totalEstimated=$seconds"}
                breakProgress += progress
            }
        }

        if (breakProgress >= 1.0f) {
            finishDigging()
        }
        return true
    }

    fun init() {
        renderWindow.inputHandler.registerCheckCallback(DESTROY_BLOCK_KEYBINDING to KeyBinding(
            mutableMapOf(
                KeyAction.CHANGE to mutableSetOf(KeyCodes.MOUSE_BUTTON_LEFT),
            ),
        ))

        connection.registerEvent(CallbackEventInvoker.of<BlockBreakAckEvent> {
            when (it.actions) {
                PlayerActionC2SP.Actions.START_DIGGING -> {
                    if (it.successful) {
                        acknowledgedBreakStarts[it.blockPosition] = it.blockState
                    } else {
                        if (it.blockPosition != breakPosition || it.blockState != breakBlockState) {
                            return@of
                        }
                        breakProgress = Double.NEGATIVE_INFINITY
                    }
                }
                PlayerActionC2SP.Actions.FINISHED_DIGGING -> {
                    if (acknowledgedBreakStarts[it.blockPosition] == null) {
                        // start was not acknowledged, undoing
                        connection.world[it.blockPosition] = it.blockState
                    }
                    acknowledgedBreakStarts.remove(it.blockPosition)
                }
            }
        })
    }

    fun draw(deltaTime: Double) {
        val isKeyDown = renderWindow.inputHandler.isKeyBindingDown(DESTROY_BLOCK_KEYBINDING)
        // ToDo: Entity attacking
        val consumed = checkBreaking(isKeyDown, deltaTime)

        if (!isKeyDown) {
            return
        }
        if (consumed) {
            return
        }
        swingArm() // ToDo: Only once
    }

    companion object {
        private val DESTROY_BLOCK_KEYBINDING = "minosoft:destroy_block".toResourceLocation()
    }
}