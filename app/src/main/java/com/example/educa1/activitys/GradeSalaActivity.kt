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
import com.example.educa1.databinding.ActivityGradeSalaBinding
import com.example.educa1.models.CelulaHorario
import com.example.educa1.models.Sala
import com.example.educa1.models.Turma
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GradeSalaActivity : BaseActivity() {

    private lateinit var binding: ActivityGradeSalaBinding
    private var salaSelecionada: Sala? = null
    private val gradeDaSala = mutableListOf<CelulaHorario>()

    private val DIAS_NA_SEMANA = 5
    private val AULAS_POR_DIA = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGradeSalaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarGradeHoraria) // Reutiliza o ID da toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarGradeHoraria.setNavigationIcon(R.drawable.ic_arrow_back_white)

        recuperarSala()

        salaSelecionada?.let {
            supportActionBar?.title = getString(R.string.horario_sala, it.nome)
            construirGradeDaSala()
            configurarRecyclerView()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.grade_sala_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_detalhes_sala -> {
                abrirDetalhesSala()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun abrirDetalhesSala() {
        salaSelecionada?.let { sala ->
            val intent = Intent(this, DetalhesSalaActivity::class.java).apply {
                putExtra("SALA_EXTRA", sala)
            }
            startActivity(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun recuperarSala() {
        salaSelecionada = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("SALA_SELECIONADA", Sala::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("SALA_SELECIONADA")
        }
    }

    private fun construirGradeDaSala() {
        // 1. Cria uma grade vazia para a sala
        for (i in 0 until (DIAS_NA_SEMANA * AULAS_POR_DIA)) {
            gradeDaSala.add(CelulaHorario(id = i.toString()))
        }

        // 2. Carrega a lista de todas as turmas cadastradas
        val todasAsTurmas = carregarListaDeTurmas()
        if (todasAsTurmas.isEmpty()) return

        // 3. Itera sobre cada turma, carrega sua grade e preenche a grade da sala
        val prefsGrades = getSharedPreferences("GradesHorariasPrefs", MODE_PRIVATE)
        val typeGrade = object : TypeToken<List<CelulaHorario>>() {}.type
        val gson = Gson()

        todasAsTurmas.forEach { turma ->
            val jsonGradeDaTurma = prefsGrades.getString("grade_${turma.id}", null)
            if (jsonGradeDaTurma != null) {
                val gradeDaTurma: List<CelulaHorario> = gson.fromJson(jsonGradeDaTurma, typeGrade)

                gradeDaTurma.forEachIndexed { index, celulaDaTurma ->
                    // Se a célula na grade da turma usa a sala que estamos visualizando...
                    if (celulaDaTurma.sala?.id == salaSelecionada?.id) {
                        // ...copia a informação para a posição correspondente na grade da sala.
                        if (index < gradeDaSala.size) {
                            gradeDaSala[index] = celulaDaTurma
                        }
                    }
                }
            }
        }
    }

    private fun configurarRecyclerView() {
        // Reutilizamos o mesmo GradeHorariaAdapter, o clique não fará nada.
        val adapter = GradeHorariaAdapter(
            listaDeCelulas = gradeDaSala,
            onItemClicked = { _, _ -> },
            isProfessorView = false // Visão da sala (mostra professor)
        )
        binding.rvGradeHoraria.layoutManager = GridLayoutManager(this, DIAS_NA_SEMANA)
        binding.rvGradeHoraria.adapter = adapter
    }

    // Função auxiliar para carregar a lista de turmas
    private fun carregarListaDeTurmas(): List<Turma> {
        val prefs = getSharedPreferences(GerenciarTurmasActivity.PREFS_NAME, MODE_PRIVATE)
        val json = prefs.getString(GerenciarTurmasActivity.KEY_TURMAS, null)
        return if (json != null) {
            val type = object : TypeToken<List<Turma>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }
}