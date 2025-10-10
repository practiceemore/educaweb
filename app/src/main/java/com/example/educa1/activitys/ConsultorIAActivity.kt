package com.example.educa1.activitys

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.educa1.R
import com.example.educa1.adapters.ChatAdapter
import com.example.educa1.databinding.ActivityChatIaBinding
import com.example.educa1.models.MensagemChat
import com.example.educa1.models.CelulaHorario
import com.example.educa1.models.Disciplina
import com.example.educa1.models.Professor
import com.example.educa1.models.Sala
import com.example.educa1.models.Turma
import com.example.educa1.GeminiManager
import com.example.educa1.utils.LocaleHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.educa1.models.RequisitoDisciplina
import java.io.File
import java.io.FileOutputStream
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.view.View
import java.util.*
import com.example.educa1.utils.ConfiguracaoGradeManager
import com.example.educa1.models.ConfiguracaoGrade

class ConsultorIAActivity : BaseActivity() {

    private lateinit var binding: ActivityChatIaBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var geminiManager: GeminiManager
    private val historicoChat = mutableListOf<MensagemChat>()
    private var isMensagemSelecionada = false // Flag para controlar estado

    companion object {
        private const val TAG = "ConsultorIAActivity"
        private const val PREFS_CHAT = "ConsultorChatPrefs"
        private const val KEY_HISTORICO_CHAT = "historico_chat_consultor"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatIaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarChat)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.consultor_ia_titulo)
        binding.toolbarChat.setNavigationIcon(R.drawable.ic_arrow_back_white)

        // Inicializar GeminiManager
        geminiManager = GeminiManager(this)

        configurarRecyclerView()
        configurarBotaoEnviar()
        iniciarChat()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d(TAG, "=== ONCREATEOPTIONSMENU CHAMADO ===")
        val quantidadeSelecionadas = chatAdapter.getMensagensSelecionadas().size
        Log.d(TAG, "Quantidade selecionadas no onCreateOptionsMenu: $quantidadeSelecionadas")
        
        // Carregar menu baseado no estado
        if (quantidadeSelecionadas > 0) {
            Log.d(TAG, "Inflando menu de mensagem selecionada")
            menuInflater.inflate(R.menu.consultor_ia_mensagem_selecionada, menu)
            
            // CORREÇÃO: Aplicar lógica de visibilidade do botão aqui
            val temGradeValida = verificarSeTemGradeValida()
            val botaoAplicarGrade = menu.findItem(R.id.action_aplicar_grade)
            
            if (botaoAplicarGrade != null) {
                val deveMostrarBotao = temGradeValida && quantidadeSelecionadas == 1
                botaoAplicarGrade.isVisible = deveMostrarBotao
                Log.d(TAG, "Botão 'Aplicar à Grade' ${if (deveMostrarBotao) "VISÍVEL" else "OCULTO"} (Grade válida: $temGradeValida, Quantidade: $quantidadeSelecionadas)")
            } else {
                Log.e(TAG, "Botão 'Aplicar à Grade' não encontrado no menu")
            }
        } else {
            Log.d(TAG, "Inflando menu normal")
        menuInflater.inflate(R.menu.consultor_ia_menu, menu)
        }
        return true
    }

    private fun configurarRecyclerView() {
        chatAdapter = ChatAdapter(
            onMensagemLongClick = { mensagem, position ->
                iniciarSelecaoMensagem(position)
            },
            onMensagemClick = { mensagem, position ->
                atualizarToolbarSelecao()
            }
        )
        
        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(this@ConsultorIAActivity)
            adapter = chatAdapter
        }
    }

    private fun iniciarSelecaoMensagem(position: Int) {
        Log.d(TAG, "=== INICIAR SELEÇÃO MENSAGEM ===")
        Log.d(TAG, "Posição clicada: $position")
        
        chatAdapter.selecionarMensagem(position)
        
        val quantidadeAposSelecao = chatAdapter.getMensagensSelecionadas().size
        Log.d(TAG, "Quantidade após seleção: $quantidadeAposSelecao")
        
        atualizarToolbarSelecao()
        
        Log.d(TAG, "=== FIM INICIAR SELEÇÃO ===")
    }

    private fun atualizarToolbarSelecao() {
        Log.d(TAG, "=== ATUALIZANDO TOOLBAR SELEÇÃO ===")
        val quantidadeSelecionadas = chatAdapter.getMensagensSelecionadas().size
        Log.d(TAG, "Quantidade selecionadas: $quantidadeSelecionadas")
        
        if (quantidadeSelecionadas == 0) {
            Log.d(TAG, "Nenhuma mensagem selecionada - limpando seleção")
            limparSelecao()
            return
        }
        
        // Atualizar título da toolbar
        val titulo = if (quantidadeSelecionadas == 1) {
            "$quantidadeSelecionadas ${getString(R.string.mensagem_selecionada)}"
        } else {
            "$quantidadeSelecionadas ${getString(R.string.mensagens_selecionadas)}"
        }
        Log.d(TAG, "Título da toolbar: $titulo")
        binding.toolbarChat.title = titulo
        
        // Mostrar toolbar de seleção
        binding.toolbarChat.visibility = View.VISIBLE
        Log.d(TAG, "Toolbar visibilidade: ${binding.toolbarChat.visibility}")
        
        // Forçar atualização do menu (isso vai chamar onCreateOptionsMenu)
        Log.d(TAG, "Invalidando opções do menu")
        invalidateOptionsMenu()
        
        Log.d(TAG, "=== FIM ATUALIZAÇÃO TOOLBAR ===")
    }
    
    private fun verificarSeTemGradeValida(): Boolean {
        val posicoesSelecionadas = chatAdapter.getMensagensSelecionadas()
        
        for (posicao in posicoesSelecionadas) {
            val mensagem = chatAdapter.getMensagens()[posicao]
            if (mensagem.isIA) {
                // Usar a mesma lógica que detectarTipoMensagemIndividual usa
                val listaTurmas = parsearRespostaIA(mensagem.texto)
                if (listaTurmas != null && listaTurmas.isNotEmpty()) {
                    Log.d(TAG, "✅ Grade válida encontrada na mensagem $posicao")
                    return true // Encontrou pelo menos uma grade válida
                }
            }
        }
        
        Log.d(TAG, "❌ Nenhuma grade válida encontrada entre as mensagens selecionadas")
        return false // Nenhuma grade válida encontrada
    }

    private fun limparSelecao() {
        Log.d(TAG, "=== LIMPAR SELEÇÃO ===")
        chatAdapter.limparSelecoes()
        supportActionBar?.title = getString(R.string.consultor_ia_titulo)
        Log.d(TAG, "Título restaurado para: ${getString(R.string.consultor_ia_titulo)}")
        invalidateOptionsMenu()
        Log.d(TAG, "=== FIM LIMPAR SELEÇÃO ===")
    }

    override fun onBackPressed() {
        if (chatAdapter.getMensagensSelecionadas().isNotEmpty()) {
            limparSelecao()
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_exportar -> {
                exportarMensagensSelecionadas()
                true
            }
            R.id.action_aplicar_grade -> {
                aplicarGradeSelecionada()
                true
            }
            R.id.action_copiar_texto -> {
                copiarTextoMensagensSelecionadas()
                true
            }
            R.id.action_limpar_chat -> {
                limparChat()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun configurarBotaoEnviar() {
        binding.btnEnviar.setOnClickListener {
            val mensagem = binding.etMensagem.text.toString().trim()
            if (mensagem.isNotEmpty()) {
                enviarMensagem(mensagem)
                binding.etMensagem.text.clear()
            }
        }
    }

    private fun iniciarChat() {
        // Carregar histórico salvo primeiro
        carregarHistoricoChat()
        
        // Se não há histórico, adicionar mensagem inicial
        if (historicoChat.isEmpty()) {
            val mensagemInicial = MensagemChat(
                texto = getString(R.string.mensagem_inicial_chat),
                isIA = true,
                tipo = MensagemChat.TipoMensagem.TEXTO
            )
            adicionarMensagem(mensagemInicial)
        } else {
            // Recarregar mensagens no adapter
            historicoChat.forEach { mensagem ->
                chatAdapter.adicionarMensagem(mensagem)
            }
            
            // CORREÇÃO: Rolar para a última mensagem ANTES de exibir a tela
            binding.rvChat.post {
                binding.rvChat.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }
    }

    private fun enviarMensagem(mensagem: String) {
        // === LOG DE TESTE ===
        Log.d("TESTE_SIMPLES", "=== ENVIANDO MENSAGEM ===")
        Log.d("TESTE_SIMPLES", "Mensagem do usuário: $mensagem")
        
        // Adiciona mensagem do usuário
        val mensagemUsuario = MensagemChat(
            texto = mensagem,
            isIA = false,
            tipo = MensagemChat.TipoMensagem.TEXTO
        )
        adicionarMensagem(mensagemUsuario)

        // Mostra indicador de carregamento
        binding.btnEnviar.isEnabled = false

        // Processa a mensagem em background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("TESTE_SIMPLES", "Iniciando processamento em background...")
                val resposta = processarMensagemConsultor(mensagem)
                withContext(Dispatchers.Main) {
                    Log.d("TESTE_SIMPLES", "Resposta recebida, adicionando ao chat...")
                    adicionarRespostaIA(resposta, mensagem) // ← PASSAR MENSAGEM ORIGINAL
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar mensagem: ${e.message}")
                withContext(Dispatchers.Main) {
                    adicionarRespostaIA("Desculpe, ocorreu um erro ao processar sua solicitação. Tente novamente.", mensagem)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.btnEnviar.isEnabled = true
                }
            }
        }
    }

    private suspend fun processarMensagemConsultor(mensagem: String): String {
        // === LOGS DE TESTE ===
        Log.d("TESTE_SIMPLES", "=== PROCESSANDO MENSAGEM ===")
        Log.d("TESTE_SIMPLES", "Mensagem recebida: $mensagem")
        Log.d("TESTE_SIMPLES", "Iniciando preparação dos dados...")
        
        val dadosParaAnalise = prepararDadosParaAnalise()
        val contextoHistorico = prepararContextoHistorico() // ← VERIFICAR SE ESTÁ AQUI
        
        Log.d("TESTE_SIMPLES", "Dados preparados, chamando GeminiManager...")
        Log.d("TESTE_SIMPLES", "Tamanho dos dados: ${dadosParaAnalise.length} caracteres")
        Log.d("TESTE_SIMPLES", "Contexto histórico: ${contextoHistorico.length} caracteres") // ← VERIFICAR SE ESTÁ AQUI
        
        return geminiManager.gerarRespostaConsultor(mensagem, dadosParaAnalise, contextoHistorico)
    }

    private fun prepararDadosParaAnalise(): String {
        Log.d(TAG, "=== PREPARANDO DADOS PARA CONSULTORIA ===")
        
        val professores = buscarProfessoresCompletos()
        val disciplinas = buscarDisciplinasCompletas()
        val salas = buscarSalasCompletas()
        val turmas = buscarTurmasCompletas()
        val grades = carregarTodasAsGrades(turmas)
        val requisitos = carregarTodosOsRequisitos(turmas)
        
        Log.d(TAG, "Dados carregados:")
        Log.d(TAG, "- Professores: ${professores.size}")
        Log.d(TAG, "- Disciplinas: ${disciplinas.size}")
        Log.d(TAG, "- Salas: ${salas.size}")
        Log.d(TAG, "- Turmas: ${turmas.size}")
        Log.d(TAG, "- Grades: ${grades.size}")
        Log.d(TAG, "- Requisitos: ${requisitos.size}")
        
        val dadosParaAnalise = DadosParaAnalise(
            professores = professores,
            disciplinas = disciplinas,
            salas = salas,
            turmas = turmas,
            grades = grades,
            requisitos = requisitos
        )
        
        val json = Gson().toJson(dadosParaAnalise)
        Log.d(TAG, "JSON de análise gerado: ${json.length} caracteres")
        
        return json
    }

    private fun prepararContextoHistorico(): String {
        Log.d(TAG, "=== PREPARANDO CONTEXTO HISTÓRICO ===")
        
        // Pegar as últimas 3 mensagens do histórico
        val ultimasMensagens = historicoChat.takeLast(3)
        
        val contexto = StringBuilder()
        contexto.append("CONTEXTO DA CONVERSA:\n")
        
        ultimasMensagens.forEach { mensagem ->
            val tipo = if (mensagem.isIA) "IA" else "Usuário"
            contexto.append("$tipo: ${mensagem.texto}\n")
        }
        
        val contextoString = contexto.toString()
        Log.d(TAG, "Contexto preparado: ${contextoString.length} caracteres")
        Log.d(TAG, "Contexto: $contextoString")
        
        return contextoString
    }

    private fun buscarProfessoresCompletos(): List<Professor> {
        val prefs = getSharedPreferences(GerenciarProfessoresActivity.PREFS_NAME, MODE_PRIVATE)
        val json = prefs.getString(GerenciarProfessoresActivity.KEY_PROFESSORES, "[]")
        return Gson().fromJson(json, Array<Professor>::class.java).toList()
    }

    private fun buscarDisciplinasCompletas(): List<Disciplina> {
        val prefs = getSharedPreferences(GerenciarDisciplinasActivity.PREFS_NAME, MODE_PRIVATE)
        val json = prefs.getString(GerenciarDisciplinasActivity.KEY_DISCIPLINAS, "[]")
        return Gson().fromJson(json, Array<Disciplina>::class.java).toList()
    }

    private fun buscarSalasCompletas(): List<Sala> {
        val prefs = getSharedPreferences(GerenciarSalasActivity.PREFS_NAME, MODE_PRIVATE)
        val json = prefs.getString(GerenciarSalasActivity.KEY_SALAS, "[]")
        return Gson().fromJson(json, Array<Sala>::class.java).toList()
    }

    private fun buscarTurmasCompletas(): List<Turma> {
        val prefs = getSharedPreferences(GerenciarTurmasActivity.PREFS_NAME, MODE_PRIVATE)
        val json = prefs.getString(GerenciarTurmasActivity.KEY_TURMAS, "[]")
        return Gson().fromJson(json, Array<Turma>::class.java).toList()
    }

    private fun carregarTodasAsGrades(turmas: List<Turma>): Map<String, List<CelulaHorario>> {
        val grades = mutableMapOf<String, List<CelulaHorario>>()
        val prefs = getSharedPreferences("GradesHorariasPrefs", MODE_PRIVATE)
        
        turmas.forEach { turma ->
            val json = prefs.getString("grade_${turma.id}", null)
            if (json != null) {
                val type = object : TypeToken<List<CelulaHorario>>() {}.type
                val grade: List<CelulaHorario> = Gson().fromJson(json, type)
                grades[turma.nome] = grade
            }
        }
        
        return grades
    }

    private fun carregarTodosOsRequisitos(turmas: List<Turma>): Map<String, List<RequisitoDisciplina>> {
        val requisitos = mutableMapOf<String, List<RequisitoDisciplina>>()
        val prefs = getSharedPreferences("RequisitosTurmasPrefs", MODE_PRIVATE)
        
        turmas.forEach { turma ->
            val json = prefs.getString("requisitos_${turma.id}", null)
            if (json != null) {
                try {
                    val type = object : TypeToken<List<RequisitoDisciplina>>() {}.type
                    val requisitosTurma: List<RequisitoDisciplina>? = Gson().fromJson(json, type)
                    if (requisitosTurma != null) {
                        val requisitosFiltrados = requisitosTurma.filter { it.aulasPorSemana > 0 }
                        requisitos[turma.nome] = requisitosFiltrados
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao carregar requisitos da turma ${turma.nome}", e)
                }
            }
        }
        
        return requisitos
    }

    private fun adicionarMensagem(mensagem: MensagemChat) {
        chatAdapter.adicionarMensagem(mensagem)
        historicoChat.add(mensagem)
        
        // Salvar histórico automaticamente
        salvarHistoricoChat()
        
        // Scroll para a última mensagem
        rolarParaUltimaMensagem()
    }

    private suspend fun processarSolicitacaoGrade(mensagemOriginal: String) {
        Log.d(TAG, "=== PROCESSANDO SOLICITAÇÃO DE GRADE ===")
        Log.d(TAG, "Mensagem original do usuário: '$mensagemOriginal'")
        
        try {
            // Preparar dados para a grade
            val dadosParaGrade = prepararDadosParaAnalise()
            
            // Obter idioma atual
            val idiomaAtual = LocaleHelper.getLanguage(this@ConsultorIAActivity)
            Log.d(TAG, "Idioma atual: $idiomaAtual")
            
            // Chamar backend com idioma
            val resposta = geminiManager.gerarRespostaGradeTexto(
                solicitacao = mensagemOriginal,
                dadosGradeJson = dadosParaGrade,
                idioma = idiomaAtual
            )
            
            Log.d(TAG, "Resposta da grade recebida: ${resposta.length} caracteres")
            
            // Exibir resposta no chat
            withContext(Dispatchers.Main) {
        val mensagemIA = MensagemChat(
            texto = resposta,
            isIA = true,
            tipo = MensagemChat.TipoMensagem.TEXTO
        )
        adicionarMensagem(mensagemIA)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar solicitação de grade", e)
            withContext(Dispatchers.Main) {
                val mensagemErro = MensagemChat(
                    texto = getString(R.string.erro_ia_grade),
                    isIA = true,
                    tipo = MensagemChat.TipoMensagem.TEXTO
                )
                adicionarMensagem(mensagemErro)
            }
        }
    }

    private suspend fun processarSolicitacaoRelatorio(mensagemOriginal: String) {
        Log.d(TAG, "=== PROCESSANDO SOLICITAÇÃO DE RELATÓRIO ===")
        Log.d(TAG, "Mensagem original do usuário: '$mensagemOriginal'")
        
        try {
            // Preparar dados para análise
            val dadosParaAnalise = prepararDadosParaAnalise()
            
            // Obter idioma atual
            val idiomaAtual = LocaleHelper.getLanguage(this@ConsultorIAActivity)
            Log.d(TAG, "Idioma atual: $idiomaAtual")
            
            // Chamar backend com idioma
            val resposta = geminiManager.gerarRelatorioComIA(
                solicitacao = mensagemOriginal,
                dadosAnaliseJson = dadosParaAnalise,
                idioma = idiomaAtual
            )
            
            Log.d(TAG, "Resposta do relatório recebida: ${resposta.length} caracteres")
            
            // Exibir resposta no chat
            withContext(Dispatchers.Main) {
                val mensagemIA = MensagemChat(
                    texto = resposta,
                    isIA = true,
                    tipo = MensagemChat.TipoMensagem.TEXTO
                )
                adicionarMensagem(mensagemIA)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar solicitação de relatório", e)
            withContext(Dispatchers.Main) {
                val mensagemErro = MensagemChat(
                    texto = getString(R.string.erro_ia_relatorio),
                    isIA = true,
                    tipo = MensagemChat.TipoMensagem.TEXTO
                )
                adicionarMensagem(mensagemErro)
            }
        }
    }

    private fun adicionarRespostaIA(resposta: String, mensagemOriginal: String) {
        Log.d(TAG, "=== RESPOSTA DA IA RECEBIDA ===")
        Log.d(TAG, "Resposta: '$resposta'")
        Log.d(TAG, "Mensagem original: '$mensagemOriginal'")
        Log.d(TAG, "Tamanho: ${resposta.length} caracteres")
        
        // Verificar se é comando especial da IA (suporte multilíngue)
        when (resposta.trim().lowercase()) {
            "gerar grade", "generar horario" -> {
                Log.d(TAG, "✅ Comando detectado: gerar grade")
                CoroutineScope(Dispatchers.IO).launch {
                    processarSolicitacaoGrade(mensagemOriginal)
                }
            }
            "gerar relatorio", "generar reporte" -> {
                Log.d(TAG, "✅ Comando detectado: gerar relatorio")
                CoroutineScope(Dispatchers.IO).launch {
                    processarSolicitacaoRelatorio(mensagemOriginal)
                }
            }
            else -> {
                Log.d(TAG, "📝 Resposta normal - exibindo no chat")
                // Exibir resposta normal no chat
                val mensagemIA = MensagemChat(
                    texto = resposta,
                    isIA = true,
                    tipo = MensagemChat.TipoMensagem.TEXTO
                )
                adicionarMensagem(mensagemIA)
            }
        }
    }

    private fun rolarParaUltimaMensagem() {
        binding.rvChat.post {
            binding.rvChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    private fun limparChat() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.limpar_chat_titulo))
            .setMessage(getString(R.string.limpar_chat_mensagem))
            .setPositiveButton(getString(R.string.sim)) { _, _ ->
                limparHistoricoChat()
            }
            .setNegativeButton(getString(R.string.nao), null)
            .show()
    }

    // Funções para persistência do histórico do chat
    private fun salvarHistoricoChat() {
        try {
            val prefs = getSharedPreferences(PREFS_CHAT, MODE_PRIVATE).edit()
            
            // Converter histórico para JSON
            val type = object : TypeToken<List<MensagemChat>>() {}.type
            val json = Gson().toJson(historicoChat, type)
            
            prefs.putString(KEY_HISTORICO_CHAT, json)
            prefs.apply()
            
            Log.d(TAG, "✅ Histórico do chat salvo: ${historicoChat.size} mensagens")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao salvar histórico do chat", e)
        }
    }
    
    private fun carregarHistoricoChat() {
        try {
            val prefs = getSharedPreferences(PREFS_CHAT, MODE_PRIVATE)
            
            val json = prefs.getString(KEY_HISTORICO_CHAT, null)
            if (json != null) {
                val type = object : TypeToken<List<MensagemChat>>() {}.type
                val historicoCarregado = Gson().fromJson<List<MensagemChat>>(json, type)
                
                historicoChat.clear()
                historicoChat.addAll(historicoCarregado)
                
                Log.d(TAG, "✅ Histórico do chat carregado: ${historicoChat.size} mensagens")
            } else {
                Log.d(TAG, "ℹ️ Nenhum histórico encontrado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao carregar histórico do chat", e)
            historicoChat.clear()
        }
    }
    
    private fun limparHistoricoChat() {
        try {
            val prefs = getSharedPreferences(PREFS_CHAT, MODE_PRIVATE).edit()
            
            prefs.remove(KEY_HISTORICO_CHAT)
            prefs.apply()
            
            historicoChat.clear()
            chatAdapter.limparMensagens()
            
            // CORREÇÃO: Chamar iniciarChat() novamente para criar a mensagem inicial no idioma correto
            iniciarChat()
            
            Log.d(TAG, "🗑️ Histórico do chat limpo")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao limpar histórico do chat", e)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // Data class para análise
    data class DadosParaAnalise(
        val professores: List<Professor>,
        val disciplinas: List<Disciplina>,
        val salas: List<Sala>,
        val turmas: List<Turma>,
        val grades: Map<String, List<CelulaHorario>>,
        val requisitos: Map<String, List<RequisitoDisciplina>>
    )

//    private fun mostrarOpcoesMensagemSelecionada(mensagem: MensagemChat) {
//        val opcoes = arrayOf("Exportar PDF", "Copiar Texto", "Cancelar")
//
//        AlertDialog.Builder(this)
//            .setTitle("Mensagem Selecionada")
//            .setMessage("O que deseja fazer com esta mensagem?")
//            .setItems(opcoes) { _, which ->
//                when (which) {
//                    0 -> exportarMensagemSelecionadaPDF(mensagem)
//                    1 -> copiarTextoMensagem(mensagem)
//                    2 -> chatAdapter.limparSelecao()
//                }
//            }
//            .setOnDismissListener {
//                chatAdapter.limparSelecao()
//            }
//            .show()
//    }

    private fun exportarMensagemSelecionadaPDF(mensagem: MensagemChat) {
        Log.d(TAG, "=== EXPORTANDO MENSAGEM SELECIONADA ===")
        Log.d(TAG, "Mensagem selecionada: ${mensagem.texto.take(100)}...")

        // Parse local da resposta da IA - agora retorna lista de turmas
        val listaTurmas = parsearRespostaIA(mensagem.texto)

        if (listaTurmas == null || listaTurmas.isEmpty()) {
            Log.e(TAG, "Não foi possível extrair dados das turmas da mensagem selecionada")
            Toast.makeText(this, getString(R.string.mensagem_sem_grade_exportar), Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Dados extraídos com sucesso: ${listaTurmas.size} turmas")

        // Gera o PDF com todas as turmas
        gerarGradePDFMultiplasTurmas(listaTurmas)
    }

    private fun copiarTextoMensagem(mensagem: MensagemChat) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Mensagem", mensagem.texto)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.texto_copiado), Toast.LENGTH_SHORT).show()
    }

    private fun exportarGradePDF() {
        Log.d(TAG, "=== EXPORTAR PDF SOLICITADO VIA MENU ===")
        
        val posicoesSelecionadas = chatAdapter.getMensagensSelecionadas()
        
        if (posicoesSelecionadas.isNotEmpty()) {
            Log.d(TAG, "Exportando mensagens selecionadas via menu")
            val mensagensSelecionadas = posicoesSelecionadas.map { posicao ->
                chatAdapter.getMensagens()[posicao]
            }
            
            // Gerar um único PDF com todas as mensagens selecionadas
            gerarPDFMultiplasMensagens(mensagensSelecionadas)
        } else {
            Log.d(TAG, "Nenhuma mensagem selecionada, buscando última resposta da IA")
            
            // Fallback: buscar última resposta da IA
            val ultimaRespostaIA = chatAdapter.getMensagens().findLast { it.isIA }

            if (ultimaRespostaIA == null) {
                Log.e(TAG, "Nenhuma resposta da IA encontrada para exportar")
                Toast.makeText(this, getString(R.string.nenhuma_grade_exportar), Toast.LENGTH_SHORT).show()
                return
            }

            Log.d(TAG, "Última resposta da IA encontrada: ${ultimaRespostaIA.texto.take(100)}...")
            exportarMensagemSelecionadaPDF(ultimaRespostaIA)
        }
    }

    private fun parsearRespostaIA(texto: String): List<DadosTabelaGrade>? {
        Log.d(TAG, "=== PARSING LOCAL DA RESPOSTA DA IA ===")
        Log.d(TAG, "Texto recebido: ${texto.take(200)}...")

        try {
            val turmasEncontradas = mutableListOf<DadosTabelaGrade>()

            // Padrões multilíngues para encontrar turmas
            val padroesTurma = listOf(
                Regex("""\*\*Turma\s+(\d+):\*\*""", RegexOption.IGNORE_CASE), // Português
                Regex("""\*\*Grupo\s+(\d+):\*\*""", RegexOption.IGNORE_CASE), // Espanhol
                Regex("""\*\*Clase\s+(\d+):\*\*""", RegexOption.IGNORE_CASE)  // Espanhol alternativo
            )

            var matchesTurma = emptySequence<MatchResult>()
            
            // Tentar cada padrão até encontrar matches
            for (padrao in padroesTurma) {
                matchesTurma = padrao.findAll(texto)
                if (matchesTurma.count() > 0) {
                    Log.d(TAG, "Padrão encontrado: ${padrao.pattern}")
                    break
                }
            }

            Log.d(TAG, "Turmas encontradas: ${matchesTurma.count()}")

            matchesTurma.forEach { match ->
                val numeroTurma = match.groupValues[1]
                Log.d(TAG, "Processando Turma $numeroTurma")

                // Extrair o bloco de texto para esta turma específica
                val turmaTexto = extrairBlocoTurma(texto, numeroTurma)

                if (turmaTexto != null) {
                    val dadosTabela = processarTurma(turmaTexto, numeroTurma)
                    if (dadosTabela != null) {
                        turmasEncontradas.add(dadosTabela)
                        Log.d(TAG, "Turma $numeroTurma processada com sucesso")
                    }
                }
            }

            if (turmasEncontradas.isEmpty()) {
                Log.e(TAG, "Nenhuma turma encontrada no texto")
                return null
            }

            Log.d(TAG, "Total de turmas processadas: ${turmasEncontradas.size}")
            return turmasEncontradas

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fazer parsing da resposta da IA", e)
            return null
        }
    }

    private fun extrairBlocoTurma(texto: String, numeroTurma: String): String? {
        // Padrões multilíngues para encontrar o bloco completo da turma
        val padroes = listOf(
            Regex("""\*\*Turma\s+$numeroTurma:\*\*(.*?)(?=\*\*Turma\s+\d+:\*\*|\*\*Observações:\*\*|$)""", RegexOption.DOT_MATCHES_ALL), // Português
            Regex("""\*\*Grupo\s+$numeroTurma:\*\*(.*?)(?=\*\*Grupo\s+\d+:\*\*|\*\*Observaciones:\*\*|$)""", RegexOption.DOT_MATCHES_ALL), // Espanhol
            Regex("""\*\*Clase\s+$numeroTurma:\*\*(.*?)(?=\*\*Clase\s+\d+:\*\*|\*\*Observaciones:\*\*|$)""", RegexOption.DOT_MATCHES_ALL)  // Espanhol alternativo
        )
        
        for (padrao in padroes) {
            val match = padrao.find(texto)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        
        return null
    }

    private fun processarTurma(turmaTexto: String, numeroTurma: String): DadosTabelaGrade? {
        try {
            val gradeType = "Grade de Turma"
            val target = numeroTurma

            // Detectar idioma baseado no conteúdo
            val isEspanhol = turmaTexto.contains("Lunes") || turmaTexto.contains("Martes") || 
                           turmaTexto.contains("Miércoles") || turmaTexto.contains("Jueves") || 
                           turmaTexto.contains("Viernes")

            // CORRIGIDO: Usar configuração global para períodos
            val configuracaoManager = ConfiguracaoGradeManager(this)
            val configuracaoGrade = configuracaoManager.carregarConfiguracao()
            val aulasPorDia = configuracaoGrade.aulasPorDia
            
            // Definir períodos e dias baseado no idioma E configuração global
            val (periodos, diasDaSemana) = if (isEspanhol) {
                // Gerar períodos dinamicamente baseado na configuração
                (1..aulasPorDia).map { "${it}ª Clase" } to 
                listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes")
            } else {
                // Gerar períodos dinamicamente baseado na configuração
                (1..aulasPorDia).map { "${it}ª Aula" } to 
                listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta")
            }

            Log.d(TAG, "Idioma detectado: ${if (isEspanhol) "Espanhol" else "Português"}")
            Log.d(TAG, "Configuração: $aulasPorDia aulas por dia")
            Log.d(TAG, "Períodos gerados: $periodos")

            // Extrair aulas de cada dia
            val aulas = mutableListOf<List<AulaDados>>()

            diasDaSemana.forEach { dia ->
                val aulasDoDia = extrairAulasDoDia(turmaTexto, dia, isEspanhol)
                aulas.add(aulasDoDia)
                Log.d(TAG, "Aulas extraídas para $dia na Turma $numeroTurma: ${aulasDoDia.size}")
            }

            // Extrair observações
            val observacoes = if (isEspanhol) {
                "Horario generado automáticamente por el sistema Educa1 - Grupo $numeroTurma"
            } else {
                "Grade gerada automaticamente pelo sistema Educa1 - Turma $numeroTurma"
            }

            val dadosTabela = DadosTabelaGrade(
                gradeType = gradeType,
                target = target,
                periodos = periodos,
                diasDaSemana = diasDaSemana,
                aulas = aulas,
                observacoes = observacoes
            )

            Log.d(TAG, "Dados da Turma $numeroTurma extraídos com sucesso")
            return dadosTabela

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar Turma $numeroTurma", e)
            return null
        }
    }

    private fun extrairAulasDoDia(texto: String, dia: String, isEspanhol: Boolean): List<AulaDados> {
        val aulas = mutableListOf<AulaDados>()
        
        // Padrões multilíngues para encontrar o bloco do dia
        val padroesDia = if (isEspanhol) {
            listOf(
                Regex("""\*\*${dia}:\*\*.*?(?=\*\*|$)""", RegexOption.DOT_MATCHES_ALL),
                Regex("""\*\*${dia}\*\*.*?(?=\*\*|$)""", RegexOption.DOT_MATCHES_ALL)
            )
        } else {
            listOf(
                Regex("""\*\*${dia}-feira:\*\*.*?(?=\*\*|$)""", RegexOption.DOT_MATCHES_ALL),
                Regex("""\*\*${dia}:\*\*.*?(?=\*\*|$)""", RegexOption.DOT_MATCHES_ALL)
            )
        }
        
        var matchDia: MatchResult? = null
        for (padrao in padroesDia) {
            matchDia = padrao.find(texto)
            if (matchDia != null) break
        }
        
        if (matchDia != null) {
            val conteudoDia = matchDia.value
            Log.d(TAG, "Conteúdo encontrado para $dia: ${conteudoDia.take(100)}...")
            
            // Padrões multilíngues para extrair aulas
            val padroesAula = if (isEspanhol) {
                listOf(
                    Regex("""(\d+)ª\s+Clase:\s*([^-]+)\s*-\s*([^\n]+)"""), // "1ª Clase: Disciplina - Professor"
                    Regex("""(\d+)ª\s+Aula:\s*([^-]+)\s*-\s*([^\n]+)""")  // Fallback para "1ª Aula"
                )
            } else {
                listOf(
                    Regex("""(\d+)ª\s+Aula:\s*([^-]+)\s*-\s*([^\n]+)""")  // "1ª Aula: Disciplina - Professor"
                )
            }
            
            var matchesAula = emptySequence<MatchResult>()
            for (padrao in padroesAula) {
                matchesAula = padrao.findAll(conteudoDia)
                if (matchesAula.count() > 0) {
                    Log.d(TAG, "Padrão de aula encontrado: ${padrao.pattern}")
                    break
                }
            }
            
            matchesAula.forEach { match ->
                val numeroAula = match.groupValues[1].toInt()
                val disciplina = match.groupValues[2].trim()
                val professor = match.groupValues[3].trim()
                
                // Ajustar índice para começar em 0
                val indiceAula = numeroAula - 1
                
                // Garantir que a lista tenha tamanho suficiente
                while (aulas.size <= indiceAula) {
                    aulas.add(AulaDados("", ""))
                }
                
                aulas[indiceAula] = AulaDados(disciplina, professor)
                Log.d(TAG, "Aula $numeroAula: $disciplina - $professor")
            }
        } else {
            Log.d(TAG, "Nenhum conteúdo encontrado para $dia")
        }
        
        // CORRIGIDO: Usar configuração global em vez de valor fixo
        val configuracaoManager = ConfiguracaoGradeManager(this)
        val configuracaoGrade = configuracaoManager.carregarConfiguracao()
        val aulasPorDia = configuracaoGrade.aulasPorDia
        
        Log.d(TAG, "Configuração carregada: $aulasPorDia aulas por dia")
        
        // Garantir que sempre retorne o número correto de aulas (mesmo que vazias)
        while (aulas.size < aulasPorDia) {
            aulas.add(AulaDados("", ""))
        }
        
        return aulas.take(aulasPorDia)
    }

    private fun gerarGradePDFMultiplasTurmas(listaTurmas: List<DadosTabelaGrade>) {
        Log.d(TAG, "=== GERANDO PDF COM MÚLTIPLAS TURMAS ===")

        // Modo paisagem: largura 842, altura 595
        val document = PdfDocument()
        var pageNumber = 1

        listaTurmas.forEachIndexed { index, dadosTabela ->
            Log.d(TAG, "Gerando página ${index + 1} para Turma ${dadosTabela.target}")

            // Criar nova página para cada turma
            val currentPage = document.startPage(PdfDocument.PageInfo.Builder(842, 595, pageNumber).create())
            val canvas = currentPage.canvas
            val paint = Paint()

            // Configurações de texto
            paint.isAntiAlias = true
            paint.textSize = 10f
            paint.color = android.graphics.Color.BLACK

            var y = 30f
            val margin = 30f
            val lineHeight = 15f

            // Título
            paint.textSize = 16f
            paint.isFakeBoldText = true
            canvas.drawText("Grade Horária - Relatório", margin, y, paint)
            y += lineHeight * 2

            // Nome da turma
            paint.textSize = 14f
            paint.isFakeBoldText = true
            canvas.drawText("Turma ${dadosTabela.target}", margin, y, paint)
            y += lineHeight * 2

            // Desenhar tabela da turma
            desenharTabelaGrade(canvas, dadosTabela, margin, y, paint)

            // Finalizar página
            document.finishPage(currentPage)
            pageNumber++

            Log.d(TAG, "Página ${pageNumber - 1} finalizada para Turma ${dadosTabela.target}")
        }

        // Salvar arquivo
        val arquivo = File(getExternalFilesDir(null), "grade_horaria_multiplas_turmas_${System.currentTimeMillis()}.pdf")
        Log.d(TAG, "Salvando PDF em: ${arquivo.absolutePath}")

        val outputStream = FileOutputStream(arquivo)
        document.writeTo(outputStream)
        document.close()
        outputStream.close()

        Log.d(TAG, "PDF gerado com sucesso: ${arquivo.length()} bytes, $pageNumber páginas")

        // Compartilhar arquivo
        compartilharArquivo(arquivo)

        // Mostrar mensagem de sucesso
        Toast.makeText(this, getString(R.string.pdf_gerado_sucesso_turmas, listaTurmas.size, pageNumber - 1), Toast.LENGTH_LONG).show()
    }

    private fun desenharTabelaGrade(canvas: Canvas, dados: DadosTabelaGrade, margin: Float, startY: Float, paint: Paint) {
        Log.d(TAG, "Desenhando tabela da grade")
        
        val tableMargin = margin
        val tableWidth = 780f // Largura total da tabela
        val cellWidth = tableWidth / (dados.diasDaSemana.size + 1) // +1 para coluna de horários
        
        // CORRIGIDO: Ajustar altura da célula baseado no número de períodos
        val numPeriodos = dados.periodos.size
        val alturaDisponivel = 500f // Altura disponível na página
        val cellHeight = if (numPeriodos > 6) {
            // Para mais de 6 períodos, reduzir altura da célula
            (alturaDisponivel / (numPeriodos + 1)).coerceAtLeast(25f)
        } else {
            40f // Altura normal para até 6 períodos
        }
        
        val tableHeight = (numPeriodos + 1) * cellHeight // +1 para o cabeçalho
        
        Log.d(TAG, "Tabela com $numPeriodos períodos, altura calculada: $tableHeight, altura da célula: $cellHeight")
        
        // Desenhar bordas da tabela
        paint.strokeWidth = 2f
        paint.color = android.graphics.Color.BLACK
        paint.style = Paint.Style.STROKE
        
        // Borda externa
        canvas.drawRect(tableMargin, startY, tableMargin + tableWidth, startY + tableHeight, paint)
        
        // Linhas horizontais (apenas para o número real de períodos)
        for (i in 0..numPeriodos) {
            val y = startY + (i * cellHeight)
            canvas.drawLine(tableMargin, y, tableMargin + tableWidth, y, paint)
        }
        
        // Linhas verticais
        for (i in 0..dados.diasDaSemana.size) {
            val x = tableMargin + (i * cellWidth)
            canvas.drawLine(x, startY, x, startY + tableHeight, paint)
        }
        
        // Configurar paint para texto
        paint.style = Paint.Style.FILL
        paint.textSize = if (cellHeight < 30f) 8f else 9f // Ajustar tamanho da fonte
        paint.isFakeBoldText = false
        
        // Cabeçalho - Horário
        paint.isFakeBoldText = true
        val headerY = startY + (cellHeight / 2) + 3
        canvas.drawText("Horário", tableMargin + (cellWidth / 2) - 20, headerY, paint)
        
        // Cabeçalhos dos dias
        for (i in dados.diasDaSemana.indices) {
            val x = tableMargin + ((i + 1) * cellWidth) + (cellWidth / 2) - 25
            canvas.drawText(dados.diasDaSemana[i], x, headerY, paint)
        }
        
        paint.isFakeBoldText = false
        
        // Preencher células com dados (apenas para o número real de períodos)
        for (periodoIndex in dados.periodos.indices) {
            val periodo = dados.periodos[periodoIndex]
            val rowY = startY + ((periodoIndex + 1) * cellHeight)
            
            // Nome do período (primeira coluna)
            val periodoX = tableMargin + (cellWidth / 2) - 20
            val periodoY = rowY + (cellHeight / 2) + 3
            canvas.drawText(periodo, periodoX, periodoY, paint)
            
            // Dados de cada dia
            for (diaIndex in dados.diasDaSemana.indices) {
                if (diaIndex < dados.aulas.size && periodoIndex < dados.aulas[diaIndex].size) {
                    val aula = dados.aulas[diaIndex][periodoIndex]
                    val cellX = tableMargin + ((diaIndex + 1) * cellWidth) + 5
                    val cellY = rowY + 10
                    
                    // Disciplina (primeira linha)
                    canvas.drawText(aula.disciplina, cellX, cellY, paint)
                    
                    // Professor (segunda linha) - só se houver espaço
                    if (cellHeight > 25f) {
                        canvas.drawText(aula.professor, cellX, cellY + 12, paint)
                    }
                }
            }
        }
        
        Log.d(TAG, "Tabela desenhada com sucesso - altura otimizada para $numPeriodos períodos")
    }

    private fun compartilharArquivo(arquivo: File) {
        Log.d(TAG, "=== COMPARTILHANDO ARQUIVO ===")
        
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                arquivo
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Grade Horária")
                putExtra(Intent.EXTRA_TEXT, "Grade horária gerada pelo sistema Educa1")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(intent, "Compartilhar Grade Horária"))
            Log.d(TAG, "Arquivo compartilhado com sucesso")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao compartilhar arquivo", e)
            Toast.makeText(this, getString(R.string.erro_compartilhar, e.message ?: ""), Toast.LENGTH_LONG).show()
        }
    }

    // Data class para os dados da tabela
    data class DadosTabelaGrade(
        val gradeType: String,
        val target: String,
        val periodos: List<String>,
        val diasDaSemana: List<String>,
        val aulas: List<List<AulaDados>>,
        val observacoes: String
    )
    
    data class AulaDados(
        val disciplina: String,
        val professor: String
    )

    private fun exportarMensagensSelecionadas() {
        Log.d(TAG, "=== INICIANDO EXPORTAÇÃO DE MENSAGENS ===")
        
        val posicoesSelecionadas = chatAdapter.getMensagensSelecionadas()
        Log.d(TAG, "Posições selecionadas: $posicoesSelecionadas")
        
        if (posicoesSelecionadas.isEmpty()) {
            Log.w(TAG, "Nenhuma mensagem selecionada")
            Toast.makeText(this, getString(R.string.nenhuma_mensagem_selecionada), Toast.LENGTH_SHORT).show()
            return
        }

        val mensagensSelecionadas = posicoesSelecionadas.map { posicao ->
            chatAdapter.getMensagens()[posicao]
        }

        Log.d(TAG, "=== EXPORTANDO ${mensagensSelecionadas.size} MENSAGENS ===")
        
        // Log detalhado de cada mensagem
        mensagensSelecionadas.forEachIndexed { index, mensagem ->
            Log.d(TAG, "Mensagem ${index + 1}:")
            Log.d(TAG, "  - É IA: ${mensagem.isIA}")
            Log.d(TAG, "  - Tipo: ${mensagem.tipo}")
            Log.d(TAG, "  - Texto (primeiros 200 chars): ${mensagem.texto.take(200)}...")
            Log.d(TAG, "  - Tamanho total: ${mensagem.texto.length} caracteres")
        }

        // Analisar cada mensagem individualmente
        Log.d(TAG, "=== ANALISANDO TIPOS DE MENSAGENS ===")
        val mensagensGrade = mutableListOf<MensagemChat>()
        val mensagensRelatorio = mutableListOf<MensagemChat>()
        
        mensagensSelecionadas.forEach { mensagem ->
            val tipo = detectarTipoMensagemIndividual(mensagem)
            when (tipo) {
                TipoExportacao.GRADE -> {
                    mensagensGrade.add(mensagem)
                    Log.d(TAG, "Mensagem classificada como GRADE")
                }
                TipoExportacao.RELATORIO -> {
                    mensagensRelatorio.add(mensagem)
                    Log.d(TAG, "Mensagem classificada como RELATÓRIO")
                }
            }
        }
        
        Log.d(TAG, "Resultado da análise:")
        Log.d(TAG, "  - Mensagens de GRADE: ${mensagensGrade.size}")
        Log.d(TAG, "  - Mensagens de RELATÓRIO: ${mensagensRelatorio.size}")
        
        // Decidir tipo de exportação
        when {
            mensagensGrade.isNotEmpty() && mensagensRelatorio.isNotEmpty() -> {
                Log.d(TAG, "✅ MISTO: Gerando PDF híbrido (relatório + grade)")
                gerarPDFHibrido(mensagensRelatorio, mensagensGrade)
            }
            mensagensGrade.isNotEmpty() -> {
                Log.d(TAG, "✅ GRADE: Exportando apenas grades")
                gerarPDFMultiplasMensagens(mensagensGrade)
            }
            mensagensRelatorio.isNotEmpty() -> {
                Log.d(TAG, "✅ RELATÓRIO: Exportando apenas relatórios")
                gerarRelatorioMultiplasMensagens(mensagensRelatorio)
            }
            else -> {
                Log.e(TAG, "❌ Nenhum tipo válido detectado")
                Toast.makeText(this, getString(R.string.nenhum_conteudo_valido), Toast.LENGTH_SHORT).show()
            }
        }
        
        // Limpar seleção após exportar
        Log.d(TAG, "=== LIMPANDO SELEÇÃO ===")
        limparSelecao()
    }

    // Enum para tipos de exportação
    enum class TipoExportacao {
        GRADE,
        RELATORIO
    }

    private fun detectarTipoMensagemIndividual(mensagem: MensagemChat): TipoExportacao {
        if (!mensagem.isIA) {
            return TipoExportacao.RELATORIO // Mensagens do usuário são sempre relatório
        }
        
        // Usar a mesma função que gerarPDFMultiplasMensagens usa
        val listaTurmas = parsearRespostaIA(mensagem.texto)
        
        if (listaTurmas != null && listaTurmas.isNotEmpty()) {
            return TipoExportacao.GRADE
        }
        
        return TipoExportacao.RELATORIO
    }

    private fun gerarPDFHibrido(mensagensRelatorio: List<MensagemChat>, mensagensGrade: List<MensagemChat>) {
        Log.d(TAG, "=== GERANDO PDF HÍBRIDO ===")
        Log.d(TAG, "Relatórios: ${mensagensRelatorio.size}, Grades: ${mensagensGrade.size}")
        
        try {
            val pdfDocument = PdfDocument()
            var pageNumber = 1
            
            // PÁGINA 1: RELATÓRIO
            if (mensagensRelatorio.isNotEmpty()) {
                Log.d(TAG, "Gerando página de relatório...")
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create() // A4 retrato
                val currentPage = pdfDocument.startPage(pageInfo)
                val canvas = currentPage.canvas
                
                // Combinar todos os relatórios em um texto
                val textoCombinado = mensagensRelatorio.joinToString("\n\n") { mensagem ->
                    val tipo = if (mensagem.isIA) "IA" else "Usuário"
                    "$tipo: ${mensagem.texto}"
                }
                
                // Gerar página de relatório
                desenharPaginaRelatorio(canvas, textoCombinado)
                pdfDocument.finishPage(currentPage)
                pageNumber++
            }
            
            // PÁGINAS 2+: GRADES
            if (mensagensGrade.isNotEmpty()) {
                Log.d(TAG, "Gerando páginas de grade...")
                
                mensagensGrade.forEach { mensagem ->
                    val listaTurmas = parsearRespostaIA(mensagem.texto)
                    
                    if (listaTurmas != null && listaTurmas.isNotEmpty()) {
                        listaTurmas.forEach { dadosTabela ->
                            Log.d(TAG, "Gerando página ${pageNumber} para Turma ${dadosTabela.target}")
                            
                            val currentPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(842, 595, pageNumber).create())
                            val canvas = currentPage.canvas
                            val paint = Paint()
                            
                            // Configurações de texto
                            paint.isAntiAlias = true
                            paint.textSize = 10f
                            paint.color = android.graphics.Color.BLACK
                            
                            var y = 30f
                            val margin = 30f
                            val lineHeight = 15f
                            
                            // Título
                            paint.textSize = 16f
                            paint.isFakeBoldText = true
                            canvas.drawText("Grade Horária - Relatório", margin, y, paint)
                            y += lineHeight * 2
                            
                            // Nome da turma
                            paint.textSize = 14f
                            paint.isFakeBoldText = true
                            canvas.drawText("Turma ${dadosTabela.target}", margin, y, paint)
                            y += lineHeight * 2
                            
                            // Desenhar tabela da turma
                            desenharTabelaGrade(canvas, dadosTabela, margin, y, paint)
                            
                            pdfDocument.finishPage(currentPage)
                            pageNumber++
                        }
                    }
                }
            }
            
            // Salvar arquivo
            val fileName = "relatorio_hibrido_${System.currentTimeMillis()}.pdf"
            val file = File(getExternalFilesDir(null), fileName)
            pdfDocument.writeTo(file.outputStream())
            pdfDocument.close()
            
            Log.d(TAG, "PDF híbrido gerado com sucesso: ${file.absolutePath}")
            
            // Compartilhar
            compartilharArquivoHibrido(file)
            
            Toast.makeText(this, getString(R.string.pdf_hibrido_gerado_sucesso, pageNumber - 1), Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao gerar PDF híbrido", e)
            Toast.makeText(this, getString(R.string.erro_gerar_pdf_hibrido, e.message ?: ""), Toast.LENGTH_LONG).show()
        }
    }

    private fun desenharPaginaRelatorio(canvas: Canvas, texto: String) {
        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
        }

        val paintTitulo = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 16f
            isFakeBoldText = true
        }

        var y = 50f
        val margin = 40f
        val lineHeight = 14f
        val maxY = 800f

        // Título
        val dataAtual = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
//        canvas.drawText("Relatório Consultor IA - $dataAtual", margin, y, paintTitulo)
//        y += 40f

        // Quebrar texto em linhas
        val linhas = quebrarTextoFormatado(texto, paint, 595 - 2 * margin)

        // Desenhar linhas do texto
        linhas.forEach { linha ->
            if (y + lineHeight > maxY) {
                // Se não couber, parar (em um PDF híbrido, o relatório fica na primeira página)
                return
            }

            if (linha.startsWith("TITULO:")) {
                val titulo = linha.substring(7)
                canvas.drawText(titulo, margin, y, paintTitulo)
                y += lineHeight + 5f
            } else if (linha.startsWith("ULTIMA_LINHA:")) {
                val texto = linha.substring(13)
                canvas.drawText(texto, margin, y, paint)
                y += lineHeight
            } else if (linha.isNotEmpty()) {
                canvas.drawText(linha, margin, y, paint)
                y += lineHeight
            } else {
                y += lineHeight / 2
            }
        }
    }

    private fun compartilharArquivoHibrido(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Relatório Híbrido - Consultor IA")
                putExtra(Intent.EXTRA_TEXT, "Relatório híbrido gerado pelo sistema Educa1")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(intent, "Compartilhar Relatório Híbrido"))
            Log.d(TAG, "Arquivo híbrido compartilhado com sucesso")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao compartilhar arquivo híbrido", e)
            Toast.makeText(this, getString(R.string.erro_compartilhar, e.message ?: ""), Toast.LENGTH_LONG).show()
        }
    }

    private fun copiarTextoMensagensSelecionadas() {
        val posicoesSelecionadas = chatAdapter.getMensagensSelecionadas()
        if (posicoesSelecionadas.isEmpty()) {
            Toast.makeText(this, getString(R.string.nenhuma_mensagem_selecionada), Toast.LENGTH_SHORT).show()
            return
        }

        val mensagensSelecionadas = posicoesSelecionadas.map { posicao ->
            chatAdapter.getMensagens()[posicao]
        }

        val textoCombinado = mensagensSelecionadas.joinToString("\n\n") { mensagem ->
            val tipo = if (mensagem.isIA) "IA" else "Usuário"
            "$tipo: ${mensagem.texto}"
        }

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Mensagens selecionadas", textoCombinado)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(this, getString(R.string.mensagens_copiadas), Toast.LENGTH_SHORT).show()
        
        // Limpar seleção após copiar
        limparSelecao()
    }

    private fun gerarPDFMultiplasMensagens(mensagens: List<MensagemChat>) {
        Log.d(TAG, "=== GERANDO PDF COM MÚLTIPLAS MENSAGENS (GRADES) ===")
        Log.d(TAG, "Total de mensagens: ${mensagens.size}")

        // Modo paisagem: largura 842, altura 595
        val document = PdfDocument()
        var pageNumber = 1

        mensagens.forEachIndexed { index, mensagem ->
            Log.d(TAG, "Processando mensagem ${index + 1}/${mensagens.size}")

            // Parse da resposta da IA para extrair dados das turmas
            val listaTurmas = parsearRespostaIA(mensagem.texto)

            if (listaTurmas != null && listaTurmas.isNotEmpty()) {
                Log.d(TAG, "Mensagem ${index + 1} contém ${listaTurmas.size} turmas")

                // Criar uma página para cada turma desta mensagem
                listaTurmas.forEach { dadosTabela ->
                    Log.d(TAG, "Gerando página ${pageNumber} para Turma ${dadosTabela.target}")

                    // Criar nova página para cada turma
                    val currentPage = document.startPage(PdfDocument.PageInfo.Builder(842, 595, pageNumber).create())
                    val canvas = currentPage.canvas
                    val paint = Paint()

                    // Configurações de texto
                    paint.isAntiAlias = true
                    paint.textSize = 10f
                    paint.color = android.graphics.Color.BLACK

                    var y = 30f
                    val margin = 30f
                    val lineHeight = 15f

                    // Título
                    paint.textSize = 16f
                    paint.isFakeBoldText = true
                    canvas.drawText("Grade Horária - Relatório", margin, y, paint)
                    y += lineHeight * 2

                    // Nome da turma
                    paint.textSize = 14f
                    paint.isFakeBoldText = true
                    canvas.drawText("Turma ${dadosTabela.target}", margin, y, paint)
                    y += lineHeight * 2

                    // Desenhar tabela da turma
                    desenharTabelaGrade(canvas, dadosTabela, margin, y, paint)

                    // Finalizar página
                    document.finishPage(currentPage)
                    pageNumber++

                    Log.d(TAG, "Página ${pageNumber - 1} finalizada para Turma ${dadosTabela.target}")
                }
            } else {
                Log.d(TAG, "Mensagem ${index + 1} não contém dados de grade válidos")
            }
        }

        if (pageNumber == 1) {
            Log.e(TAG, "Nenhuma turma válida encontrada nas mensagens selecionadas")
            Toast.makeText(this, getString(R.string.nenhuma_grade_valida_encontrada), Toast.LENGTH_SHORT).show()
            document.close()
            return
        }

        // Salvar arquivo
        val arquivo = File(getExternalFilesDir(null), "grade_horaria_multiplas_turmas_${System.currentTimeMillis()}.pdf")
        Log.d(TAG, "Salvando PDF em: ${arquivo.absolutePath}")

        val outputStream = FileOutputStream(arquivo)
        document.writeTo(outputStream)
        document.close()
        outputStream.close()

        Log.d(TAG, "PDF gerado com sucesso: ${arquivo.length()} bytes, ${pageNumber - 1} páginas")

        // Compartilhar arquivo
        compartilharArquivo(arquivo)

        // Mostrar mensagem de sucesso
        Toast.makeText(this, getString(R.string.pdf_gerado_sucesso_todas_turmas, pageNumber - 1), Toast.LENGTH_LONG).show()
    }

    private fun exportarMensagensSelecionadasRelatorio() {
        val posicoesSelecionadas = chatAdapter.getMensagensSelecionadas()
        if (posicoesSelecionadas.isEmpty()) {
            Toast.makeText(this, getString(R.string.nenhuma_mensagem_selecionada), Toast.LENGTH_SHORT).show()
            return
        }

        val mensagensSelecionadas = posicoesSelecionadas.map { posicao ->
            chatAdapter.getMensagens()[posicao]
        }

        Log.d(TAG, "=== EXPORTANDO ${mensagensSelecionadas.size} MENSAGENS COMO RELATÓRIO ===")

        // Gerar relatório com todas as mensagens selecionadas
        gerarRelatorioMultiplasMensagens(mensagensSelecionadas)
        
        // Limpar seleção após exportar
        limparSelecao()
    }

    private fun gerarRelatorioMultiplasMensagens(mensagens: List<MensagemChat>) {
        Log.d(TAG, "=== GERANDO RELATÓRIO COM MÚLTIPLAS MENSAGENS ===")
        Log.d(TAG, "Total de mensagens: ${mensagens.size}")

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 retrato
        var currentPage = document.startPage(pageInfo)
        var canvas = currentPage.canvas
        var pageNumber = 1

        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }

        val paintTitulo = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 16f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val paintSubtitulo = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }

        // NOVO: Paint para texto em negrito
        val paintNegrito = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
        }

        var y = 50f
        val margin = 50f
        val indentacao = 20f // Indentação da primeira linha do parágrafo
        val lineHeight = 14f
        val maxY = 800f

        // Processar cada mensagem
        mensagens.forEachIndexed { index, mensagem ->
            if (mensagem.isIA) {
                Log.d(TAG, "Processando mensagem ${index + 1}/${mensagens.size}")
                
                val texto = mensagem.texto
                val larguraMaxima = 595f - 2 * margin
                
                // CORREÇÃO: Usar nova função que processa asteriscos
                val linhas = quebrarTextoFormatadoComAsteriscos(texto, paint, paintNegrito, paintSubtitulo, larguraMaxima)
                
                // Desenhar linhas do texto
                var isPrimeiraLinhaTitulo = true
                var isPrimeiraLinhaParagrafo = true
                var isPrimeiraLinhaDoRelatorio = true // NOVO: Flag para primeira linha
                
                linhas.forEach { linha ->
                    if (y + lineHeight > maxY) {
                        Log.d(TAG, "Criando nova página ${pageNumber + 1}")
                        document.finishPage(currentPage)
                        currentPage = document.startPage(PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create())
                        canvas = currentPage.canvas
                        pageNumber++
                        y = 50f
                        isPrimeiraLinhaTitulo = true
                        isPrimeiraLinhaParagrafo = true
                        isPrimeiraLinhaDoRelatorio = true // Reset para nova página
                    }
                    
                    // Verificar se é um título
                    if (linha.startsWith("TITULO:")) {
                        val titulo = linha.substring(7) // Remove "TITULO:"
                        
                        // ADICIONAR ESPAÇAMENTO EXTRA APENAS NA PRIMEIRA LINHA DO TÍTULO
                        if (isPrimeiraLinhaTitulo) {
                            y += lineHeight * 1.5f // Espaçamento extra de 21 pixels antes do título
                            isPrimeiraLinhaTitulo = false
                        }
                        
                        // APLICAR JUSTIFICAÇÃO AOS TÍTULOS (exceto última linha)
                        val larguraTexto = paintSubtitulo.measureText(titulo)
                        val espacoDisponivel = 595f - 2 * margin
                        val espacoExtra = espacoDisponivel - larguraTexto
                        
                        if (espacoExtra > 0 && titulo.contains(" ")) {
                            // Aplicar justificação distribuindo espaços
                            val palavras = titulo.split(" ")
                            if (palavras.size > 1) {
                                val espacoEntrePalavras = espacoExtra / (palavras.size - 1)
                                var xAtual = margin
                                
                                for (i in palavras.indices) {
                                    canvas.drawText(palavras[i], xAtual, y, paintSubtitulo)
                                    if (i < palavras.size - 1) {
                                        xAtual += paintSubtitulo.measureText(palavras[i] + " ") + espacoEntrePalavras
                                    }
                                }
                            } else {
                                canvas.drawText(titulo, margin, y, paintSubtitulo)
                            }
                        } else {
                            canvas.drawText(titulo, margin, y, paintSubtitulo)
                        }

//                        y += lineHeight * 1.5f // Espaçamento extra para títulos

                        // NOVO: Se for a primeira linha do relatório, adicionar quebra extra
//                        if (isPrimeiraLinhaDoRelatorio) {
//                            y += lineHeight * 1.5f // Quebra de linha extra após o título principal
//                            isPrimeiraLinhaDoRelatorio = false
//                        }
                        
                        isPrimeiraLinhaParagrafo = true // Reset para próximo parágrafo
                    } else if (linha.startsWith("TITULO_ULTIMA_LINHA:")) {
                        // Última linha do título - alinhar à esquerda sem justificação
                        val titulo = linha.substring(20) // Remove "TITULO_ULTIMA_LINHA:"
                        canvas.drawText(titulo, margin, y, paintSubtitulo)
                        y += lineHeight * 1.2f  // Espaçamento extra para títulos
                        
                        // NOVO: Se for a primeira linha do relatório, adicionar quebra extra
//                        if (isPrimeiraLinhaDoRelatorio) {
//                            y += lineHeight * 1.1f // Quebra de linha extra após o título principal
//                            isPrimeiraLinhaDoRelatorio = false
//                        }
                        
                        isPrimeiraLinhaTitulo = true // Reset para próximo título
                        isPrimeiraLinhaParagrafo = true // Reset para próximo parágrafo
                    } else if (linha.startsWith("ULTIMA_LINHA:")) {
                        // Última linha do parágrafo - alinhar à esquerda sem justificação
                        val texto = linha.substring(13) // Remove "ULTIMA_LINHA:"
                        val margemAtual = if (isPrimeiraLinhaParagrafo) margin + indentacao else margin
                        
                        // NOVO: Processar texto com asteriscos
                        desenharTextoComAsteriscos(canvas, texto, margemAtual, y, paint, paintNegrito, larguraMaxima)
                        
                        y += lineHeight
                        isPrimeiraLinhaTitulo = true // Reset para próximo título
                        isPrimeiraLinhaParagrafo = true // Reset para próximo parágrafo
                    } else if (linha.isNotEmpty()) {
                        // Texto normal - aplicar justificação
                        val margemAtual = if (isPrimeiraLinhaParagrafo) margin + indentacao else margin
                        val larguraMaximaAtual = 595f - 2 * margin - (if (isPrimeiraLinhaParagrafo) indentacao else 0f)
                        
                        // CORREÇÃO: Aplicar justificação para texto normal
                        val larguraTexto = paint.measureText(linha)
                        val espacoDisponivel = larguraMaximaAtual
                        val espacoExtra = espacoDisponivel - larguraTexto
                        
                        if (espacoExtra > 0 && linha.contains(" ") && !linha.startsWith("ULTIMA_LINHA:")) {
                            // Aplicar justificação distribuindo espaços
                            val palavras = linha.split(" ")
                            if (palavras.size > 1) {
                                val espacoEntrePalavras = espacoExtra / (palavras.size - 1)
                                var xAtual = margemAtual
                                
                                for (i in palavras.indices) {
                                    // NOVO: Processar texto com asteriscos para cada palavra
                                    desenharTextoComAsteriscos(canvas, palavras[i], xAtual, y, paint, paintNegrito, espacoDisponivel)
                                    if (i < palavras.size - 1) {
                                        xAtual += paint.measureText(palavras[i] + " ") + espacoEntrePalavras
                                    }
                                }
                            } else {
                                // NOVO: Processar texto com asteriscos
                                desenharTextoComAsteriscos(canvas, linha, margemAtual, y, paint, paintNegrito, larguraMaximaAtual)
                            }
                        } else {
                            // NOVO: Processar texto com asteriscos
                            desenharTextoComAsteriscos(canvas, linha, margemAtual, y, paint, paintNegrito, larguraMaximaAtual)
                        }
                        
                        y += lineHeight
                        isPrimeiraLinhaParagrafo = false // Não é mais primeira linha
                        isPrimeiraLinhaTitulo = true // Reset para próximo título
                    } else {
                        // Linha em branco
                        y += lineHeight / 2
                        isPrimeiraLinhaTitulo = true // Reset para próximo título
                        isPrimeiraLinhaParagrafo = true // Reset para próximo parágrafo
                    }
                }
                
                y += lineHeight // Espaço entre mensagens
            }
        }

        document.finishPage(currentPage)
        Log.d(TAG, "PDF gerado com $pageNumber páginas")

        // Salvar arquivo
        val arquivo = File(getExternalFilesDir(null), "relatorio_consultor_ia_${System.currentTimeMillis()}.pdf")
        Log.d(TAG, "Salvando PDF em: ${arquivo.absolutePath}")

        val outputStream = FileOutputStream(arquivo)
        document.writeTo(outputStream)
        document.close()
        outputStream.close()

        Log.d(TAG, "PDF gerado com sucesso: ${arquivo.length()} bytes")

        // Compartilhar arquivo
        compartilharArquivoRelatorio(arquivo)

        // Mostrar mensagem de sucesso
        Toast.makeText(this, getString(R.string.relatorio_gerado_sucesso), Toast.LENGTH_SHORT).show()
    }

    // NOVA FUNÇÃO: Processar texto com asteriscos
    private fun quebrarTextoFormatadoComAsteriscos(texto: String, paintTexto: Paint, paintNegrito: Paint, paintTitulo: Paint, larguraMaxima: Float): List<String> {
        val linhas = mutableListOf<String>()
        
        // Dividir por quebras de linha simples (\n) em vez de parágrafos (\n\n)
        val linhasOriginais = texto.split("\n")
        
        for (linha in linhasOriginais) {
            val linhaTrimmed = linha.trim()
            
            if (linhaTrimmed.isEmpty()) {
                linhas.add("") // Linha em branco
                continue
            }
            
            // Verificar se é um título (começa com ** e termina com **)
            if (linhaTrimmed.startsWith("**") && linhaTrimmed.endsWith("**")) {
                val titulo = linhaTrimmed.substring(2, linhaTrimmed.length - 2).trim()
                
                // Adicionar título como TITULO
                val linhasTitulo = quebrarTitulo(titulo, paintTitulo, larguraMaxima)
                for (j in linhasTitulo.indices) {
                    val isUltimaLinha = j == linhasTitulo.size - 1
                    if (isUltimaLinha) {
                        linhas.add("TITULO_ULTIMA_LINHA:${linhasTitulo[j]}")
                    } else {
                        linhas.add("TITULO:${linhasTitulo[j]}")
                    }
                }
                continue
            }
            
            // Verificar se é um título numerado (começa com número seguido de ponto)
            if (linhaTrimmed.matches(Regex("^\\d+\\..*"))) {
                // Adicionar título como TITULO
                val linhasTitulo = quebrarTitulo(linhaTrimmed, paintTitulo, larguraMaxima)
                for (j in linhasTitulo.indices) {
                    val isUltimaLinha = j == linhasTitulo.size - 1
                    if (isUltimaLinha) {
                        linhas.add("TITULO_ULTIMA_LINHA:${linhasTitulo[j]}")
                    } else {
                        linhas.add("TITULO:${linhasTitulo[j]}")
                    }
                }
                continue
            }
            
            // Verificar se é um subtítulo (texto em maiúsculas seguido de :)
            if (linhaTrimmed.matches(Regex("^[A-Z][A-Z\\s]+:$"))) {
                // Adicionar subtítulo como TITULO
                val linhasTitulo = quebrarTitulo(linhaTrimmed, paintTitulo, larguraMaxima)
                for (j in linhasTitulo.indices) {
                    val isUltimaLinha = j == linhasTitulo.size - 1
                    if (isUltimaLinha) {
                        linhas.add("TITULO_ULTIMA_LINHA:${linhasTitulo[j]}")
                    } else {
                        linhas.add("TITULO:${linhasTitulo[j]}")
                    }
                }
                continue
            }
            
            // Texto normal - quebrar se necessário
            val linhasTexto = quebrarParagrafo(linhaTrimmed, paintTexto, larguraMaxima)
            for (j in linhasTexto.indices) {
                val isUltimaLinha = j == linhasTexto.size - 1
                if (isUltimaLinha) {
                    linhas.add("ULTIMA_LINHA:${linhasTexto[j]}")
                } else {
                    linhas.add(linhasTexto[j])
                }
            }
        }
        
        return linhas
    }

    // NOVA FUNÇÃO: Desenhar texto com asteriscos
    private fun desenharTextoComAsteriscos(canvas: Canvas, texto: String, x: Float, y: Float, paintNormal: Paint, paintNegrito: Paint, larguraMaxima: Float) {
        var xAtual = x
        val regex = Regex("\\*\\*(.*?)\\*\\*")
        var ultimoIndex = 0
        
        regex.findAll(texto).forEach { matchResult ->
            val startIndex = matchResult.range.first
            val endIndex = matchResult.range.last + 1
            val textoNegrito = matchResult.groupValues[1]
            
            // Desenhar texto normal antes do asterisco
            if (startIndex > ultimoIndex) {
                val textoAntes = texto.substring(ultimoIndex, startIndex)
                canvas.drawText(textoAntes, xAtual, y, paintNormal)
                xAtual += paintNormal.measureText(textoAntes)
            }
            
            // Desenhar texto em negrito
            canvas.drawText(textoNegrito, xAtual, y, paintNegrito)
            xAtual += paintNegrito.measureText(textoNegrito)
            
            ultimoIndex = endIndex
        }
        
        // Desenhar texto restante
        if (ultimoIndex < texto.length) {
            val textoRestante = texto.substring(ultimoIndex)
            canvas.drawText(textoRestante, xAtual, y, paintNormal)
        }
    }

    private fun quebrarTextoFormatado(texto: String, paint: Paint, larguraMaxima: Float): List<String> {
        val linhas = mutableListOf<String>()
        val paragrafos = texto.split("\n\n")
        
        for (paragrafo in paragrafos) {
            if (paragrafo.trim().isEmpty()) {
                linhas.add("") // Linha em branco
                continue
            }
            
            // Verifica se é um título (começa com #, número seguido de ponto, ou texto em maiúsculas seguido de :)
            val isTitulo = paragrafo.trim().startsWith("#") || 
                          paragrafo.trim().matches(Regex("^\\d+\\..*")) ||
                          paragrafo.trim().matches(Regex("^[A-Z][A-Z\\s]+:.*"))
            
            if (isTitulo) {
                // Adiciona linha em branco antes do título (exceto se for o primeiro)
                if (linhas.isNotEmpty() && linhas.last().isNotEmpty()) {
                    linhas.add("")
                }
                
                // Remove símbolos de formatação e adiciona como título
                val tituloLimpo = paragrafo.trim()
                    .replace("#", "")
                    .replace("*", "")
                    .trim()
                
                // QUEBRAR TÍTULO SE FOR MUITO LONGO - USAR PAINT CORRETO
                val linhasTitulo = quebrarTitulo(tituloLimpo, paint, larguraMaxima)
                for (linhaTitulo in linhasTitulo) {
                    linhas.add("TITULO:$linhaTitulo")
                }
                linhas.add("") // Linha em branco após o título
                continue
            }
            
            // Processa parágrafo normal - remove símbolos de formatação
            val paragrafoLimpo = paragrafo.trim()
                .replace("*", "")
                .replace("#", "")
                .trim()
            
            val palavras = paragrafoLimpo.split(" ")
            var linhaAtual = ""
            val linhasParagrafo = mutableListOf<String>()
            
            for (palavra in palavras) {
                val linhaTeste = if (linhaAtual.isEmpty()) palavra else "$linhaAtual $palavra"
                val larguraTeste = paint.measureText(linhaTeste)
                
                if (larguraTeste > larguraMaxima && linhaAtual.isNotEmpty()) {
                    linhasParagrafo.add(linhaAtual)
                    linhaAtual = palavra
                } else {
                    linhaAtual = linhaTeste
                }
            }
            
            if (linhaAtual.isNotEmpty()) {
                linhasParagrafo.add(linhaAtual)
            }
            
            // Adiciona linhas do parágrafo com marcação da última linha
            for (i in linhasParagrafo.indices) {
                val isUltimaLinha = i == linhasParagrafo.size - 1
                if (isUltimaLinha) {
                    linhas.add("ULTIMA_LINHA:${linhasParagrafo[i]}")
                } else {
                    linhas.add(linhasParagrafo[i])
                }
            }
            
            // Adiciona linha em branco após o parágrafo
            linhas.add("")
        }
        
        return linhas
    }
    
    private fun quebrarTitulo(titulo: String, paint: Paint, larguraMaxima: Float): List<String> {
        val linhas = mutableListOf<String>()
        val palavras = titulo.split(" ")
        var linhaAtual = ""
        
        for (palavra in palavras) {
            val linhaTeste = if (linhaAtual.isEmpty()) palavra else "$linhaAtual $palavra"
            val larguraTeste = paint.measureText(linhaTeste)
            
            if (larguraTeste > larguraMaxima && linhaAtual.isNotEmpty()) {
                linhas.add(linhaAtual)
                linhaAtual = palavra
            } else {
                linhaAtual = linhaTeste
            }
        }
        
        if (linhaAtual.isNotEmpty()) {
            linhas.add(linhaAtual)
        }
        
        return linhas
    }
    
    private fun quebrarParagrafo(texto: String, paint: Paint, larguraMaxima: Float): List<String> {
        val linhas = mutableListOf<String>()
        val palavras = texto.split(" ")
        var linhaAtual = ""
        
        for (palavra in palavras) {
            val linhaTeste = if (linhaAtual.isEmpty()) palavra else "$linhaAtual $palavra"
            val larguraLinha = paint.measureText(linhaTeste)
            
            if (larguraLinha <= larguraMaxima) {
                linhaAtual = linhaTeste
            } else {
                if (linhaAtual.isNotEmpty()) {
                    linhas.add(linhaAtual)
                    linhaAtual = palavra
                } else {
                    // Palavra muito longa, quebrar no meio
                    val palavraQuebrada = quebrarPalavraLonga(palavra, paint, larguraMaxima)
                    linhas.addAll(palavraQuebrada)
                    linhaAtual = ""
                }
            }
        }
        
        if (linhaAtual.isNotEmpty()) {
            linhas.add(linhaAtual)
        }
        
        return linhas
    }
    
    private fun quebrarPalavraLonga(palavra: String, paint: Paint, larguraMaxima: Float): List<String> {
        val linhas = mutableListOf<String>()
        var palavraRestante = palavra
        
        while (palavraRestante.isNotEmpty()) {
            var linha = ""
            var i = 0
            
            while (i < palavraRestante.length) {
                val linhaTeste = linha + palavraRestante[i]
                val larguraLinha = paint.measureText(linhaTeste)
                
                if (larguraLinha <= larguraMaxima) {
                    linha = linhaTeste
                    i++
                } else {
                    break
                }
            }
            
            if (linha.isEmpty()) {
                // Caractere muito largo, adicionar mesmo assim
                linha = palavraRestante[0].toString()
                i = 1
            }
            
            linhas.add(linha)
            palavraRestante = palavraRestante.substring(i)
        }
        
        return linhas
    }

    private fun compartilharArquivoRelatorio(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
//                putExtra(Intent.EXTRA_SUBJECT, "Relatório Consultor IA")
                putExtra(Intent.EXTRA_TEXT, "Relatório gerado pelo sistema Educa1")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(intent, "Compartilhar Relatório"))
            Log.d(TAG, "Arquivo compartilhado com sucesso")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao compartilhar arquivo", e)
            Toast.makeText(this, getString(R.string.erro_compartilhar, e.message ?: ""), Toast.LENGTH_LONG).show()
        }
    }

    private fun aplicarGradeSelecionada() {
        Log.d(TAG, "=== APLICANDO GRADE SELECIONADA ===")
        
        val mensagensSelecionadas = chatAdapter.getMensagensSelecionadas()
        if (mensagensSelecionadas.isEmpty()) {
            Toast.makeText(this, getString(R.string.nenhuma_mensagem_selecionada), Toast.LENGTH_SHORT).show()
            return
        }

        // Pegar a primeira mensagem selecionada (assumindo que é uma grade)
        val mensagemGrade = chatAdapter.getMensagens()[mensagensSelecionadas.first()]
        Log.d(TAG, "Aplicando grade: ${mensagemGrade.texto.take(100)}...")

        // Parse da grade
        val listaTurmas = parsearRespostaIA(mensagemGrade.texto)
        
        if (listaTurmas == null || listaTurmas.isEmpty()) {
            Toast.makeText(this, getString(R.string.nao_foi_possivel_extrair_grade), Toast.LENGTH_SHORT).show()
            return
        }

        // MODIFICAÇÃO: Aplicar grade para TODAS as turmas automaticamente
        Log.d(TAG, "Aplicando grade para ${listaTurmas.size} turmas automaticamente")
        
        var turmasAplicadas = 0
        var turmasComErro = 0
        
        listaTurmas.forEach { dadosTabela ->
            try {
                aplicarGradeParaTurma(dadosTabela)
                turmasAplicadas++
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao aplicar grade para Turma ${dadosTabela.target}", e)
                turmasComErro++
            }
        }
        
        // Mostrar resultado final
        when {
            turmasComErro == 0 -> {
                Toast.makeText(this, getString(R.string.grade_aplicada_sucesso, turmasAplicadas), Toast.LENGTH_LONG).show()
            }
            turmasAplicadas > 0 -> {
                Toast.makeText(this, getString(R.string.grade_aplicada_parcial, turmasAplicadas, turmasComErro), Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(this, getString(R.string.grade_aplicada_erro), Toast.LENGTH_LONG).show()
            }
        }
        
        // Limpar seleção
        limparSelecao()
    }

    private fun aplicarGradeParaTurma(dadosTabela: DadosTabelaGrade) {
        Log.d(TAG, "=== APLICANDO GRADE PARA TURMA ${dadosTabela.target} ===")
        
        try {
            // Converter dados da tabela para CelulaHorario
            val celulasHorario = converterParaCelulasHorario(dadosTabela)
            
            // Salvar no SharedPreferences usando o nome da turma
            salvarGradeNoSharedPreferences(dadosTabela.target, celulasHorario)
            
            Log.d(TAG, "Grade salva no SharedPreferences para turma ${dadosTabela.target}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao aplicar grade para Turma ${dadosTabela.target}", e)
            throw e // Re-throw para ser capturado pelo forEach
        }
    }

    private fun converterParaCelulasHorario(dadosTabela: DadosTabelaGrade): List<CelulaHorario> {
        val celulas = mutableListOf<CelulaHorario>()
        
        // NOVO: Carregar configuração global
        val configuracaoManager = ConfiguracaoGradeManager(this)
        val configuracaoGrade = configuracaoManager.carregarConfiguracao()
        
        // Mapear dias da semana - PORTUGUÊS E ESPANHOL
        val diasMap = mapOf(
            // Português
            "Segunda" to 0,
            "Terça" to 1,
            "Quarta" to 2,
            "Quinta" to 3,
            "Sexta" to 4,
            // Espanhol
            "Lunes" to 0,
            "Martes" to 1,
            "Miércoles" to 2,
            "Jueves" to 3,
            "Viernes" to 4
        )
        
        // NOVO: Criar grade vazia baseada na configuração
        val totalCelulas = configuracaoGrade.getTotalCelulas()
        for (i in 0 until totalCelulas) {
            celulas.add(CelulaHorario(id = i.toString()))
        }
        
        Log.d(TAG, "Convertendo grade para Turma ${dadosTabela.target}")
        Log.d(TAG, "Dias da semana: ${dadosTabela.diasDaSemana}")
        Log.d(TAG, "Total de células: $totalCelulas")
        
        // Agora preencher com os dados da IA
        dadosTabela.aulas.forEachIndexed { diaIndex, aulasDoDia ->
            val diaNome = dadosTabela.diasDaSemana[diaIndex]
            val diaNumero = diasMap[diaNome]
            
            if (diaNumero == null) {
                Log.e(TAG, "Dia não encontrado no mapeamento: $diaNome")
                return@forEachIndexed
            }
            
            Log.d(TAG, "Processando $diaNome (índice: $diaNumero)")
            
            aulasDoDia.forEachIndexed { periodoIndex, aula ->
                if (aula.disciplina.isNotEmpty() && aula.professor.isNotEmpty()) {
                    // NOVO: Calcular posição baseada na configuração (sempre 5 dias)
                    val posicao = (periodoIndex * 5) + diaNumero
                    
                    if (posicao < celulas.size) {
                        // Buscar objetos Disciplina e Professor pelos nomes
                        val disciplina = buscarDisciplinaPorNome(aula.disciplina)
                        val professor = buscarProfessorPorNome(aula.professor)
                        val turma = buscarTurmaPorId(dadosTabela.target)
                        
                        Log.d(TAG, "Posição $posicao: ${aula.disciplina} - ${aula.professor}")
                        Log.d(TAG, "Disciplina encontrada: ${disciplina?.nome ?: "NENHUMA"}")
                        Log.d(TAG, "Professor encontrado: ${professor?.nome ?: "NENHUM"}")
                        Log.d(TAG, "Turma encontrada: ${turma?.nome ?: "NENHUMA"}")
                        
                        celulas[posicao] = celulas[posicao].copy(
                            disciplina = disciplina,
                            professor = professor,
                            turma = turma
                        )
                    }
                }
            }
        }
        
        Log.d(TAG, "Convertidas ${celulas.size} células de horário")
        return celulas
    }

    private fun buscarDisciplinaPorNome(nome: String): Disciplina? {
        val prefs = getSharedPreferences(GerenciarDisciplinasActivity.PREFS_NAME, MODE_PRIVATE)
        val json = prefs.getString(GerenciarDisciplinasActivity.KEY_DISCIPLINAS, "[]")
        val disciplinas = Gson().fromJson(json, Array<Disciplina>::class.java).toList()
        
        return disciplinas.find { it.nome.equals(nome, ignoreCase = true) }
    }

    private fun buscarProfessorPorNome(nome: String): Professor? {
        val prefs = getSharedPreferences(GerenciarProfessoresActivity.PREFS_NAME, MODE_PRIVATE)
        val json = prefs.getString(GerenciarProfessoresActivity.KEY_PROFESSORES, "[]")
        val professores = Gson().fromJson(json, Array<Professor>::class.java).toList()
        
        return professores.find { it.nome.equals(nome, ignoreCase = true) }
    }

    private fun buscarTurmaPorId(turmaId: String): Turma? {
        val prefs = getSharedPreferences(GerenciarTurmasActivity.PREFS_NAME, MODE_PRIVATE)
        val json = prefs.getString(GerenciarTurmasActivity.KEY_TURMAS, "[]")
        val turmas = Gson().fromJson(json, Array<Turma>::class.java).toList()
        
        Log.d(TAG, "Buscando turma com ID: $turmaId")
        Log.d(TAG, "Turmas disponíveis: ${turmas.map { "${it.id} - ${it.nome}" }}")
        
        val turmaEncontrada = turmas.find { it.id.toString() == turmaId }
        Log.d(TAG, "Turma encontrada: ${turmaEncontrada?.nome ?: "NENHUMA"}")
        
        return turmaEncontrada
    }

    private fun salvarGradeNoSharedPreferences(turmaId: String, celulas: List<CelulaHorario>) {
        val prefs = getSharedPreferences("GradesHorariasPrefs", MODE_PRIVATE).edit()
        
        // Converter para JSON
        val type = object : TypeToken<List<CelulaHorario>>() {}.type
        val json = Gson().toJson(celulas, type)
        
        // ADICIONAR LOGS PARA DEBUG
        Log.d(TAG, "Salvando grade com chave: grade_$turmaId")
        Log.d(TAG, "JSON gerado: ${json.take(200)}...")
        
        // Usar o nome da turma (mais legível)
        prefs.putString("grade_$turmaId", json)
        prefs.apply()
        
        Log.d(TAG, "Grade salva no SharedPreferences para turma $turmaId")
        
        // VERIFICAR SE FOI SALVA CORRETAMENTE
        val prefsRead = getSharedPreferences("GradesHorariasPrefs", MODE_PRIVATE)
        val jsonSalvo = prefsRead.getString("grade_$turmaId", null)
        Log.d(TAG, "Verificação: Grade salva? ${jsonSalvo != null}")
        if (jsonSalvo != null) {
            Log.d(TAG, "JSON salvo verificado: ${jsonSalvo.take(200)}...")
        }
    }

    private fun detectarTipoExportacao(mensagens: List<MensagemChat>): TipoExportacao {
        // Verificar se alguma mensagem contém dados de grade usando a mesma lógica de gerarPDFMultiplasMensagens
        for (mensagem in mensagens) {
            if (mensagem.isIA) {
                // Usar a mesma função que gerarPDFMultiplasMensagens usa
                val listaTurmas = parsearRespostaIA(mensagem.texto)
                
                if (listaTurmas != null && listaTurmas.isNotEmpty()) {
                    return TipoExportacao.GRADE
                }
            }
        }
        
        // Se não encontrou turmas válidas, é relatório
        return TipoExportacao.RELATORIO
    }
} 