package com.example.educa1.activitys

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.educa1.CustomButtonView
import com.example.educa1.R
import com.example.educa1.adapters.DisponibilidadeAdapter
import com.example.educa1.databinding.ActivityDetalhesProfessorBinding
import com.example.educa1.models.Disciplina
import com.example.educa1.models.Professor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DetalhesProfessorActivity : BaseActivity() {

    private lateinit var binding: ActivityDetalhesProfessorBinding

    private var professor: Professor? = null

    private lateinit var listaDeTodasDisciplinas: List<String>

    private var dadosForamAlterados = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalhesProfessorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura a Toolbar
        setSupportActionBar(binding.toolbarDetalhesProfessor)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Adiciona o botão "Voltar"
        binding.toolbarDetalhesProfessor.setNavigationIcon(R.drawable.ic_arrow_back_white)

        carregarTodasDisciplinas()
        recuperarProfessor()

        professor?.let {
            supportActionBar?.title = it.nome // Define o nome do professor como título da Toolbar
            preencherDados(it)
            configurarGridDisponibilidade(it)
        } ?: run {
            Toast.makeText(this, "Erro ao carregar dados do professor.", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnAddDisciplina.setOnClickListener {
            mostrarDialogoAdicionarDisciplina()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finalizarAtividade()
                }
            }
        )


    }

    override fun onSupportNavigateUp(): Boolean {
        finalizarAtividade()
        return true
    }

    private fun preencherDados(prof: Professor) {
        binding.etAulasContratadas.setText(prof.aulasContratadas.toString()) // <<< ADICIONE

        // Configurar o EditText para fechar o teclado quando OK for pressionado
        binding.etAulasContratadas.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                // Esconde o teclado
                val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(binding.etAulasContratadas.windowToken, 0)
                // Remove o foco do EditText
                binding.etAulasContratadas.clearFocus()
                return@setOnEditorActionListener true
            }
            false
        }

        // Limpa a lista visual antes de recriá-la
        binding.lnaDisciplinasProfessor.removeAllViews()

        // Para cada disciplina do professor, cria um botão
        prof.disciplinas.forEach { nomeDisciplina ->
            criarVisualBotaoDisciplina(nomeDisciplina)
        }
    }

    private fun criarVisualBotaoDisciplina(nomeDisciplina: String) {
        // Usa o LayoutInflater para criar uma nova instância do nosso layout botao_disciplina.xml
        val inflater = LayoutInflater.from(this)
        val novoBotao = inflater.inflate(R.layout.botao_disciplina, binding.lnaDisciplinasProfessor, false) as CustomButtonView

        // Configura o botão recém-criado
        novoBotao.apply {
            // Define o texto da disciplina
            text = nomeDisciplina

            // Adiciona a margem inferior dinamicamente
            (layoutParams as LinearLayout.LayoutParams).apply {
                val marginBottom = resources.getDimensionPixelSize(R.dimen.margem_inferior_botao_novo)
                setMargins(0, 0, 0, marginBottom)
            }

            // Configura a ação de deletar (remover do professor)
            setOnDeleteClickListener {
                // Remove da lista de dados
                professor?.disciplinas?.remove(nomeDisciplina)
                // Remove da lista visual
                binding.lnaDisciplinasProfessor.removeView(this)
                // Marca que houve alteração para salvar ao sair
                dadosForamAlterados = true
                Toast.makeText(this@DetalhesProfessorActivity, "'$nomeDisciplina' removida do professor.", Toast.LENGTH_SHORT).show()
            }
        }

        // Adiciona o botão pronto ao LinearLayout
        binding.lnaDisciplinasProfessor.addView(novoBotao)
    }

    private fun mostrarDialogoAdicionarDisciplina() {
        val disciplinasParaEscolher = listaDeTodasDisciplinas
            .filter { disciplina -> professor?.disciplinas?.contains(disciplina) == false }
            .toTypedArray()

        if (disciplinasParaEscolher.isEmpty()) {
            Toast.makeText(this, getString(R.string.todas_disciplinas_adicionadas), Toast.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("Selecione a Disciplina")
            .setItems(disciplinasParaEscolher) { _, which ->
                val disciplinaSelecionada = disciplinasParaEscolher[which]

                // Adiciona na lista de dados
                professor?.disciplinas?.add(disciplinaSelecionada)
                professor?.disciplinas?.sort()

                // Cria o botão visual para a nova disciplina
                criarVisualBotaoDisciplina(disciplinaSelecionada)

                dadosForamAlterados = true
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun recuperarProfessor() {
        professor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("PROFESSOR_EXTRA", Professor::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("PROFESSOR_EXTRA")
        }
    }

    private fun finalizarAtividade() {
        // Guarda o número de aulas que o professor TINHA quando a tela abriu.
        // Usamos o 'intent' para pegar o valor original novamente.
        val aulasOriginais = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("PROFESSOR_EXTRA", Professor::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Professor>("PROFESSOR_EXTRA")
        })?.aulasContratadas ?: 0

        // Lê o novo valor do EditText.
        val aulasString = binding.etAulasContratadas.text.toString()
        val aulasContratadasNovas = aulasString.toIntOrNull() ?: 0

        // Atualiza o objeto 'professor' que temos em memória.
        professor?.aulasContratadas = aulasContratadasNovas

        // <<< A CORREÇÃO PRINCIPAL ESTÁ AQUI >>>
        // Verifica se o número de aulas mudou. Se mudou, ativa a flag.
        if (aulasContratadasNovas != aulasOriginais) {
            dadosForamAlterados = true
        }

        // SALVAR NO SHAREDPREFERENCES
        if (dadosForamAlterados) {
            professor?.let { prof ->
            val sharedPreferences = getSharedPreferences(GerenciarProfessoresActivity.PREFS_NAME, MODE_PRIVATE)
            val jsonString = sharedPreferences.getString(GerenciarProfessoresActivity.KEY_PROFESSORES, null)
            if (jsonString != null) {
                val type = object : TypeToken<MutableList<Professor>>() {}.type
                val listaDeProfessores: MutableList<Professor> = Gson().fromJson(jsonString, type)
                val index = listaDeProfessores.indexOfFirst { it.id == prof.id }
                if (index != -1) {
                    listaDeProfessores[index] = prof
                    val editor = sharedPreferences.edit()
                    val novaJsonString = Gson().toJson(listaDeProfessores)
                    editor.putString(GerenciarProfessoresActivity.KEY_PROFESSORES, novaJsonString)
                    editor.apply()
                }
            }
        }
        }

        // O resto da função continua igual.
        val resultIntent = Intent()
        if (dadosForamAlterados) {
            resultIntent.putExtra("PROFESSOR_ATUALIZADO", professor)
            setResult(RESULT_OK, resultIntent)
        } else {
            setResult(RESULT_CANCELED, resultIntent)
        }
        finish()
    }

    private fun carregarTodasDisciplinas() {
        val sharedPreferences = getSharedPreferences(GerenciarDisciplinasActivity.PREFS_NAME, MODE_PRIVATE)
        val jsonString = sharedPreferences.getString(GerenciarDisciplinasActivity.KEY_DISCIPLINAS, null)
        listaDeTodasDisciplinas = if (jsonString != null) {
            val type = object : TypeToken<List<Disciplina>>() {}.type
            val disciplinasSalvas: List<Disciplina> = Gson().fromJson(jsonString, type)
            disciplinasSalvas.map { it.nome }
        } else {
            emptyList()
        }
    }

    private fun configurarGridDisponibilidade(prof: Professor) {
        val totalDeSlots = 25 // 5 dias * 5 aulas
        val slotsIndisponiveis = prof.indisponibilidades.toSet()

        val adapter = DisponibilidadeAdapter(totalDeSlots, slotsIndisponiveis) { slotId, estavaIndisponivel ->
            // Lógica de clique: inverte o estado
            if (estavaIndisponivel) {
                prof.indisponibilidades.remove(slotId)
            } else {
                prof.indisponibilidades.add(slotId)
            }
            dadosForamAlterados = true // Marca que houve mudança para salvar ao sair

            // Recria o adapter com os dados atualizados para forçar a UI a redesenhar
            configurarGridDisponibilidade(prof)
        }
        binding.rvDisponibilidade.layoutManager = GridLayoutManager(this, 5) // 5 colunas
        binding.rvDisponibilidade.adapter = adapter
    }
}
