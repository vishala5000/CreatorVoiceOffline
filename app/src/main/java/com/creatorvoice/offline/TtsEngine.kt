package com.creatorvoice.offline

import android.content.Context
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.getOfflineTtsConfig

class TtsEngine(private val context: Context) {
    private var tts: OfflineTts? = null

    @Synchronized
    private fun getTts(): OfflineTts {
        return tts ?: run {
            val config = getOfflineTtsConfig(
                modelDir = MODEL_DIR,
                modelName = "model.onnx",
                acousticModelName = "",
                vocoder = "",
                voices = "voices.bin",
                lexicon = "",
                dataDir = "$MODEL_DIR/espeak-ng-data",
                dictDir = "",
                ruleFsts = "",
                ruleFars = "",
                numThreads = 4
            )
            OfflineTts(assetManager = context.assets, config = config).also {
                tts = it
            }
        }
    }

    fun speakerCount(): Int = getTts().numSpeakers()

    fun synthesize(text: String, speakerId: Int, speed: Float): Pair<FloatArray, Int> {
        require(text.isNotBlank()) { "Text is empty." }
        val audio = getTts().generate(
            text = text.trim(),
            sid = speakerId.coerceAtLeast(0),
            speed = speed.coerceIn(0.5f, 2.0f)
        )
        return audio.samples to audio.sampleRate
    }

    fun release() {
        tts?.release()
        tts = null
    }

    companion object {
        const val MODEL_DIR = "kokoro-en-v0_19"
    }
}
