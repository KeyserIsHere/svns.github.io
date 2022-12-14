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

package de.bixilon.minosoft.gui.rendering.framebuffer.world.overlay.overlays.arm

import de.bixilon.kotlinglm.GLM
import de.bixilon.kotlinglm.func.rad
import de.bixilon.kotlinglm.mat4x4.Mat4
import de.bixilon.kotlinglm.vec3.Vec3
import de.bixilon.kutil.cast.CastUtil.nullCast
import de.bixilon.kutil.cast.CastUtil.unsafeNull
import de.bixilon.minosoft.data.entities.entities.player.Arms
import de.bixilon.minosoft.gui.rendering.RenderWindow
import de.bixilon.minosoft.gui.rendering.camera.CameraDefinition
import de.bixilon.minosoft.gui.rendering.entity.models.minecraft.player.PlayerModel
import de.bixilon.minosoft.gui.rendering.framebuffer.world.overlay.Overlay
import de.bixilon.minosoft.gui.rendering.framebuffer.world.overlay.OverlayFactory
import de.bixilon.minosoft.gui.rendering.skeletal.baked.BakedSkeletalModel.Companion.fromBlockCoordinates
import de.bixilon.minosoft.gui.rendering.skeletal.baked.BakedSkeletalModel.Companion.toBlockCoordinate
import de.bixilon.minosoft.gui.rendering.system.base.IntegratedBufferTypes
import de.bixilon.minosoft.gui.rendering.system.base.RenderingCapabilities
import de.bixilon.minosoft.gui.rendering.system.base.texture.skin.PlayerSkin
import de.bixilon.minosoft.util.KUtil.minosoft

class ArmOverlay(private val renderWindow: RenderWindow) : Overlay {
    private val config = renderWindow.connection.profiles.rendering.overlay
    private val shader = renderWindow.renderSystem.createShader(minosoft("arm")) { ArmOverlayShader(it) }
    override val render: Boolean
        get() = renderWindow.camera.view.view.renderArm && config.arm.render
    private var arm = renderWindow.connection.player.mainArm // TODO: camera player entity
    private var skin: PlayerSkin? = null
    private var model: PlayerModel? = null
    private var mesh: ArmMesh = unsafeNull()

    override fun postInit() {
        shader.load()
        updateMesh()
    }

    private fun updateMesh() {
        this.mesh = ArmMesh(renderWindow)
        val skin = this.skin
        val model = this.model?.instance?.model?.model
        if (model != null && skin != null) {
            this.mesh.addArm(model, arm, skin.texture)
        }
        this.mesh.load()
    }


    override fun update() {
        val arm = renderWindow.connection.player.mainArm
        if (arm != this.arm) {
            this.mesh.unload()
            this.arm = arm
            init()
        }
        val model = renderWindow.connection.player.model.nullCast<PlayerModel>()
        this.model = model
        val skin = model?.skin ?: return
        if (this.skin == skin) {
            return
        }
        this.skin?.texture?.usages?.decrementAndGet()
        skin.texture.usages.incrementAndGet()
        this.skin = skin
    }

    private fun calculateTransform(): Mat4 {
        val screen = renderWindow.window.sizef
        val projection = GLM.perspective(60.0f.rad, screen.x / screen.y, CameraDefinition.NEAR_PLANE, CameraDefinition.FAR_PLANE)

        val model = this.model ?: return Mat4()
        val outliner = model.instance?.model?.model?.outliner?.find { it.name == if (arm == Arms.LEFT) "LEFT_ARM" else "RIGHT_ARM" } ?: return Mat4()
        outliner.origin.z = 15.0f.toBlockCoordinate()

        val matrix = FirstPersonArmAnimator(model).calculateTransform(outliner, 0.0f)
        val screenMatrix = Mat4()

        screenMatrix.translateAssign(Vec3(if (arm == Arms.LEFT) -0.2f else 0.2f, 0, 0)) // move inner side of arm to 0|0|0

        screenMatrix.translateAssign(Vec3(-18, -55, -10).fromBlockCoordinates())

        return projection * screenMatrix * matrix
    }

    override fun draw() {
        val skin = this.skin ?: return
        renderWindow.renderSystem.clear(IntegratedBufferTypes.DEPTH_BUFFER)
        renderWindow.renderSystem.disable(RenderingCapabilities.FACE_CULLING)
        renderWindow.renderSystem.enable(RenderingCapabilities.DEPTH_TEST)
        renderWindow.renderSystem.enable(RenderingCapabilities.BLENDING)
        renderWindow.renderSystem.depthMask = true
        mesh.unload()
        updateMesh()
        shader.use()
        shader.transform = calculateTransform()
        shader.textureIndexLayer = skin.texture.shaderId
        mesh.draw()
        renderWindow.renderSystem.clear(IntegratedBufferTypes.DEPTH_BUFFER)
    }


    companion object : OverlayFactory<ArmOverlay> {
        override fun build(renderWindow: RenderWindow): ArmOverlay {
            return ArmOverlay(renderWindow)
        }
    }
}