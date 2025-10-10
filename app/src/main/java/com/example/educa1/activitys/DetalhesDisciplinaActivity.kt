package com.example.educa1.activitys

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.educa1.R
import com.example.educa1.databinding.ActivityDetalhesDisciplinaBinding
import com.example.educa1.models.Disciplina
import com.example.educa1.models.Professor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DetalhesDisciplinaActivity : BaseActivity() {

    private lateinit var binding: ActivityDetalhesDisciplinaBinding
    private var disciplina: Disciplina? = null

    companion object {
        private const val PREFS_ANOTACOES = "AnotacoesDisciplinasPrefs"
        private const val KEY_ANOTACOES_PREFIX = "anotacoes_disciplina_"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalhesDisciplinaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recuperarDisciplina()

        disciplina?.let {
                    setSupportActionBar(binding.toolbarDetalhesDisciplina)
        supportActionBar?.title = it.nome
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarDetalhesDisciplina.setNavigationIcon(R.drawable.ic_arrow_back_white)
        } ?: run {
            Toast.makeText(this, getString(R.string.erro_carregar_disciplina), Toast.LENGTH_SHORT).show()
            finish()
        }

        // Carregar anotações salvas
        carregarAnotacoes()

        // Configurar listener para salvar automaticamente
        configurarListenerAnotacoes()

        // Carregar e exibir professores da disciplina
        carregarProfessoresDaDisciplina()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun recuperarDisciplina() {
        disciplina = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("DISCIPLINA_EXTRA", Disciplina::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("DISCIPLINA_EXTRA")
        }
    }

    private fun carregarAnotacoes() {
        disciplina?.let { disciplina ->
            val prefs = getSharedPreferences(PREFS_ANOTACOES, MODE_PRIVATE)
            val anotacoes = prefs.getString("${KEY_ANOTACOES_PREFIX}${disciplina.id}", "")
            binding.etAnotacoesDisciplina.setText(anotacoes)
        }
    }

    private fun salvarAnotacoes() {
        disciplina?.let { disciplina ->
            val anotacoes = binding.etAnotacoesDisciplina.text.toString()
            val prefs = getSharedPreferences(PREFS_ANOTACOES, MODE_PRIVATE).edit()
            prefs.putString("${KEY_ANOTACOES_PREFIX}${disciplina.id}", anotacoes)
            prefs.apply()
        }
    }

    private fun configurarListenerAnotacoes() {
        binding.etAnotacoesDisciplina.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                // Salvar automaticamente após mudanças
                salvarAnotacoes()
            }
        })
    }

    private fun carregarProfessoresDaDisciplina() {
        disciplina?.let { disciplina ->
            // Carregar todos os professores
            val prefs = getSharedPreferences(GerenciarProfessoresActivity.PREFS_NAME, MODE_PRIVATE)
            val json = prefs.getString(GerenciarProfessoresActivity.KEY_PROFESSORES, "[]")
            val todosProfessores: List<Professor> = Gson().fromJson(json, Array<Professor>::class.java).toList()

            // Filtrar professores que lecionam esta disciplina
            val professoresDaDisciplina = todosProfessores.filter { professor ->
                professor.disciplinas.contains(disciplina.nome)
            }

            // Exibir lista de professores
            if (professoresDaDisciplina.isNotEmpty()) {
                val professoresText = StringBuilder()
                professoresDaDisciplina.forEach { professor ->
                    professoresText.append("• ${professor.nome}\n")
                }
                binding.tvProfessoresDisciplina.text = professoresText.toString()
            } else {
                binding.tvProfessoresDisciplina.text = getString(R.string.nenhum_professor_encontrado)
            }
        }
    }
} 