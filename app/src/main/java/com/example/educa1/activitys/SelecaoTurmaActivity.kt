package com.example.educa1.activitys

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.educa1.CustomButtonView
import com.example.educa1.R
import com.example.educa1.databinding.ActivitySelecaoTurmaBinding
import com.example.educa1.models.Turma
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SelecaoTurmaActivity : BaseActivity() {

    private lateinit var binding: ActivitySelecaoTurmaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelecaoTurmaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura a Toolbar
        setSupportActionBar(binding.toolbarSelecaoTurma)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Adiciona o botão "Voltar"
        binding.toolbarSelecaoTurma.setNavigationIcon(R.drawable.ic_arrow_back_white) // Ícone branco
        supportActionBar?.title = getString(R.string.qual_sua_turma)

        carregarEExibirTurmas()
    }

    private fun carregarEExibirTurmas() {
        val sharedPreferences = getSharedPreferences(GerenciarTurmasActivity.PREFS_NAME, MODE_PRIVATE)
        val jsonString = sharedPreferences.getString(GerenciarTurmasActivity.KEY_TURMAS, null)

        if (jsonString.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.nenhuma_turma_cadastrada), Toast.LENGTH_LONG).show()
            return
        }

        val type = object : TypeToken<List<Turma>>() {}.type
        val turmas: List<Turma> = Gson().fromJson(jsonString, type)

        // Ordena as turmas por nome antes de exibi-las
        turmas.sortedBy { it.nome }.forEach { turma ->
            criarBotaoParaTurma(turma)
        }
    }

    private fun criarBotaoParaTurma(turma: Turma) {
        val novoBotao = CustomButtonView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val marginBottom = resources.getDimensionPixelSize(R.dimen.margem_inferior_botao_novo)
                setMargins(0, 0, 0, marginBottom)
            }
            text = turma.nome

            // Esconde os ícones para uma interface de seleção limpa
            setEditIconVisibility(false)
            setDeleteIconVisibility(false)
            setOnEditClickListener {}
            setOnDeleteClickListener {}

            setOnClickListener {
                val intent = Intent(this@SelecaoTurmaActivity, GradeAlunoActivity::class.java).apply {
                    putExtra("TURMA_SELECIONADA", turma)
                }
                startActivity(intent)
            }
        }
        binding.lnaListaTurmasSelecao.addView(novoBotao)
    }

    // Função para lidar com o clique no botão "Voltar" da Toolbar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}