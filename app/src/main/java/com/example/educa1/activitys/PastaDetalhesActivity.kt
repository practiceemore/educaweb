package com.example.educa1.activitys

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.educa1.adapters.ImageAdapter
import com.example.educa1.databinding.ActivityPastaDetalhesBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


import android.speech.tts.TextToSpeech // IMPORTANTE
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.example.educa1.R
import com.example.educa1.adapters.ImageInteractionListener
import java.util.Locale // IMPORTANTE

import android.media.MediaPlayer // NOVO IMPORT
import android.widget.Toast
import androidx.core.view.GestureDetectorCompat // NOVO IMPORT (se o Android Studio não adicionar)

// A classe de dados que representa cada item da nossa galeria
// A classe de dados agora tem um campo opcional para o áudio
data class ImagemItem(
    val uriString: String,
    val nome: String,
    val audioUriString: String? = null // NOVO CAMPO
)
//class PastaDetalhesActivity : AppCompatActivity() {
class PastaDetalhesActivity : AppCompatActivity(), TextToSpeech.OnInitListener, ImageInteractionListener {

    private lateinit var binding: ActivityPastaDetalhesBinding
    private val listaDeItens = mutableListOf<ImagemItem>()
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var tts: TextToSpeech // NOVO: Variável para o motor TTS

    // NOVO: MediaPlayer para tocar os áudios customizados
    private var mediaPlayer: MediaPlayer? = null
    // NOVO: Posição do item cujo áudio está sendo editado
    private var posicaoItemEditandoAudio: Int = -1

