package de.culture4life.luca.dataaccess

import android.util.Base64
import com.nexenio.rxkeystore.util.RxBase64
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.TimeUnit

data class NotificationDataChunk(
    val version: Int,
    val algorithm: Int,
    val hashLength: Int,
    val creationTimestamp: Long,
    val previousChunkId: String,
    val hashedTraceIds: ArrayList<String>
) {

    companion object Factory {

        fun from(responseBody: ByteArray): NotificationDataChunk {
            require(responseBody.size >= 32) { "Invalid response body length: " + responseBody.size }
            val byteBuffer = ByteBuffer.wrap(responseBody)
            val version = byteBuffer.get().toInt()
            require(version == 1) { "Invalid version: $version" }

            val algorithm = byteBuffer.get().toInt()
            require(algorithm == 0) { "Invalid algorithm: $algorithm" }

            val hashLength = byteBuffer.get().toInt()
            require(!(hashLength < 4 || hashLength > 32)) { "Invalid hash length: $hashLength" }

            val creationTimestamp = byteBuffer.long
            val isValidTimestamp =
                creationTimestamp > System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30) &&
                        creationTimestamp < System.currentTimeMillis()
            require(isValidTimestamp) { "Invalid creation timestamp: $creationTimestamp" }

            byteBuffer.position(16)
            val previousChunkHash = ByteArray(16)
            byteBuffer.get(previousChunkHash)
            val previousChunkId = RxBase64.encode(previousChunkHash, Base64.NO_WRAP).blockingGet()

            val hashedTraceIds = ArrayList<String>()
            while (byteBuffer.hasRemaining()) {
                val hash = ByteArray(hashLength)
                byteBuffer.get(hash)
                hashedTraceIds.add(RxBase64.encode(hash, Base64.NO_WRAP).blockingGet())
            }

            return NotificationDataChunk(
                version,
                algorithm,
                hashLength,
                creationTimestamp,
                previousChunkId,
                hashedTraceIds
            )
        }

    }

    override fun toString(): String {
        return "Chunk(version=$version, algorithm=$algorithm, hashLength=$hashLength, creationTimestamp=$creationTimestamp, previousChunkId='$previousChunkId', hashedTraceIds=${hashedTraceIds.size})"
    }

}