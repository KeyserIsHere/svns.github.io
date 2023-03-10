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
package de.bixilon.minosoft.protocol.protocol

import de.bixilon.kotlinglm.vec2.Vec2
import de.bixilon.kotlinglm.vec2.Vec2d
import de.bixilon.kotlinglm.vec2.Vec2i
import de.bixilon.kotlinglm.vec3.Vec3
import de.bixilon.kotlinglm.vec3.Vec3d
import de.bixilon.kotlinglm.vec3.Vec3i
import de.bixilon.kutil.compression.zlib.GzipUtil.decompress
import de.bixilon.kutil.uuid.UUIDUtil.toUUID
import de.bixilon.minosoft.data.direction.Directions
import de.bixilon.minosoft.data.entities.EntityRotation
import de.bixilon.minosoft.data.entities.Poses
import de.bixilon.minosoft.data.registries.identified.ResourceLocation
import de.bixilon.minosoft.data.tags.Tag
import de.bixilon.minosoft.data.text.ChatComponent
import de.bixilon.minosoft.util.json.Jackson
import de.bixilon.minosoft.util.nbt.tag.NBTTagTypes
import java.nio.charset.StandardCharsets
import java.util.*


@Deprecated("Kutil")
open class InByteBuffer {
    private val bytes: ByteArray
    var pointer = 0

    constructor(bytes: ByteArray) {
        this.bytes = bytes
    }

    constructor(buffer: InByteBuffer) {
        bytes = buffer.bytes.clone()
        pointer = buffer.pointer
    }

    val size: Int
        get() = bytes.size

    val bytesLeft: Int
        get() = size - pointer

    inline fun <reified T> readArray(length: Int = readVarInt(), reader: () -> T): Array<T> {
        check(length <= size) { "Trying to allocate too much memory!" }
        val array: MutableList<T> = mutableListOf()
        for (i in 0 until length) {
            array.add(i, reader())
        }
        return array.toTypedArray()
    }

    fun readByte(): Byte {
        if (pointer >= size) {
            throw ByteBufferUnderflowException(pointer, size)
        }
        return bytes[pointer++]
    }

    open fun readByteArray(array: ByteArray) {
        System.arraycopy(bytes, pointer, array, 0, array.size)
        pointer += array.size
    }

    open fun readByteArray(length: Int = readVarInt()): ByteArray {
        check(length <= bytes.size) { "Trying to allocate too much memory!" }
        val array = ByteArray(length)
        System.arraycopy(bytes, pointer, array, 0, length)
        pointer += length
        return array
    }

    fun readUnsignedByte(): Int {
        return readByte().toInt() and ((1 shl Byte.SIZE_BITS) - 1)
    }


    fun readShort(): Short {
        return (readUnsignedByte() shl Byte.SIZE_BITS or readUnsignedByte()).toShort()
    }

    fun readShortArray(length: Int = readVarInt()): ShortArray {
        check(length <= bytes.size / Short.SIZE_BYTES) { "Trying to allocate too much memory!" }
        val array = ShortArray(length)
        for (i in 0 until length) {
            array[i] = readShort()
        }
        return array
    }

    fun readUnsignedShort(): Int {
        return readShort().toInt() and ((1 shl Short.SIZE_BITS) - 1)
    }

    fun readVelocity(): Vec3d {
        return Vec3d(readShort(), readShort(), readShort()) / ProtocolDefinition.VELOCITY_NETWORK_DIVIDER
    }

    fun readInt(): Int {
        return (readUnsignedShort() shl Short.SIZE_BITS or readUnsignedShort())
    }

    fun readIntArray(length: Int = readVarInt()): IntArray {
        check(length <= bytes.size / Int.SIZE_BYTES) { "Trying to allocate too much memory!" }
        val array = IntArray(length)
        for (i in 0 until length) {
            array[i] = readInt()
        }
        return array
    }

    fun readUnsignedInt(): Long {
        return readInt().toLong() and ((1L shl Int.SIZE_BITS) - 1)
    }


    fun readVarInt(): Int {
        var byteCount = 0
        var result = 0
        var read: Int
        do {
            read = readUnsignedByte()
            result = result or (read and 0x7F shl (Byte.SIZE_BITS - 1) * byteCount)
            byteCount++
            require(byteCount <= Int.SIZE_BYTES + 1) { "VarInt is too big" }
        } while (read and 0x80 != 0)

        return result
    }