    // NOVO: ActivityResultLauncher para selecionar UM arquivo de áudio
    private val seletorDeAudioLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument() // MUDANÇA 1: Usar OpenDocument()
    ) { uri: Uri? ->
        uri?.let { audioUri ->
            // Garante que temos permissão persistente para acessar o áudio
            contentResolver.takePersistableUriPermission(
                audioUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            // Atualiza o item na lista com a nova URI do áudio
            if (posicaoItemEditandoAudio != -1) {
                val itemAntigo = listaDeItens[posicaoItemEditandoAudio]
                val itemAtualizado = itemAntigo.copy(audioUriString = audioUri.toString())
                listaDeItens[posicaoItemEditandoAudio] = itemAtualizado
                imageAdapter.notifyItemChanged(posicaoItemEditandoAudio)
                salvarListaDeItens()
                posicaoItemEditandoAudio = -1 // Reseta a posição
            }
        }
    }

    // Contrato para selecionar múltiplas imagens e receber seus URIs
    private val seletorDeImagensLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            // Pede permissão persistente para cada URI selecionado
            uris.forEach { uri ->
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            if (uris.size == 1) {
                // Caso 1: Apenas UMA imagem foi selecionada.
                // Chama o diálogo de nomeação para essa única imagem.
                mostrarDialogoParaNomearImagemUnica(uris.first())
            } else {
                // Caso 2: MÚLTIPLAS imagens foram selecionadas.
                // Usa o fluxo de adição em massa com os nomes de arquivo originais.
                val novosItens = uris.map { uri ->
                    val nomeDoArquivo = getFileNameFromUri(this, uri)
                    ImagemItem(uri.toString(), nomeDoArquivo)
                }

                val startPosition = listaDeItens.size
                listaDeItens.addAll(novosItens)
                imageAdapter.notifyItemRangeInserted(startPosition, novosItens.size)
                salvarListaDeItens()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPastaDetalhesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NOVO: Inicializa o motor TTS
        tts = TextToSpeech(this, this)
        mediaPlayer = MediaPlayer() // NOVO: Inicializa o MediaPlayer

        configurarRecyclerView()

        val nomeDaPasta = intent.getStringExtra("NOME_PASTA") ?: "Pasta Desconhecida"
        title = nomeDaPasta
        binding.tvTituloPasta.text = nomeDaPasta

        // O clique do FAB agora lança o seletor de múltiplas imagens
        binding.fabAdicionarItemNaPasta.setOnClickListener {
            seletorDeImagensLauncher.launch("image/*")
        }

        // Carrega os itens salvos quando a tela é criada
        carregarListaDeItens()
    }

    // NOVO: Função chamada quando a inicialização do TTS termina
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Define o idioma. Use o idioma do seu público-alvo.
            // "pt" para português, "BR" para Brasil.
            val result = tts.setLanguage(Locale("pt", "BR"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Log ou Toast informando que o idioma não é suportado
            }
        } else {
            // Log ou Toast informando que a inicialização falhou
        }
    }

    // NOVO: Função para falar o texto
    private fun speak(text: String) {
        // Verifica se o motor TTS está pronto antes de usar
        if (::tts.isInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    // NOVO: Garante que os recursos do TTS sejam liberados ao fechar a tela
    override fun onDestroy() {
        // Libera os recursos do TTS
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        // NOVO: Libera os recursos do MediaPlayer para evitar vazamento de memória
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    // --- MÉTODOS DA NOVA INTERFACE IMPLEMENTADOS AQUI ---

    override fun onImageClick(item: ImagemItem) {
        // A lógica do clique simples que já existia
        val nomeFormatado = formatarNomeParaFala(item.nome)
        speak(nomeFormatado)
    }


    override fun onImageDoubleClick(item: ImagemItem) {
        // Verifica se o item tem um áudio customizado vinculado
        if (item.audioUriString != null) {
            try {
                val audioUri = Uri.parse(item.audioUriString)
                mediaPlayer?.apply {
                    reset() // Reseta o player para um novo uso
                    setDataSource(this@PastaDetalhesActivity, audioUri)
                    prepare() // Prepara o áudio (pode levar um tempo)
                    start()   // Toca o som
                }
            } catch (e: Exception) {
                // Trata possíveis erros (arquivo não encontrado, formato inválido, etc)
                Toast.makeText(this, "Não foi possível tocar o áudio.", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            // Opcional: Dar um feedback de que não há áudio
            // Toast.makeText(this, "Nenhum áudio customizado.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onImageLongClick(item: ImagemItem, position: Int) {
        val opcoes = arrayOf("Vincular/Editar Áudio", "Editar Nome", "Deletar Imagem")

        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle(item.nome)
            .setItems(opcoes) { _, which ->
                when (which) {
                    0 -> vincularAudio(position)
                    1 -> mostrarDialogoEditarNome(item, position)
                    2 -> mostrarDialogoDeletarImagem(item, position)
                }
            }
            .show()
    }

    // NOVO: Função para iniciar o processo de vinculação de áudio
    private fun vincularAudio(position: Int) {
        this.posicaoItemEditandoAudio = position
        seletorDeAudioLauncher.launch(arrayOf("audio/*"))
    }

    private fun mostrarDialogoParaNomearImagemUnica(uri: Uri) {
        // 1. Nossa bandeira de controle. Começa como 'falso'.
        var acaoDoBotaoFoiRealizada = false

        val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        builder.setTitle("Nomear Nova Imagem")
        builder.setCancelable(true)

        val input = EditText(builder.context)
        val nomeOriginal = getFileNameFromUri(this, uri)
        input.setText(nomeOriginal)
        builder.setView(input)

        // Ação para o botão "Salvar"
        builder.setPositiveButton("Salvar") { _, _ ->
            // 2. Marcamos que uma ação explícita foi feita.
            acaoDoBotaoFoiRealizada = true
            val nomeEditado = input.text.toString().trim().ifEmpty { nomeOriginal }
            adicionarNovoItemNaLista(uri, nomeEditado)
        }

        // Ação para o botão "Cancelar"
        builder.setNegativeButton("Cancelar") { _, _ ->
            // 3. Marcamos que uma ação explícita foi feita (mesmo sendo cancelar).
            acaoDoBotaoFoiRealizada = true
            contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Ação para quando o diálogo é dispensado
        builder.setOnDismissListener {
            // 4. SÓ executamos esta lógica se NENHUM botão foi pressionado.
            // Este é o verdadeiro e único caso de "clique fora".
            if (!acaoDoBotaoFoiRealizada) {
                adicionarNovoItemNaLista(uri, nomeOriginal)
            }
        }

        builder.show()
    }

    // A função de ajuda `adicionarNovoItemNaLista` permanece a mesma.
    private fun adicionarNovoItemNaLista(uri: Uri, nome: String) {
        val novoItem = ImagemItem(uri.toString(), nome)
        listaDeItens.add(novoItem)
        imageAdapter.notifyItemInserted(listaDeItens.size - 1)
        salvarListaDeItens()
    }

    // --- NOVOS DIÁLOGOS PARA AS AÇÕES ---

    private fun mostrarDialogoEditarNome(itemAntigo: ImagemItem, position: Int) {
        val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        builder.setTitle("Editar Nome")

        val input = EditText(builder.context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(itemAntigo.nome)
        builder.setView(input)

        builder.setPositiveButton("Salvar") { _, _ ->
            val novoNome = input.text.toString().trim()
            if (novoNome.isNotEmpty()) {
                // Cria um novo item com o nome atualizado
                val itemAtualizado = itemAntigo.copy(nome = novoNome)
                // Substitui o item antigo na lista
                listaDeItens[position] = itemAtualizado
                // Notifica o adapter que o item na posição X mudou
                imageAdapter.notifyItemChanged(position)
                // Salva a lista inteira
                salvarListaDeItens()
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun mostrarDialogoDeletarImagem(itemParaDeletar: ImagemItem, position: Int) {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem certeza que deseja excluir a imagem '${itemParaDeletar.nome}'?")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("Sim, excluir") { _, _ ->
                // Remove o item da lista de dados
                listaDeItens.removeAt(position)
                // Notifica o adapter que um item foi removido da posição X
                imageAdapter.notifyItemRemoved(position)
                // Salva a lista atualizada
                salvarListaDeItens()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Configura o RecyclerView, definindo seu layout manager e o adapter.
     */
    private fun configurarRecyclerView() {
        // ALTERADO: Passamos 'this' como listener, pois a Activity agora implementa a interface
        imageAdapter = ImageAdapter(listaDeItens, this)
        binding.rvImagens.adapter = imageAdapter
        binding.rvImagens.layoutManager = GridLayoutManager(this, 3)
    }

    /**
     * Usa a biblioteca Gson para converter a lista de ImagemItem em uma string JSON e salvá-la
     * em SharedPreferences, associada ao nome da pasta atual.
     */
    private fun salvarListaDeItens() {
        val nomeDaPasta = intent.getStringExtra("NOME_PASTA") ?: return
        val sharedPreferences = getSharedPreferences("PastaDetalhesPrefs", MODE_PRIVATE)
        val jsonString = Gson().toJson(listaDeItens)
        sharedPreferences.edit().putString("itens_imagens_$nomeDaPasta", jsonString).apply()
    }

    /**
     * Carrega a string JSON de SharedPreferences, a converte de volta para uma lista de
     * ImagemItem e atualiza o RecyclerView.
     */
    private fun carregarListaDeItens() {
        val nomeDaPasta = intent.getStringExtra("NOME_PASTA") ?: return
        val sharedPreferences = getSharedPreferences("PastaDetalhesPrefs", MODE_PRIVATE)
        val jsonString = sharedPreferences.getString("itens_imagens_$nomeDaPasta", null)

        if (jsonString != null) {
            val type = object : TypeToken<MutableList<ImagemItem>>() {}.type
            val itensSalvos: MutableList<ImagemItem> = Gson().fromJson(jsonString, type)
            listaDeItens.clear()
            listaDeItens.addAll(itensSalvos)
            imageAdapter.notifyDataSetChanged()
        }
    }

    /**
     * Função de ajuda para obter o nome de exibição de um arquivo a partir de seu URI.
     * Retorna um nome padrão se não conseguir encontrar o nome do arquivo.
     */
    private fun getFileNameFromUri(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor.use {
                if (it != null && it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                if (cut != null) {
                    result = result.substring(cut + 1)
                }
            }
        }
        return result ?: "Arquivo Desconhecido"
    }

    private fun formatarNomeParaFala(nomeDoArquivo: String): String {
        // Primeiro, remove a extensão do arquivo, se houver uma.
        // "praia_bonita.jpg" -> "praia_bonita"
        val nomeSemExtensao = nomeDoArquivo.substringBeforeLast('.')

        // Em seguida, substitui underscores por espaços para uma fala mais natural.
        // "praia_bonita" -> "praia bonita"
        return nomeSemExtensao.replace('_', ' ')
    }
}