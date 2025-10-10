package com.example.educa1.activitys

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.educa1.databinding.ActivityPianoBinding
import com.example.educa1.views.Key
import android.graphics.RectF
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sin

class PianoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPianoBinding

    // --- Início das variáveis do Sintetizador ---
    private var audioTrack: AudioTrack? = null
    private lateinit var audioThread: Thread
    private var isPlaying = false
    private val sampleRate = 44100

    // --- Início das constantes do Envelope de Amplitude ---
    // Duração do fade-in (ataque) em milissegundos. Um valor curto evita atraso.
    private val ATTACK_DURATION_MS = 10.0
    // Duração do fade-out (release) em milissegundos. Um valor maior simula a nota soando após soltar a tecla.
    private val RELEASE_DURATION_MS = 100.0

    // Calcula o quanto o volume deve mudar a cada sample para atingir a duração desejada
    private val attackIncrement = 1.0 / (ATTACK_DURATION_MS / 1000.0 * sampleRate)
    private val releaseIncrement = 1.0 / (RELEASE_DURATION_MS / 1000.0 * sampleRate)
    // --- Fim das constantes do Envelope de Amplitude ---

    // Data class interna modificada para guardar o estado do envelope
    private data class NoteState(
        val frequency: Double,
        var phase: Double = 0.0,
        var volume: Double = 0.0,      // Volume atual da nota (0.0 a 1.0)
        var isReleasing: Boolean = false // Flag para indicar se a nota está em fade-out
    )

    private val activeNotes = ConcurrentHashMap<Double, NoteState>()

    private val noteFrequencies = listOf(
        261.63, 277.18, 293.66, 311.13, 329.63, 349.23,
        369.99, 392.00, 415.30, 440.00, 466.16, 493.88,
        523.25, 554.37, 587.33, 622.25, 659.25, 698.46,
        739.99, 783.99, 830.61, 880.00, 932.33, 987.77
    )
    // --- Fim das variáveis do Sintetizador ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPianoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPianoView()
    }

    private fun setupAudioTrack() {
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }


    private fun startAudioGeneration() {
        isPlaying = true
        audioTrack?.play()

        audioThread = Thread {
            val bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val buffer = ShortArray(bufferSize)

            while (isPlaying) {
                // Lista para guardar as notas cujo fade-out terminou, para remoção segura
                val notesToRemove = mutableListOf<Double>()

                for (i in buffer.indices) {
                    var sampleValue = 0.0
                    // Pega uma "foto" das notas ativas para processar neste ciclo
                    val notesToPlay = activeNotes.values

                    // Itera sobre cada nota ativa para calcular seu volume e onda
                    notesToPlay.forEach { note ->
                        // --- LÓGICA DO ENVELOPE (FADE-IN / FADE-OUT) ---
                        if (note.isReleasing) {
                            // Se está em fade-out, diminui o volume
                            note.volume -= releaseIncrement
                            if (note.volume < 0) note.volume = 0.0
                        } else if (note.volume < 1.0) {
                            // Se está em fade-in, aumenta o volume
                            note.volume += attackIncrement
                            if (note.volume > 1.0) note.volume = 1.0
                        }

                        // Só processa a onda se o volume for audível
                        if (note.volume > 0) {
                            // Adiciona a onda da nota (multiplicada pelo seu volume atual) ao valor final do sample
                            sampleValue += sin(note.phase) * note.volume
                            note.phase += 2.0 * Math.PI * note.frequency / sampleRate
                        } else if (note.isReleasing) {
                            // Se o volume chegou a zero durante o fade-out, marca para remoção
                            notesToRemove.add(note.frequency)
                        }
                    }

                    // Normaliza o volume para evitar clipping, mas com um teto de 1.0 para não silenciar notas únicas
                    val normalizationFactor = if (notesToPlay.size > 1) notesToPlay.size.toDouble() else 1.0
                    sampleValue /= normalizationFactor
                    buffer[i] = (sampleValue * Short.MAX_VALUE).toInt().toShort()
                }

                // Remove as notas que terminaram o fade-out do mapa principal
                notesToRemove.forEach { freq -> activeNotes.remove(freq) }

                audioTrack?.write(buffer, 0, buffer.size)
            }
        }
        audioThread.start()
    }


    private fun stopAudioGeneration() {
        isPlaying = false
        try {
            audioThread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        audioTrack?.stop()
        audioTrack?.flush()
    }


    private fun setupPianoView() {
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        val whiteKeyCount = 14
        val whiteKeyWidth = screenWidth / whiteKeyCount
        val blackKeyWidth = whiteKeyWidth * 0.6f
        val blackKeyHeight = screenHeight * 0.55f

        val pianoKeys = mutableListOf<Key>()
        var whiteKeyIndex = 0

        for (i in 0 until 24) {
            val noteInOctave = i % 12
            val frequency = noteFrequencies[i]

            if (isBlack(noteInOctave)) {
                val left = (whiteKeyIndex - 1) * whiteKeyWidth + (whiteKeyWidth - blackKeyWidth / 2)
                pianoKeys.add(Key(RectF(left, 0f, left + blackKeyWidth, blackKeyHeight), true, frequency))
            } else {
                val left = whiteKeyIndex * whiteKeyWidth
                pianoKeys.add(Key(RectF(left, 0f, left + whiteKeyWidth, screenHeight), false, frequency))
                whiteKeyIndex++
            }
        }

        binding.pianoView.setKeys(pianoKeys) { freq, isPressed ->
            if (isPressed) {
                // Se a nota não existe ou está em fade-out, cria uma nova (permitindo re-tocar uma nota rapidamente)
                if (!activeNotes.containsKey(freq) || activeNotes[freq]?.isReleasing == true) {
                    activeNotes[freq] = NoteState(freq)
                }
            } else {
                // Ao soltar a tecla, não remove a nota, apenas ativa a flag de "releasing"
                activeNotes[freq]?.isReleasing = true
            }
        }
    }

    private fun isBlack(noteInOctave: Int): Boolean {
        return when (noteInOctave) {
            1, 3, 6, 8, 10 -> true
            else -> false
        }
    }

    override fun onResume() {
        super.onResume()
        setupAudioTrack()
        startAudioGeneration()
    }

    override fun onPause() {
        super.onPause()
        stopAudioGeneration()
        audioTrack?.release()
        audioTrack = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::audioThread.isInitialized && audioThread.isAlive) {
            stopAudioGeneration()
        }
        audioTrack?.release()
    }
}