    @JvmOverloads
    fun readVarIntArray(length: Int = readVarInt()): IntArray {
        check(length <= bytes.size) { "Trying to allocate too much memory!" }
        val array = IntArray(length)
        for (i in 0 until length) {
            array[i] = readVarInt()
        }
        return array
    }

    fun readUnsignedVarInt(): Long {
        return readVarInt().toLong() and ((1 shl Int.SIZE_BITS) - 1).toLong()
    }


    fun readLong(): Long {
        return (readUnsignedInt() shl Int.SIZE_BITS or readUnsignedInt())
    }

    fun readLongArray(length: Int = readVarInt()): LongArray {
        check(length <= bytes.size / Long.SIZE_BYTES) { "Trying to allocate too much memory!" }
        val array = LongArray(length)
        for (i in 0 until length) {
            array[i] = readLong()
        }
        return array
    }

    fun readLongArray(target: LongArray, size: Int = readVarInt()) {
        for (i in 0 until size) {
            target[i] = readLong()
        }
    }


    fun readVarLong(): Long {
        var byteCount = 0
        var result = 0L
        var read: Int
        do {
            read = readUnsignedByte()
            result = result or ((read and 0x7F shl (Byte.SIZE_BITS - 1) * byteCount).toLong())
            byteCount++
            require(byteCount <= Long.SIZE_BYTES + 1) { "VarLong is too big" }
        } while (read and 0x80 != 0)

        return result
    }

    fun readVarLongArray(length: Int = readVarInt()): LongArray {
        check(length <= bytes.size) { "Trying to allocate too much memory!" }
        val array = LongArray(length)
        for (i in 0 until length) {
            array[i] = readVarLong()
        }
        return array
    }

    fun readFloat(): Float {
        return Float.fromBits(readInt())
    }

    fun readFloatArray(length: Int = readVarInt()): FloatArray {
        check(length <= bytes.size / Float.SIZE_BYTES) { "Trying to allocate too much memory!" }
        val array = FloatArray(length)
        for (i in 0 until length) {
            array[i] = readFloat()
        }
        return array
    }


    fun readDouble(): Double {
        return Double.fromBits(readLong())
    }

    fun readDoubleArray(length: Int = readVarInt()): DoubleArray {
        check(length <= bytes.size / Double.SIZE_BYTES) { "Trying to allocate too much memory!" }
        val array = DoubleArray(length)
        for (i in 0 until length) {
            array[i] = readDouble()
        }
        return array
    }


    fun readFixedPointNumberInt(): Double {
        return readInt() / 32.0
    }

    fun readFixedPointNumberByte(): Double {
        return readByte() / 32.0
    }

    fun readBoolean(): Boolean {
        return readUnsignedByte() == 1
    }

    fun readString(length: Int = readVarInt()): String {
        val string = String(readByteArray(length), StandardCharsets.UTF_8)
        check(string.length <= ProtocolDefinition.STRING_MAX_LENGTH) { "String max string length exceeded ${string.length} > ${ProtocolDefinition.STRING_MAX_LENGTH}" }
        return string
    }

    fun readNullString(length: Int = readVarInt()): String? {
        val string = readString(length)
        if (string.isBlank()) {
            return null
        }
        return string
    }

    fun readUUID(): UUID {
        return UUID(readLong(), readLong())
    }

    fun readUUIDString(): UUID {
        return readString().toUUID()
    }

    fun readJson(): Map<String, Any> {
        return Jackson.MAPPER.readValue(readString(), Jackson.JSON_MAP_TYPE)
    }

    open fun readChatComponent(): ChatComponent {
        return ChatComponent.of(readString(), restrictedMode = true)
    }

    fun readDirection(): Directions {
        return Directions.VALUES[readVarInt()]
    }

    fun readPose(): Poses {
        return Poses[readVarInt()]
    }

    fun readRest(): ByteArray {
        return readByteArray(length = size - pointer)
    }

    fun readAngle(): Int {
        return (readByte() * ProtocolDefinition.ROTATION_ANGLE_DIVIDER).toInt()
    }

