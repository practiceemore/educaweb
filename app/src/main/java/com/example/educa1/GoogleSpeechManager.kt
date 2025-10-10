package com.example.educa1

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.educa1.interfaces.VoiceCommandListener
import com.google.api.gax.rpc.ApiStreamObserver
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.*
import com.google.protobuf.ByteString
import java.io.InputStream
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import android.media.AudioAttributes
import android.media.SoundPool

object GoogleSpeechManager {

    private const val TAG = "SpeechManager"
    private const val ACCESS_KEY = "q8wjx1RkweCheaFxnPSVjc3xEwvpxFMubWtbxzeVAsDDV/GqQc90lw==" // <<< SUBSTITUA AQUI

    private enum class State { IDLE, LISTENING_FOR_WAKE_WORD, LISTENING_FOR_COMMAND }
    @Volatile private var currentState = State.IDLE

    // Gerenciadores e Callbacks
    private var porcupineManager: PorcupineManager? = null
    private lateinit var porcupineManagerCallback: PorcupineManagerCallback // Inicializado em 'initializePorcupine'
    private var speechClient: SpeechClient? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var soundPool: SoundPool? = null // <<< ADICIONE ESTA LINHA
    private var somAtivacaoId: Int = 0       // <<< ADICIONE ESTA LINHA

    // Apenas para o Google
    private var audioRecord: AudioRecord? = null
    @Volatile private var googleStreamingThread: Thread? = null
    private var requestObserver: ApiStreamObserver<StreamingRecognizeRequest>? = null

    // Variáveis de Controle
    private var activityRef: WeakReference<AppCompatActivity>? = null
    private var isPermissionGranted = false
    private var shouldBeListening = false
    private var commandJustExecuted = false
    private const val COMMAND_COOLDOWN_MS = 2000L
    private const val COMMAND_TIMEOUT_MS = 5000L

    // Configurações de Áudio para o Google
    private const val GOOGLE_SAMPLE_RATE = 16000
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    fun initialize(context: Context) {
        initializeSoundPool(context) // <<< ADICIONE ESTA LINHA NO INÍCIO
        initializeGoogleSpeech(context)
        initializePorcupine(context)
    }

    fun onPermissionResult(granted: Boolean) {
        isPermissionGranted = granted
        if (granted) tryToStartListening() else stopAllListening()
    }

    fun setAppInForeground(inForeground: Boolean) {
        shouldBeListening = inForeground
        if (inForeground) tryToStartListening() else stopAllListening()
    }

    fun registerActivity(activity: AppCompatActivity) { activityRef = WeakReference(activity) }
    fun unregisterActivity(activity: AppCompatActivity) { if (activityRef?.get() == activity) activityRef?.clear() }

    fun shutdown() {
        stopAllListening()
        soundPool?.release() // <<< ADICIONE ESTA LINHA
        soundPool = null     // <<< ADICIONE ESTA LINHA
        executor.execute {
            porcupineManager?.delete()
            speechClient?.shutdown()
        }
    }

    private fun initializeGoogleSpeech(context: Context) {
        // CORREÇÃO: Vamos rodar isso na thread do executor que já existe.
        executor.execute {
            try {
                Log.d(TAG, "Iniciando tarefa de criação do Google SpeechClient...")

                // Passo 1: Obter o fluxo do arquivo de credenciais
                val credentialsStream: InputStream = context.resources.openRawResource(R.raw.google_credentials)
                Log.d(TAG, "Fluxo de credenciais obtido com sucesso.")

                // Passo 2: Criar as credenciais
                val credentials = GoogleCredentials.fromStream(credentialsStream)
                    .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
                Log.d(TAG, "Objeto de credenciais criado com sucesso.")

                // Passo 3: Criar as configurações do serviço
                val speechSettings = SpeechSettings.newBuilder()
                    .setCredentialsProvider { credentials }
                    .build()
                Log.d(TAG, "Configurações do serviço criadas com sucesso.")

                // Passo 4: Criar o cliente final
                speechClient = SpeechClient.create(speechSettings)
                Log.i(TAG, ">>> SUCESSO: Google SpeechClient criado e pronto para uso! <<<")

            } catch (e: Exception) {
                // Se QUALQUER um dos passos acima falhar, este bloco será executado.
                // Isso nos dirá exatamente por que a inicialização está falhando.
                Log.e(TAG, "FALHA CRÍTICA AO INICIALIZAR O GOOGLE SPEECHCLIENT", e)
            }
        }
    }
    private fun initializePorcupine(context: Context) {
        porcupineManagerCallback = PorcupineManagerCallback {
            mainHandler.post {
                Log.i(TAG, ">>> Palavra de Ativação 'comando' detectada! <<<")
                switchToGoogleSpeech()
            }
        }

        try {
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(ACCESS_KEY)
                .setKeywordPath("comando_android.ppn")
                .setModelPath("porcupine_params_pt.pv")
                // A chamada .build() CORRETA, com DOIS argumentos
                .build(context, porcupineManagerCallback)
            Log.i(TAG, "PorcupineManager criado com sucesso.")
        } catch (e: PorcupineException) {
            Log.e(TAG, "FALHA CRÍTICA ao criar PorcupineManager", e)
        }
    }

