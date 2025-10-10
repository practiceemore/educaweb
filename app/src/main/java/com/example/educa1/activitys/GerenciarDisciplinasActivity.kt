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
import com.example.educa1.databinding.ActivityGerenciarDisciplinasBinding
import com.example.educa1.models.Disciplina
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GerenciarDisciplinasActivity : BaseActivity() {

    private lateinit var binding: ActivityGerenciarDisciplinasBinding
    private val listaDeDisciplinas = mutableListOf<Disciplina>()

    companion object {
        const val PREFS_NAME = "DirecaoPrefs"
        const val KEY_DISCIPLINAS = "key_disciplinas"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGerenciarDisciplinasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura a Toolbar
        setSupportActionBar(binding.toolbarGerenciarDisciplinas)
        supportActionBar?.title = getString(R.string.gerenciar_disciplinas)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Adiciona o botão "Voltar"
        binding.toolbarGerenciarDisciplinas.setNavigationIcon(R.drawable.ic_arrow_back_white)

        carregarDisciplinas()

        binding.fabAddDisciplina.setOnClickListener {
            mostrarDialogoNovaDisciplina()
        }
    }

    // Adiciona a função para lidar com o clique no botão "Voltar" da Toolbar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun criarVisualBotao(disciplina: Disciplina) {
        val novoBotao = CustomButtonView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val marginBottom = resources.getDimensionPixelSize(R.dimen.margem_inferior_botao_novo)
                setMargins(0, 0, 0, marginBottom)
            }
            text = disciplina.nome

            // Ação de clique principal: vai para a grade horária da disciplina
            setOnClickListener {
                val intent = Intent(this@GerenciarDisciplinasActivity, GradeDisciplinaActivity::class.java).apply {
                    putExtra("DISCIPLINA_SELECIONADA", disciplina)
                }
                startActivity(intent)
            }
            setOnEditClickListener { mostrarDialogoEdicao(this, disciplina) }
            setOnDeleteClickListener { mostrarDialogoDelecao(this, disciplina) }
        }
        binding.lnaDisciplinas.addView(novoBotao)
    }

    private fun adicionarNovaDisciplina(nomeDisciplina: String) {
        val novaDisciplina = Disciplina(System.currentTimeMillis(), nomeDisciplina)
        listaDeDisciplinas.add(novaDisciplina)
        criarVisualBotao(novaDisciplina)
        salvarDisciplinas()
        Toast.makeText(this, getString(R.string.disciplina_adicionada, nomeDisciplina), Toast.LENGTH_SHORT).show()
    }

    private fun mostrarDialogoNovaDisciplina() {
        val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        builder.setTitle(getString(R.string.nova_disciplina))
        val input = EditText(builder.context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            hint = getString(R.string.nome_disciplina)
        }
        builder.setView(input)

        builder.setPositiveButton(getString(R.string.btn_salvar)) { dialog, _ ->
            val nomeDisciplina = input.text.toString().trim()
            if (nomeDisciplina.isNotEmpty()) {
                adicionarNovaDisciplina(nomeDisciplina)
            } else {
                Toast.makeText(this, getString(R.string.nome_nao_pode_vazio), Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton(getString(R.string.btn_cancelar)) { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun mostrarDialogoEdicao(botaoParaEditar: CustomButtonView, disciplina: Disciplina) {
        val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        builder.setTitle(getString(R.string.editar_nome_disciplina))
        val input = EditText(builder.context).apply { setText(disciplina.nome) }
        builder.setView(input)

        builder.setPositiveButton(getString(R.string.btn_salvar)) { _, _ ->
            val nomeNovo = input.text.toString().trim()
            if (nomeNovo.isNotEmpty()) {
                disciplina.nome = nomeNovo
                botaoParaEditar.text = nomeNovo
                salvarDisciplinas()
            }
        }
        builder.setNegativeButton(getString(R.string.btn_cancelar), null)
        builder.show()
    }

    private fun mostrarDialogoDelecao(botaoParaDeletar: CustomButtonView, disciplina: Disciplina) {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle(getString(R.string.confirmar_exclusao))
            .setMessage(getString(R.string.tem_certeza_excluir_disciplina, disciplina.nome))
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton(getString(R.string.sim_excluir)) { _, _ ->
                binding.lnaDisciplinas.removeView(botaoParaDeletar)
                listaDeDisciplinas.remove(disciplina)
                salvarDisciplinas()
                Toast.makeText(this, getString(R.string.disciplina_excluida), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.btn_cancelar), null)
            .show()
    }

    private fun salvarDisciplinas() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val jsonString = Gson().toJson(listaDeDisciplinas)
        editor.putString(KEY_DISCIPLINAS, jsonString)
        editor.apply()
    }

    private fun carregarDisciplinas() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val jsonString = sharedPreferences.getString(KEY_DISCIPLINAS, null)

        binding.lnaDisciplinas.removeAllViews()
        listaDeDisciplinas.clear()

        if (jsonString != null) {
            val type = object : TypeToken<MutableList<Disciplina>>() {}.type
            val disciplinasSalvas: MutableList<Disciplina> = Gson().fromJson(jsonString, type)
            listaDeDisciplinas.addAll(disciplinasSalvas)
            listaDeDisciplinas.forEach { disciplina -> criarVisualBotao(disciplina) }
        }
    }
}