    fun readEntityRotation(): EntityRotation {
        return EntityRotation(readAngle().toFloat(), readAngle().toFloat())
    }

    fun readVec2f(): Vec2 {
        return Vec2(readFloat(), readFloat())
    }

    fun readVec2d(): Vec2d {
        return Vec2d(readDouble(), readDouble())
    }

    fun readVec3f(): Vec3 {
        return Vec3(readFloat(), readFloat(), readFloat())
    }

    open fun readVec3d(): Vec3d {
        return Vec3d(readDouble(), readDouble(), readDouble())
    }

    fun readByteBlockPosition(): Vec3i {
        return Vec3i(readInt(), readByte(), readInt())
    }

    fun readShortBlockPosition(): Vec3i {
        return Vec3i(readInt(), readShort(), readInt())
    }

    fun readIntBlockPosition(): Vec3i {
        return Vec3i(readInt(), readInt(), readInt())
    }

    fun readResourceLocation(): ResourceLocation {
        return ResourceLocation.of(readString())
    }

    fun readChunkPosition(): Vec2i {
        return Vec2i(readInt(), readInt())
    }

    fun readUnsignedShortsLE(length: Int): IntArray {
        require(length <= size) { "Trying to allocate to much memory" }
        val ret = IntArray(length)
        for (i in 0 until length) {
            ret[i] = readUnsignedByte() or (readUnsignedByte() shl 8)
        }
        return ret
    }

    fun readNBTTag(tagType: NBTTagTypes): Any? {
        return when (tagType) {
            NBTTagTypes.END -> null
            NBTTagTypes.BYTE -> readByte()
            NBTTagTypes.SHORT -> readShort()
            NBTTagTypes.INT -> readInt()
            NBTTagTypes.LONG -> readLong()
            NBTTagTypes.FLOAT -> readFloat()
            NBTTagTypes.DOUBLE -> readDouble()
            NBTTagTypes.BYTE_ARRAY -> readByteArray(readInt())
            NBTTagTypes.STRING -> readString(readUnsignedShort())
            NBTTagTypes.LIST -> {
                val listType = NBTTagTypes[readUnsignedByte()]
                val length = readInt()
                val out: MutableList<Any> = mutableListOf()
                for (i in 0 until length) {
                    readNBTTag(listType)?.let { out.add(it) }
                }
                out
            }

            NBTTagTypes.COMPOUND -> {
                val out: MutableMap<String, Any> = mutableMapOf()
                while (true) {
                    val compoundTagType = NBTTagTypes[readUnsignedByte()]
                    if (compoundTagType === NBTTagTypes.END) {
                        // end tag
                        break
                    }
                    val tagName: String = readString(readUnsignedShort())
                    val tag = readNBTTag(compoundTagType) ?: continue
                    out[tagName] = tag
                }
                out
            }

            NBTTagTypes.INT_ARRAY -> readIntArray(readInt())
            NBTTagTypes.LONG_ARRAY -> readLongArray(readInt())
        }
    }

    fun readNBTTag(compressed: Boolean): Any? {
        if (compressed) {
            val length = readShort().toInt()
            return if (length == -1) {
                // no nbt data here...
                null
            } else {
                InByteBuffer(readByteArray(length).decompress()).readNBTTag(false)
            }
        }
        val type = NBTTagTypes[readUnsignedByte()]
        if (type === NBTTagTypes.COMPOUND) {
            var name = readString(readUnsignedShort()) // ToDo: Should this name be ignored?
        }
        return readNBTTag(type)
    }

    fun getBase64(): String {
        return String(Base64.getEncoder().encode(readRest()))
    }

    fun <T> readTag(idResolver: (Int) -> T): Pair<ResourceLocation, Tag<T>> {
        val resourceLocation = readResourceLocation()
        val ids = readVarIntArray()
        val items: MutableSet<T> = mutableSetOf()
        for (id in ids) {
            items += idResolver(id)
        }
        return Pair(resourceLocation, Tag(items))
    }

    fun <T> readOptional(reader: InByteBuffer.() -> T): T? {
        return if (readBoolean()) {
            reader(this)
        } else {
            null
        }
    }
}
