package com.example.educa1.activitys

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.example.educa1.CustomButtonView
import com.example.educa1.R
import com.example.educa1.databinding.ActivityGerenciarProfessoresBinding
import com.example.educa1.models.Professor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GerenciarProfessoresActivity : BaseActivity() {

    private lateinit var binding: ActivityGerenciarProfessoresBinding
    private val listaDeProfessores = mutableListOf<Professor>()

    companion object {
        const val PREFS_NAME = "DirecaoPrefs"
        const val KEY_PROFESSORES = "key_professores"
    }

    private val detalhesProfessorResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Este bloco é executado quando a DetalhesProfessorActivity retorna.
        if (result.resultCode == RESULT_OK) {
            // Passo 1: Extrai o objeto Professor atualizado que foi devolvido.
            val professorAtualizado = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra("PROFESSOR_ATUALIZADO", Professor::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra("PROFESSOR_ATUALIZADO")
            }

            // Passo 2: Garante que o objeto não é nulo.
            professorAtualizado?.let { profAtualizado ->
                // Passo 3: Encontra a posição (o índice) do professor antigo na nossa lista,
                // usando o ID para garantir que estamos atualizando a pessoa certa.
                val index = listaDeProfessores.indexOfFirst { p -> p.id == profAtualizado.id }

                // Passo 4: Se encontrou o professor na lista...
                if (index != -1) {
                    // Substitui o objeto antigo pelo novo na lista de dados.
                    listaDeProfessores[index] = profAtualizado

                    // Salva a lista inteira e atualizada no SharedPreferences.
                    salvarProfessores()

                    // Recarrega a UI para refletir todas as mudanças.
                    // Isso é mais seguro do que tentar atualizar apenas um item.
                    carregarProfessores()

                    Toast.makeText(this, getString(R.string.professor_atualizado, profAtualizado.nome), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val gradeProfessorResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Este bloco é executado quando a GradeProfessorActivity retorna.
        // Recarrega os professores para garantir que temos os dados mais atualizados
        carregarProfessores()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGerenciarProfessoresBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura a Toolbar
        setSupportActionBar(binding.toolbarGerenciarProfessores)
        supportActionBar?.title = getString(R.string.gerenciar_professores)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Adiciona o botão "Voltar"
        binding.toolbarGerenciarProfessores.setNavigationIcon(R.drawable.ic_arrow_back_white)

        carregarProfessores()

        binding.fabAddProfessor.setOnClickListener {
            mostrarDialogoNovoProfessor()
        }
    }

    // Adiciona a função para lidar com o clique no botão "Voltar" da Toolbar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // --- Funções de Lógica e UI ---

    private fun criarVisualBotao(professor: Professor) {
        val novoBotao = CustomButtonView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val marginBottom = resources.getDimensionPixelSize(R.dimen.margem_inferior_botao_novo)
                setMargins(0, 0, 0, marginBottom)
            }
            text = professor.nome

            // Ação de clique principal: vai para os detalhes do professor
            setOnClickListener {
                val intent = Intent(this@GerenciarProfessoresActivity, GradeProfessorActivity::class.java).apply {
                    putExtra("PROFESSOR_SELECIONADO", professor)
                }
                gradeProfessorResultLauncher.launch(intent)
            }

            // Ação do ícone de editar
            setOnEditClickListener { mostrarDialogoEdicao(this, professor) }

            // Ação do ícone de deletar
            setOnDeleteClickListener { mostrarDialogoDelecao(this, professor) }
        }
        binding.lnaProfessores.addView(novoBotao)
    }

    private fun adicionarNovoProfessor(nomeProfessor: String) {
        val novoProfessor = Professor(
            id = System.currentTimeMillis(),
            nome = nomeProfessor
        )
        listaDeProfessores.add(novoProfessor)
        criarVisualBotao(novoProfessor) // Cria a representação visual
        salvarProfessores()
        Toast.makeText(this, getString(R.string.professor_adicionado, nomeProfessor), Toast.LENGTH_SHORT).show()
    }

    // --- Funções de Diálogo ---

    private fun mostrarDialogoNovoProfessor() {
        val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        builder.setTitle(getString(R.string.novo_professor))
        val input = EditText(builder.context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            hint = getString(R.string.nome_completo_professor)
        }
        builder.setView(input)

        builder.setPositiveButton(getString(R.string.btn_salvar)) { dialog, _ ->
            val nomeProfessor = input.text.toString().trim()
            if (nomeProfessor.isNotEmpty()) {
                adicionarNovoProfessor(nomeProfessor)
            } else {
                Toast.makeText(this, getString(R.string.nome_nao_pode_vazio), Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton(getString(R.string.btn_cancelar)) { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun mostrarDialogoEdicao(botaoParaEditar: CustomButtonView, professor: Professor) {
        val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        builder.setTitle(getString(R.string.editar_nome))
        val input = EditText(builder.context).apply {
            setText(professor.nome)
        }
        builder.setView(input)

        builder.setPositiveButton(getString(R.string.btn_salvar)) { _, _ ->
            val nomeNovo = input.text.toString().trim()
            if (nomeNovo.isNotEmpty()) {
                // Atualiza o nome no objeto de dados
                professor.nome = nomeNovo
                // Atualiza o texto no botão visual
                botaoParaEditar.text = nomeNovo
                // Salva a lista inteira com a alteração
                salvarProfessores()
            }
        }
        builder.setNegativeButton(getString(R.string.btn_cancelar), null)
        builder.show()
    }

    private fun mostrarDialogoDelecao(botaoParaDeletar: CustomButtonView, professor: Professor) {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle(getString(R.string.confirmar_exclusao))
            .setMessage(getString(R.string.tem_certeza_excluir_professor, professor.nome))
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton(getString(R.string.sim_excluir)) { _, _ ->
                // Remove o botão da tela
                binding.lnaProfessores.removeView(botaoParaDeletar)
                // Remove o professor da lista de dados
                listaDeProfessores.remove(professor)
                // Salva a lista atualizada
                salvarProfessores()
                Toast.makeText(this, getString(R.string.professor_excluido), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.btn_cancelar), null)
            .show()
    }

    // --- Funções de Persistência de Dados ---

    private fun salvarProfessores() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val jsonString = Gson().toJson(listaDeProfessores)
        editor.putString(KEY_PROFESSORES, jsonString)
        editor.apply()
    }

    private fun carregarProfessores() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val jsonString = sharedPreferences.getString(KEY_PROFESSORES, null)

        // Limpa a visualização atual antes de carregar
        binding.lnaProfessores.removeAllViews()
        listaDeProfessores.clear()

        if (jsonString != null) {
            val type = object : TypeToken<MutableList<Professor>>() {}.type
            val professoresSalvos: MutableList<Professor> = Gson().fromJson(jsonString, type)

            listaDeProfessores.addAll(professoresSalvos)
            // Para cada professor carregado, cria seu botão correspondente
            listaDeProfessores.forEach { professor ->
                criarVisualBotao(professor)
            }
        }
    }
}