    private fun tryToStartListening() {
        if (isPermissionGranted && shouldBeListening) {
            startPorcupineListening()
        }
    }

    private fun stopAllListening() {
        Log.w(TAG, "Parando todas as formas de escuta.")
        stopPorcupineListening()
        stopGoogleStreaming()
    }

    private fun switchToGoogleSpeech() {
        // Para a escuta do Porcupine imediatamente
        stopPorcupineListening()

        // <<< ADICIONE ESTAS LINHAS PARA TOCAR O SOM >>>
        if (somAtivacaoId != 0) {
            soundPool?.play(somAtivacaoId, 1.0f, 1.0f, 1, 0, 1.0f)
        }

        // CORREÇÃO: Adicionamos um "guarda" aqui.
        // Verificamos se o cliente do Google já foi criado pela tarefa em segundo plano.
        if (speechClient == null) {
            // Se ainda não estiver pronto, avisamos o usuário e voltamos a ouvir a wake word.
            Log.e(TAG, "Google SpeechClient ainda não está pronto. Tente o comando novamente em alguns segundos.")
            // Opcional: Você pode adicionar um Toast para avisar o usuário na tela.
            startPorcupineListening() // Volta para o estado seguro
            return // Sai da função para não continuar com um cliente nulo
        }

        // Se o cliente do Google estiver pronto, o código continua normalmente.
        startGoogleStreaming()
        mainHandler.postDelayed({
            if (currentState == State.LISTENING_FOR_COMMAND) {
                Log.w(TAG, "Timeout do comando. Voltando a ouvir a wake word.")
                startPorcupineListening()
            }
        }, COMMAND_TIMEOUT_MS)
    }

    @SuppressLint("MissingPermission")
    private fun startPorcupineListening() {
        if (currentState == State.LISTENING_FOR_WAKE_WORD || porcupineManager == null) return
        stopGoogleStreaming()
        currentState = State.LISTENING_FOR_WAKE_WORD
        try {
            // A chamada .start() CORRETA, com ZERO argumentos
            porcupineManager!!.start()
            Log.i(TAG, "Ouvindo pela palavra de ativação 'comando'...")
        } catch (e: PorcupineException) {
            Log.e(TAG, "Erro ao iniciar o PorcupineManager: ${e.message}")
        }
    }

    private fun stopPorcupineListening() {
        if (currentState != State.LISTENING_FOR_WAKE_WORD) return
        currentState = State.IDLE
        try {
            porcupineManager?.stop()
        } catch (e: PorcupineException) {
            Log.w(TAG, "Erro ao parar o PorcupineManager: ${e.message}")
        }
        Log.d(TAG, "Escuta do Porcupine parada.")
    }

    // --- CÓDIGO DO GOOGLE SPEECH E PROCESSAMENTO DE COMANDOS ---
    // (As funções abaixo permanecem as mesmas)

