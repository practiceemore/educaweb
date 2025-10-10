package com.example.educa1.activitys

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.educa1.CustomButtonView
import com.example.educa1.R
import com.example.educa1.databinding.ActivitySelecaoProfessorBinding
import com.example.educa1.models.Professor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SelecaoProfessorActivity : BaseActivity() {

    private lateinit var binding: ActivitySelecaoProfessorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelecaoProfessorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura a Toolbar
        setSupportActionBar(binding.toolbarSelecaoProfessor)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Adiciona o botão "Voltar"
        binding.toolbarSelecaoProfessor.setNavigationIcon(R.drawable.ic_arrow_back_white) // Ícone branco
        supportActionBar?.title = getString(R.string.quem_e_voce)

        carregarEExibirProfessores()
    }

    private fun carregarEExibirProfessores() {
        val sharedPreferences = getSharedPreferences(GerenciarProfessoresActivity.PREFS_NAME, MODE_PRIVATE)
        val jsonString = sharedPreferences.getString(GerenciarProfessoresActivity.KEY_PROFESSORES, null)

        if (jsonString.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.nenhum_professor_cadastrado), Toast.LENGTH_LONG).show()
            return
        }

        val type = object : TypeToken<List<Professor>>() {}.type
        val professores: List<Professor> = Gson().fromJson(jsonString, type)

        // Ordena os professores por nome antes de exibi-los
        professores.sortedBy { it.nome }.forEach { professor ->
            criarBotaoParaProfessor(professor)
        }
    }

    private fun criarBotaoParaProfessor(professor: Professor) {
        val novoBotao = CustomButtonView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val marginBottom = resources.getDimensionPixelSize(R.dimen.margem_inferior_botao_novo)
                setMargins(0, 0, 0, marginBottom)
            }
            text = professor.nome

            // Esconde os ícones, pois esta é apenas uma tela de seleção
            setEditIconVisibility(false)
            setDeleteIconVisibility(false)

            // Desativa os listeners dos ícones por segurança
            setOnEditClickListener {}
            setOnDeleteClickListener {}

            // Ação principal de clique: navegar para a grade do professor
            setOnClickListener {
                val intent = Intent(this@SelecaoProfessorActivity, GradeProfessorActivity::class.java).apply {
                    putExtra("PROFESSOR_SELECIONADO", professor)
                }
                startActivity(intent)
            }
        }
        binding.lnaListaProfessoresSelecao.addView(novoBotao)
    }

    // Função para lidar com o clique no botão "Voltar" da Toolbar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}