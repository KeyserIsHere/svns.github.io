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

package de.bixilon.minosoft.data.entities.entities.player.properties.textures

import com.fasterxml.jackson.annotation.JsonIgnore
import de.bixilon.kutil.hash.HashUtil.sha256
import de.bixilon.kutil.url.URLUtil.checkWeb
import de.bixilon.minosoft.assets.util.FileAssetsUtil
import de.bixilon.minosoft.assets.util.FileUtil
import de.bixilon.minosoft.util.logging.Log
import de.bixilon.minosoft.util.logging.LogLevels
import de.bixilon.minosoft.util.logging.LogMessageType
import java.io.File
import java.net.URL

open class PlayerTexture(
    val url: URL,
) {
    @JsonIgnore
    var data: ByteArray? = null
        private set

    init {
        url.checkWeb()

        check(urlMatches(url, ALLOWED_DOMAINS) && !urlMatches(url, BLOCKED_DOMAINS)) { "URL hostname is not allowed!" }
    }

    @Synchronized
    fun read(): ByteArray {
        this.data?.let { return it }
        val sha256 = when (url.host) {
            "textures.minecraft.net" -> url.file.split("/").last()
            else -> TODO("Can not get texture identifier: $url")
        }

        val diskPath = FileAssetsUtil.getPath(sha256)

        FileUtil.safeReadFile(diskPath, true)?.let {
            val data = it.readAllBytes()
            if (data.sha256() != sha256) {
                // hash mismatch, download again
                File(diskPath).delete()
                return@let
            }
            this.data = data
            return data
        }

        val input = url.openStream()
        if (input.available() > MAX_TEXTURE_SIZE) {
            throw IllegalStateException("Texture is too big: ${input.available()}!")
        }
        val (hash, data) = FileAssetsUtil.saveAndGet(input)
        if (sha256 != hash) {
            File(diskPath).delete()
            throw IllegalStateException("Hash mismatch (expected=$sha256, got=$hash)")
        }
        Log.log(LogMessageType.ASSETS, LogLevels.VERBOSE) { "Downloaded player texture ($url)" }
        return data
    }

    companion object {
        private const val MAX_TEXTURE_SIZE = 64 * 64 * 3 + 100 // width * height * rgb + some padding
        private val ALLOWED_DOMAINS = arrayOf(".minecraft.net", ".mojang.com")
        private val BLOCKED_DOMAINS = arrayOf("bugs.mojang.com", "education.minecraft.net", "feedback.minecraft.net") // pretty much guaranteed to not happen, texture data must always be signed by mojang. Taken from the original game.

        private fun urlMatches(url: URL, domains: Array<String>): Boolean {
            for (checkURL in domains) {
                if (url.host.endsWith(checkURL)) {
                    return true
                }
            }
            return false
        }
    }
}
