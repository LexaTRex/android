package de.culture4life.luca.network.pojo

import de.culture4life.luca.util.TimeUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class DailyKeyPair(
        val keyId: Int,
        val createdAt: Long,
        val issuerId: String,
        val publicKey: String,
        val signature: String
) {
    fun getEncodedKeyId(): ByteArray = ByteBuffer.allocate(4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(keyId)
            .array()

    fun getEncodedCreatedAt(): ByteArray = TimeUtil.encodeUnixTimestamp(createdAt).blockingGet()
}