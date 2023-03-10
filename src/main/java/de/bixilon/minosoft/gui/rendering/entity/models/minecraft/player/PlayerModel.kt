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

package de.bixilon.minosoft.gui.rendering.entity.models.minecraft.player

import de.bixilon.kutil.observer.DataObserver.Companion.observe
import de.bixilon.kutil.observer.set.SetObserver.Companion.observeSet
import de.bixilon.minosoft.data.entities.entities.player.Arms
import de.bixilon.minosoft.data.entities.entities.player.PlayerEntity
import de.bixilon.minosoft.data.entities.entities.player.SkinParts
import de.bixilon.minosoft.data.entities.entities.player.properties.PlayerProperties
import de.bixilon.minosoft.data.entities.entities.player.properties.textures.metadata.SkinModel
import de.bixilon.minosoft.gui.rendering.entity.EntityRenderer
import de.bixilon.minosoft.gui.rendering.entity.models.SkeletalEntityModel
import de.bixilon.minosoft.gui.rendering.models.ModelLoader.Companion.bbModel
import de.bixilon.minosoft.gui.rendering.skeletal.instance.SkeletalInstance
import de.bixilon.minosoft.gui.rendering.skeletal.model.animations.SkeletalAnimation
import de.bixilon.minosoft.gui.rendering.skeletal.model.elements.SkeletalElement
import de.bixilon.minosoft.gui.rendering.system.base.texture.dynamic.DynamicStateChangeCallback
import de.bixilon.minosoft.gui.rendering.system.base.texture.dynamic.DynamicTexture
import de.bixilon.minosoft.gui.rendering.system.base.texture.dynamic.DynamicTextureState
import de.bixilon.minosoft.gui.rendering.system.base.texture.skin.PlayerSkin
import de.bixilon.minosoft.util.KUtil.toResourceLocation

open class PlayerModel(renderer: EntityRenderer, player: PlayerEntity) : SkeletalEntityModel<PlayerEntity>(renderer, player), DynamicStateChangeCallback {
    private var properties = player.additional.properties
    var skin: PlayerSkin? = null
        private set
    protected var refreshModel = false

    private val legAnimator = LegAnimator(this)
    private val armAnimator = ArmAnimator(this)
    private val animations: MutableList<SkeletalAnimation> = mutableListOf(legAnimator, armAnimator)


    private var _instance: SkeletalInstance? = null
    override var instance = createModel(properties)


    init {
        entity::skinParts.observeSet(this) { refreshModel = true }
        player.additional::properties.observe(this) { this.properties = it; refreshModel = true }
    }


    private fun createModel(properties: PlayerProperties?): SkeletalInstance? {
        val skin = context.textureManager.skins.getSkin(entity, properties) ?: return null
        val skinModel = skin.model
        val unbaked = context.modelLoader.entities.loadUnbakedModel(if (skinModel == SkinModel.SLIM) SLIM_MODEL else WIDE_MODEL)

        val elements: MutableList<SkeletalElement> = mutableListOf()
        elementLoop@ for (element in unbaked.elements) {
            for (skinPart in SkinParts.VALUES) {
                if (skinPart.name == element.name) {
                    elements += element.skinCopy(entity.skinParts, skinPart)
                    continue@elementLoop
                }
            }
            elements += element
        }
        skin.texture.usages.incrementAndGet()
        this.skin?.texture?.usages?.decrementAndGet()
        this.skin = skin
        skin.texture += this

        val skinTexture = if (skin.texture.state != DynamicTextureState.LOADED) context.textureManager.skins.default[entity.uuid] ?: return null else skin

        val model = unbaked.copy(elements = elements, animations = animations).bake(context, mapOf(0 to skinTexture.texture))

        val instance = SkeletalInstance(context, model)

        for (animation in animations) {
            instance.playAnimation(animation)
        }

        refreshModel = false
        this.properties = properties

        return instance
    }

    override fun prepareAsync() {
        if (refreshModel) {
            _instance = instance
            instance = createModel(properties)
        }
        super.prepareAsync()
    }

    override fun prepare() {
        _instance?.unload()
        _instance = null
        super.prepare()
    }

    private fun SkeletalElement.skinCopy(parts: Set<SkinParts>, part: SkinParts): SkeletalElement {
        if (part in parts) {
            return this
        }
        return this.copy(visible = false)
    }

    override fun onStateChange(texture: DynamicTexture, state: DynamicTextureState) {
        if (skin?.texture === texture) {
            refreshModel = true
        }
    }

    override fun unload() {
        skin?.texture?.usages?.decrementAndGet()
    }

    fun swingArm(arm: Arms) {
        armAnimator.swingArm(arm)
    }


    companion object {
        private val WIDE_MODEL = "minecraft:entities/player/wide".toResourceLocation().bbModel()
        private val SLIM_MODEL = "minecraft:entities/player/slim".toResourceLocation().bbModel()
    }
}
