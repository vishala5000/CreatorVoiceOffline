package com.creatorvoice.offline

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var script: EditText
    private lateinit var status: TextView
    private lateinit var speaker: Spinner
    private lateinit var speed: Slider
    private lateinit var generate: MaterialButton
    private lateinit var play: MaterialButton
    private lateinit var export: MaterialButton

    private val executor = Executors.newSingleThreadExecutor()
    private val engine by lazy { TtsEngine(this) }
    private var lastAudio: Pair<FloatArray, Int>? = null
    private var lastFile: File? = null
    private var player: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        loadSpeakers()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(18))
            setBackgroundColor(0xFF101114.toInt())
        }

        fun lp(h: Int = ViewGroup.LayoutParams.WRAP_CONTENT) =
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h).apply {
                bottomMargin = dp(12)
            }

        val title = TextView(this).apply {
            text = "CreatorVoice Offline"
            textSize = 26f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        root.addView(title, lp())

        val subtitle = TextView(this).apply {
            text = "Private neural TTS • works without internet"
            textSize = 14f
            setTextColor(0xFFB8BDC8.toInt())
        }
        root.addView(subtitle, lp())

        script = EditText(this).apply {
            hint = "Paste your YouTube script here…"
            gravity = Gravity.TOP
            minLines = 12
            maxLines = 30
            textSize = 17f
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF777C86.toInt())
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(0xFF1A1C21.toInt())
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        root.addView(script, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ).apply { bottomMargin = dp(12) })

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        speaker = Spinner(this).apply {
            setBackgroundColor(0xFF1A1C21.toInt())
        }
        row.addView(speaker, LinearLayout.LayoutParams(0, dp(52), 1f).apply {
            rightMargin = dp(8)
        })

        speed = Slider(this).apply {
            valueFrom = 0.5f
            valueTo = 2.0f
            value = 1.0f
            stepSize = 0.05f
            contentDescription = "Speech speed"
        }
        row.addView(speed, LinearLayout.LayoutParams(0, dp(52), 1f))
        root.addView(row, lp())

        val speedLabel = TextView(this).apply {
            text = "Speed: 1.00×"
            textSize = 13f
            setTextColor(0xFFB8BDC8.toInt())
        }
        speed.addOnChangeListener { _, value, _ ->
            speedLabel.text = "Speed: ${"%.2f".format(Locale.US, value)}×"
        }
        root.addView(speedLabel, lp())

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        generate = button("Generate")
        play = button("Play")
        export = button("Export WAV")

        buttons.addView(generate, buttonLp())
        buttons.addView(play, buttonLp())
        buttons.addView(export, buttonLp())
        root.addView(buttons, lp())

        status = TextView(this).apply {
            text = "Ready"
            textSize = 13f
            setTextColor(0xFFB8BDC8.toInt())
        }
        root.addView(status, lp(44))

        setContentView(root)

        generate.setOnClickListener { synthesize() }
        play.setOnClickListener { playLast() }
        export.setOnClickListener { exportLast() }
    }

    private fun loadSpeakers() {
        status.text = "Loading local voice model…"
        executor.execute {
            try {
                val count = engine.speakerCount().coerceAtLeast(1)
                runOnUiThread {
                    speaker.adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        (0 until count).map { "Voice ${it + 1}" }
                    )
                    status.text = "$count offline voices available"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Model error: ${e.message}"
                    generate.isEnabled = false
                }
            }
        }
    }

    private fun synthesize() {
        val text = script.text.toString().trim()
        if (text.isEmpty()) {
            script.error = "Enter a script first"
            return
        }

        generate.isEnabled = false
        play.isEnabled = false
        export.isEnabled = false
        status.text = "Generating speech locally…"

        executor.execute {
            try {
                val result = engine.synthesize(
                    text,
                    speaker.selectedItemPosition.coerceAtLeast(0),
                    speed.value
                )
                lastAudio = result
                val file = File(cacheDir, "preview.wav")
                WavWriter.writePcm16(file, result.first, result.second)
                lastFile = file

                runOnUiThread {
                    generate.isEnabled = true
                    play.isEnabled = true
                    export.isEnabled = true
                    status.text = "Done • ${result.second} Hz • ${(result.first.size / result.second)} sec"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    generate.isEnabled = true
                    status.text = "Generation failed: ${e.message}"
                }
            }
        }
    }

    private fun playLast() {
        val file = lastFile ?: return
        player?.release()
        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            setOnCompletionListener { it.release() }
            start()
        }
        status.text = "Playing preview…"
    }

    private fun exportLast() {
        val audio = lastAudio ?: return
        val dir = getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: filesDir
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "creatorvoice_$stamp.wav")
        WavWriter.writePcm16(file, audio.first, audio.second)

        val uri = Uri.fromFile(file)
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(share, "Export WAV"))
        status.text = "Saved: ${file.name}"
    }

    override fun onDestroy() {
        player?.release()
        engine.release()
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun button(text: String) = MaterialButton(this).apply {
        this.text = text
        isAllCaps = false
    }

    private fun buttonLp() = LinearLayout.LayoutParams(0, dp(52), 1f).apply {
        marginEnd = dp(6)
    }

    private fun dp(value: Int) =
        (value * resources.displayMetrics.density).toInt()
}
