package com.creatorvoice.offline

import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

object WavWriter {
    fun writePcm16(file: File, samples: FloatArray, sampleRate: Int) {
        FileOutputStream(file).use { out ->
            val dataSize = samples.size * 2
            val byteRate = sampleRate * 2

            fun le16(v: Int) {
                out.write(v and 0xff)
                out.write((v ushr 8) and 0xff)
            }

            fun le32(v: Int) {
                out.write(v and 0xff)
                out.write((v ushr 8) and 0xff)
                out.write((v ushr 16) and 0xff)
                out.write((v ushr 24) and 0xff)
            }

            out.write("RIFF".toByteArray(Charsets.US_ASCII))
            le32(36 + dataSize)
            out.write("WAVE".toByteArray(Charsets.US_ASCII))
            out.write("fmt ".toByteArray(Charsets.US_ASCII))
            le32(16)
            le16(1)       // PCM
            le16(1)       // mono
            le32(sampleRate)
            le32(byteRate)
            le16(2)       // block align
            le16(16)      // bits/sample
            out.write("data".toByteArray(Charsets.US_ASCII))
            le32(dataSize)

            for (sample in samples) {
                val clamped = sample.coerceIn(-1f, 1f)
                val value = (clamped * 32767f).roundToInt()
                le16(value)
            }
        }
    }
}
