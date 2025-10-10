package com.example.educa1.activitys

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.educa1.CustomButtonView
import com.example.educa1.R
import com.example.educa1.databinding.ActivityGerenciarTurmasBinding
import com.example.educa1.models.Turma
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.util.Log

class GerenciarTurmasActivity : BaseActivity() {

    private lateinit var binding: ActivityGerenciarTurmasBinding
    private val listaDeTurmas = mutableListOf<Turma>()

    companion object {
        const val PREFS_NAME = "DirecaoPrefs"
        const val KEY_TURMAS = "key_turmas"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGerenciarTurmasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura a Toolbar
        setSupportActionBar(binding.toolbarGerenciarTurmas)
        supportActionBar?.title = getString(R.string.gerenciar_turmas)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Adiciona o botão "Voltar"
        binding.toolbarGerenciarTurmas.setNavigationIcon(R.drawable.ic_arrow_back_white)

        carregarTurmas()

        binding.fabAddTurma.setOnClickListener {
            mostrarDialogoNovaTurma()
        }
    }

    // Adiciona a função para lidar com o clique no botão "Voltar" da Toolbar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // Em GerenciarTurmasActivity.kt -> criarVisualBotao()
    private fun criarVisualBotao(turma: Turma) {
        val novoBotao = CustomButtonView(this).apply {
            // A CORREÇÃO ESTÁ AQUI: Definimos os parâmetros de layout com margem
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                // Pega o valor da margem que definimos nos recursos
                val marginBottom = resources.getDimensionPixelSize(R.dimen.margem_inferior_botao_novo)
                // Aplica a margem apenas na parte inferior do botão
                setMargins(0, 0, 0, marginBottom)
            }

            // O resto da configuração do botão permanece o mesmo
            text = turma.nome

            setOnClickListener {
                // Abrir diretamente a Grade Horária
                val intent = Intent(this@GerenciarTurmasActivity, GradeHorariaActivity::class.java).apply {
                    putExtra("TURMA_SELECIONADA", turma)
                }
                startActivity(intent)
            }

            setOnEditClickListener { mostrarDialogoEdicao(this, turma) }
            setOnDeleteClickListener { mostrarDialogoDelecao(this, turma) }
        }
        binding.lnaTurmas.addView(novoBotao)
    }

    private fun adicionarNovaTurma(nomeTurma: String) {
        val novaTurma = Turma(System.currentTimeMillis(), nomeTurma)
        listaDeTurmas.add(novaTurma)
        criarVisualBotao(novaTurma)
        salvarTurmas()
        Toast.makeText(this, getString(R.string.turma_adicionada, nomeTurma), Toast.LENGTH_SHORT).show()
    }

    private fun mostrarDialogoNovaTurma() {
        val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        builder.setTitle(getString(R.string.nova_turma))
        val input = EditText(builder.context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            hint = getString(R.string.exemplo_turma)
        }
        builder.setView(input)

        builder.setPositiveButton(getString(R.string.btn_salvar)) { dialog, _ ->
            val nomeTurma = input.text.toString().trim()
            if (nomeTurma.isNotEmpty()) {
                adicionarNovaTurma(nomeTurma)
            } else {
                Toast.makeText(this, getString(R.string.nome_nao_pode_vazio), Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton(getString(R.string.btn_cancelar)) { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun mostrarDialogoEdicao(botaoParaEditar: CustomButtonView, turma: Turma) {
        val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        builder.setTitle(getString(R.string.editar_nome_turma))
        val input = EditText(builder.context).apply { setText(turma.nome) }
        builder.setView(input)

        builder.setPositiveButton(getString(R.string.btn_salvar)) { _, _ ->
            val nomeNovo = input.text.toString().trim()
            if (nomeNovo.isNotEmpty()) {
                turma.nome = nomeNovo
                botaoParaEditar.text = nomeNovo
                salvarTurmas()
            }
        }
        builder.setNegativeButton(getString(R.string.btn_cancelar), null)
        builder.show()
    }

    private fun mostrarDialogoDelecao(botaoParaDeletar: CustomButtonView, turma: Turma) {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle(getString(R.string.confirmar_exclusao))
            .setMessage(getString(R.string.tem_certeza_excluir_turma, turma.nome))
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton(getString(R.string.sim_excluir)) { _, _ ->
                binding.lnaTurmas.removeView(botaoParaDeletar)
                listaDeTurmas.remove(turma)
                
                // NOVO: Limpar a grade horária da turma deletada
                limparGradeDaTurma(turma)
                
                salvarTurmas()
                Toast.makeText(this, getString(R.string.turma_excluida), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.btn_cancelar), null)
            .show()
    }

    // NOVA FUNÇÃO: Limpar grade da turma deletada
    private fun limparGradeDaTurma(turma: Turma) {
        val prefs = getSharedPreferences("GradesHorariasPrefs", MODE_PRIVATE).edit()
        
        // Limpar tanto pelo ID quanto pelo nome (para compatibilidade)
        prefs.remove("grade_${turma.id}")
        prefs.remove("grade_${turma.nome}")
        
        // Também limpar requisitos se existirem
        val prefsRequisitos = getSharedPreferences("RequisitosTurmasPrefs", MODE_PRIVATE).edit()
        prefsRequisitos.remove("requisitos_${turma.id}")
        prefsRequisitos.remove("requisitos_${turma.nome}")
        
        prefs.apply()
        prefsRequisitos.apply()
        
        Log.d("GerenciarTurmasActivity", "Grade e requisitos da turma '${turma.nome}' (ID: ${turma.id}) foram limpos")
    }

    private fun salvarTurmas() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val jsonString = Gson().toJson(listaDeTurmas)
        editor.putString(KEY_TURMAS, jsonString)
        editor.apply()
    }

    private fun carregarTurmas() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val jsonString = sharedPreferences.getString(KEY_TURMAS, null)

        binding.lnaTurmas.removeAllViews()
        listaDeTurmas.clear()

        if (jsonString != null) {
            val type = object : TypeToken<MutableList<Turma>>() {}.type
            val turmasSalvas: MutableList<Turma> = Gson().fromJson(jsonString, type)
            listaDeTurmas.addAll(turmasSalvas)
            listaDeTurmas.forEach { turma -> criarVisualBotao(turma) }
        }
    }
}
