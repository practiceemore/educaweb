package com.example.educa1.activitys

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.educa1.R
import com.example.educa1.adapters.GradeHorariaAdapter
import com.example.educa1.databinding.ActivityGradeDisciplinaBinding
import com.example.educa1.models.CelulaHorario
import com.example.educa1.models.Disciplina
import com.example.educa1.models.Turma
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GradeDisciplinaActivity : BaseActivity() {

    private lateinit var binding: ActivityGradeDisciplinaBinding
    private var disciplinaSelecionada: Disciplina? = null
    private val gradeDaDisciplina = mutableListOf<CelulaHorario>()

    private val DIAS_NA_SEMANA = 5
    private val AULAS_POR_DIA = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGradeDisciplinaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarGradeDisciplina)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarGradeDisciplina.setNavigationIcon(R.drawable.ic_arrow_back_white)

        recuperarDisciplina()

        disciplinaSelecionada?.let {
            // Define o título da Toolbar com o nome da disciplina
            supportActionBar?.title = getString(R.string.horario_disciplina, it.nome)
            construirGradeDaDisciplina()
            configurarRecyclerView()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.grade_disciplina_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_detalhes_disciplina -> {
                abrirDetalhesDisciplina()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun abrirDetalhesDisciplina() {
        disciplinaSelecionada?.let { disciplina ->
            val intent = Intent(this, DetalhesDisciplinaActivity::class.java).apply {
                putExtra("DISCIPLINA_EXTRA", disciplina)
            }
            startActivity(intent)
        }
    }

    private fun recuperarDisciplina() {
        disciplinaSelecionada = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("DISCIPLINA_SELECIONADA", Disciplina::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("DISCIPLINA_SELECIONADA")
        }
    }

    private fun construirGradeDaDisciplina() {
        // 1. Cria uma grade vazia
        for (i in 0 until (DIAS_NA_SEMANA * AULAS_POR_DIA)) {
            gradeDaDisciplina.add(CelulaHorario(id = i.toString()))
        }

        // 2. Carrega a lista de todas as turmas
        val prefs = getSharedPreferences(GerenciarTurmasActivity.PREFS_NAME, MODE_PRIVATE)
        val jsonTurmas = prefs.getString(GerenciarTurmasActivity.KEY_TURMAS, null)
        if (jsonTurmas == null) return // Não há turmas, não há o que fazer

        val typeTurmas = object : TypeToken<List<Turma>>() {}.type
        val todasAsTurmas: List<Turma> = Gson().fromJson(jsonTurmas, typeTurmas)

        // 3. Itera sobre cada turma, carrega sua grade e preenche a grade da disciplina
        val prefsGrades = getSharedPreferences("GradesHorariasPrefs", MODE_PRIVATE)
        val typeGrade = object : TypeToken<List<CelulaHorario>>() {}.type

        todasAsTurmas.forEach { turma ->
            val jsonGradeDaTurma = prefsGrades.getString("grade_${turma.id}", null)
            if (jsonGradeDaTurma != null) {
                val gradeDaTurma: List<CelulaHorario> = Gson().fromJson(jsonGradeDaTurma, typeGrade)

                gradeDaTurma.forEachIndexed { index, celulaDaTurma ->
                    // Se a célula pertence à nossa disciplina, copia para a grade dela
                    if (celulaDaTurma.disciplina?.id == disciplinaSelecionada?.id) {
                        if (index < gradeDaDisciplina.size) {
                            gradeDaDisciplina[index] = celulaDaTurma
                        }
                    }
                }
            }
        }
    }

    private fun configurarRecyclerView() {
        val adapter = GradeHorariaAdapter(
            listaDeCelulas = gradeDaDisciplina,
            onItemClicked = { _, _ ->
                // O clique na grade da disciplina não faz nada por enquanto
            },
            isDisciplinaView = true // Flag para mostrar professor, turma e sala
        )
        binding.rvGradeDisciplina.layoutManager = GridLayoutManager(this, DIAS_NA_SEMANA)
        binding.rvGradeDisciplina.adapter = adapter
    }
} 