    @SuppressLint("MissingPermission")
    private fun startGoogleStreaming() {
        if (speechClient == null) {
            Log.e(TAG, "Google SpeechClient não está inicializado.")
            return
        }
        currentState = State.LISTENING_FOR_COMMAND

        val responseObserver = object : ApiStreamObserver<StreamingRecognizeResponse> {
            override fun onNext(response: StreamingRecognizeResponse) {
                if (response.resultsCount > 0) {
                    val result = response.getResults(0)
                    if (result.isFinal && result.alternativesCount > 0) {
                        val transcript = result.getAlternatives(0).transcript.trim()
                        if (transcript.isNotEmpty()) {
                            Log.i(TAG, "VOZ RECONHECIDA (FINAL): '$transcript'")
                            processCommand(transcript)
                        }
                    }
                }
            }
            override fun onError(t: Throwable) {
                Log.e(TAG, "ERRO NO STREAMING (Google): ${t.message}")
                mainHandler.post { startPorcupineListening() }
            }
            override fun onCompleted() {
                Log.d(TAG, "Streaming (Google) completado pelo servidor.")
            }
        }

        requestObserver = speechClient?.streamingRecognizeCallable()?.bidiStreamingCall(responseObserver)

        val recognitionConfig = RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
            .setLanguageCode("pt-BR")
            .setSampleRateHertz(GOOGLE_SAMPLE_RATE)
            .build()
        val streamingConfig = StreamingRecognitionConfig.newBuilder().setConfig(recognitionConfig).build()
        requestObserver?.onNext(StreamingRecognizeRequest.newBuilder().setStreamingConfig(streamingConfig).build())

        googleStreamingThread = Thread {
            val bufferSize = AudioRecord.getMinBufferSize(GOOGLE_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, GOOGLE_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize)

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord falhou ao inicializar para o Google.")
                return@Thread
            }

            val buffer = ByteArray(bufferSize)
            audioRecord?.startRecording()
            Log.i(TAG, "Escuta de comando iniciada (Google).")

            while (currentState == State.LISTENING_FOR_COMMAND) {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (bytesRead > 0) {
                    val request = StreamingRecognizeRequest.newBuilder()
                        .setAudioContent(ByteString.copyFrom(buffer, 0, bytesRead)).build()
                    try { requestObserver?.onNext(request) } catch (e: Exception) { break }
                }
            }
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            try { requestObserver?.onCompleted() } catch (e: Exception) { /* Ignora */ }
        }
        googleStreamingThread?.start()
    }

    private fun stopGoogleStreaming() {
        // Se não estivermos no estado correto, não há nada a fazer.
        if (currentState != State.LISTENING_FOR_COMMAND) return

        // Muda o estado primeiro para que o loop na thread pare.
        currentState = State.IDLE

        // Desliga a thread de forma segura.
        try {
            googleStreamingThread?.join(500)
        } catch (e: InterruptedException) {
            Log.w(TAG, "Thread do Google interrompida durante o join.")
        }
        googleStreamingThread = null

        // CORREÇÃO: Encerra o stream do Google de forma segura.
        // O try-catch aqui é crucial para evitar o crash se o stream já estiver fechado.
        try {
            requestObserver?.onCompleted()
        } catch (e: IllegalStateException) {
            // Isso é esperado se o servidor já fechou a conexão por timeout. Apenas logamos.
            Log.w(TAG, "Stream do Google já estava fechado. Isso é normal em caso de timeout.")
        } catch (e: Exception) {
            // Captura outros erros inesperados ao fechar o stream.
            Log.e(TAG, "Erro inesperado ao fechar o stream do Google.", e)
        }

        requestObserver = null
        Log.d(TAG, "Escuta do Google parada.")
    }

    private fun processCommand(text: String) {
        if (commandJustExecuted || text.isEmpty()) {
            startPorcupineListening()
            return
        }
        val actualCommand = text.trim().lowercase()
        when {
            actualCommand == "voltar" -> {
                activityRef?.get()?.let { mainHandler.post { it.onBackPressedDispatcher.onBackPressed() } }
                activateCooldown()
            }
            actualCommand.startsWith("nova pasta") -> {
                val folderName = actualCommand.substringAfter("nova pasta").trim()
                if (folderName.isNotEmpty()) {
                    sendCommandToActivity("CREATE_FOLDER", folderName)
                }
                activateCooldown()
            }
            else -> {
                sendCommandToActivity("FIND_AND_CLICK", actualCommand)
                activateCooldown()
            }
        }
    }

    private fun sendCommandToActivity(command: String, data: String) {
        activityRef?.get()?.let { activity ->
            if (activity is VoiceCommandListener) {
                mainHandler.post {
                    activity.onVoiceCommand(command, data)
                }
            }
        }
    }

    private fun activateCooldown() {
        commandJustExecuted = true
        mainHandler.postDelayed({
            commandJustExecuted = false
            startPorcupineListening()
        }, COMMAND_COOLDOWN_MS)
    }
    // Em GoogleSpeechManager.kt

    private fun initializeSoundPool(context: Context) {
        val audioAttributes = AudioAttributes.Builder()
            // Esta configuração é ideal para sons de feedback de UI.
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1) // Só precisamos tocar um som por vez.
            .setAudioAttributes(audioAttributes)
            .build()

        // Carrega o nosso som da pasta res/raw e guarda seu ID.
        // Substitua 'ativacao_sonora' pelo nome do seu arquivo.
        somAtivacaoId = soundPool?.load(context, R.raw.ativacao_sonora, 1) ?: 0

        if (somAtivacaoId == 0) {
            Log.e(TAG, "Falha ao carregar o som de ativação.")
        }
    }
}