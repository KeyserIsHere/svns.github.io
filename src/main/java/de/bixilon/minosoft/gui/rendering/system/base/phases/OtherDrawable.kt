package de.bixilon.minosoft.gui.rendering.system.base.phases

import de.bixilon.minosoft.gui.rendering.Renderer

interface OtherDrawable : Renderer {
    val skipOther: Boolean
        get() = false

    fun setupOther() {
        renderSystem.reset()
    }

    fun drawOther()
}