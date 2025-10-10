package com.example.educa1.activitys

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.example.educa1.R
import com.google.android.material.button.MaterialButton
import kotlin.math.pow
import kotlin.math.sin

class TeremimActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var scaleToggleButton: MaterialButton
    private lateinit var scaleTypeButton: MaterialButton
    private lateinit var keyButton: MaterialButton
    private lateinit var touchButton: MaterialButton

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var volume = 0.0f
    private var frequency = 440.0 // A4 note
    private var isMajorScale = true
    private var isDiatonicScale = true
    private var isButtonPressed = false
    private var currentKey = 0 // 0 = C, 1 = C#, 2 = D, etc.
    private var phase = 0.0

    private val sampleRate = 44100
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    private val handler = Handler(Looper.getMainLooper())
    private val fadeInDuration = 100L // Duração do fade in em milissegundos
    private val fadeInSteps = 10 // Número de passos para o fade in
    private val volumeStep = 1.0f / fadeInSteps

    // Base frequencies for C4 (middle C)
    private val baseFrequencies = listOf(
        261.63, // C4
        277.18, // C#4
        293.66, // D4
        311.13, // D#4
        329.63, // E4
        349.23, // F4
        369.99, // F#4
        392.00, // G4
        415.30, // G#4
        440.00, // A4
        466.16, // A#4
        493.88  // B4
    )

    // Scale patterns (intervals from root note)
    private val majorScalePattern = listOf(0, 2, 4, 5, 7, 9, 11, 12) // C, D, E, F, G, A, B, C
    private val minorScalePattern = listOf(0, 2, 3, 5, 7, 8, 10, 12) // C, D, Eb, F, G, Ab, Bb, C
    private val majorPentatonicPattern = listOf(0, 2, 4, 7, 9) // C, D, E, G, A
    private val minorPentatonicPattern = listOf(0, 2, 3, 7, 8) // C, D, Eb, G, Ab

    // Key names
    private val keyNames = listOf(
        "Dó", "Dó#", "Ré", "Ré#", "Mi", "Fá",
        "Fá#", "Sol", "Sol#", "Lá", "Lá#", "Si"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teremim)

        // Initialize views
        scaleToggleButton = findViewById(R.id.scaleToggleButton)
        scaleTypeButton = findViewById(R.id.scaleTypeButton)
        keyButton = findViewById(R.id.keyButton)
        touchButton = findViewById(R.id.touchButton)

        scaleToggleButton.setOnClickListener {
            isMajorScale = !isMajorScale
            scaleToggleButton.text = if (isMajorScale) "Escala Maior" else "Escala Menor"
        }

        scaleTypeButton.setOnClickListener {
            isDiatonicScale = !isDiatonicScale
            scaleTypeButton.text = if (isDiatonicScale) "Escala Diatônica" else "Escala Pentatônica"
        }

        keyButton.setOnClickListener {
            currentKey = (currentKey + 1) % 12
            keyButton.text = "Tonalidade: ${keyNames[currentKey]}"
        }

        touchButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isButtonPressed = true
                    touchButton.setBackgroundColor(getColor(android.R.color.holo_blue_light))
                    startPlaying()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isButtonPressed = false
                    touchButton.setBackgroundColor(getColor(android.R.color.holo_blue_dark))
                    stopPlaying()
                }
            }
            true
        }

        // Initialize sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Initialize audio
        setupAudioTrack()
    }

    private fun setupAudioTrack() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private fun getTransposedFrequencies(): List<Double> {
        val pattern = when {
            isDiatonicScale && isMajorScale -> majorScalePattern
            isDiatonicScale && !isMajorScale -> minorScalePattern
            !isDiatonicScale && isMajorScale -> majorPentatonicPattern
            else -> minorPentatonicPattern
        }

        return pattern.map { interval ->
            val noteIndex = (currentKey + interval) % 12
            val octave = (currentKey + interval) / 12
            baseFrequencies[noteIndex] * 2.0.pow(octave)
        }
    }

    private fun startPlaying() {
        if (!isPlaying) {
            isPlaying = true
            volume = 0.0f
            phase = 0.0
            audioTrack?.play()
            startFadeIn()
            startAudioThread()
        }
    }

    private fun stopPlaying() {
        isPlaying = false
        audioTrack?.stop()
        audioTrack?.flush()
    }

    private fun startFadeIn() {
        var currentStep = 0
        handler.post(object : Runnable {
            override fun run() {
                if (isPlaying && currentStep < fadeInSteps) {
                    volume += volumeStep
                    currentStep++
                    handler.postDelayed(this, fadeInDuration / fadeInSteps)
                }
            }
        })
    }

    private fun startAudioThread() {
        Thread {
            val buffer = ShortArray(bufferSize)
            while (isPlaying) {
                for (i in buffer.indices) {
                    if (isButtonPressed) {
                        buffer[i] = (sin(phase) * volume * Short.MAX_VALUE).toInt().toShort()
                        phase += 2.0 * Math.PI * frequency / sampleRate
                        if (phase > 2.0 * Math.PI) {
                            phase -= 2.0 * Math.PI
                        }
                    } else {
                        buffer[i] = 0
                    }
                }
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }.start()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]

            // Map accelerometer values to volume and note
            // Y axis controls volume (0.0 to 1.0)
            volume = (y + 10) / 20.0f
            if (volume < 0) volume = 0.0f
            volume = volume.coerceIn(0.0f, 1.0f)

            // X axis controls note selection (inverted)
            val frequencies = getTransposedFrequencies()
            val noteIndex = ((10 - x) / 20.0 * (frequencies.size - 1)).toInt()
                .coerceIn(0, frequencies.size - 1)
            frequency = frequencies[noteIndex]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlaying()
        audioTrack?.release()
        audioTrack = null
    }
}