package com.example.educa1.activitys

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.educa1.R
import com.example.educa1.adapters.GradeHorariaAdapter
import com.example.educa1.databinding.ActivityGradeProfessorBinding
import com.example.educa1.models.CelulaHorario
import com.example.educa1.models.Professor
import com.example.educa1.models.Turma
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GradeProfessorActivity : BaseActivity() {

    private lateinit var binding: ActivityGradeProfessorBinding
    private var professorSelecionado: Professor? = null
    private val gradeDoProfessor = mutableListOf<CelulaHorario>()

    private val DIAS_NA_SEMANA = 5
    private val AULAS_POR_DIA = 5

    private val detalhesProfessorResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val professorAtualizado = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra("PROFESSOR_ATUALIZADO", Professor::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra("PROFESSOR_ATUALIZADO")
            }

            professorAtualizado?.let { profAtualizado ->
                // Atualiza o professor selecionado com os dados atualizados
                professorSelecionado = profAtualizado
                
                // Reconstrói a grade do professor com os dados atualizados
                gradeDoProfessor.clear()
                construirGradeDoProfessor()
                
                // Atualiza o adapter para refletir as mudanças
                configurarRecyclerView()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGradeProfessorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarGradeHoraria)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarGradeHoraria.setNavigationIcon(R.drawable.ic_arrow_back_white)

        recuperarProfessor()

        professorSelecionado?.let {
            // Define o título da Toolbar com o nome do professor
            supportActionBar?.title = getString(R.string.horario_professor, it.nome)
            construirGradeDoProfessor()
            configurarRecyclerView()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.grade_professor_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_detalhes_professor -> {
                abrirDetalhesProfessor()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun abrirDetalhesProfessor() {
        professorSelecionado?.let { professor ->
            val intent = Intent(this, DetalhesProfessorActivity::class.java).apply {
                putExtra("PROFESSOR_EXTRA", professor)
            }
            detalhesProfessorResultLauncher.launch(intent)
        }
    }

    private fun recuperarProfessor() {
        professorSelecionado = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("PROFESSOR_SELECIONADO", Professor::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("PROFESSOR_SELECIONADO")
        }
    }

    private fun construirGradeDoProfessor() {
        // 1. Cria uma grade vazia
        for (i in 0 until (DIAS_NA_SEMANA * AULAS_POR_DIA)) {
            gradeDoProfessor.add(CelulaHorario(id = i.toString()))
        }

        // 2. Carrega a lista de todas as turmas
        val prefs = getSharedPreferences(GerenciarTurmasActivity.PREFS_NAME, MODE_PRIVATE)
        val jsonTurmas = prefs.getString(GerenciarTurmasActivity.KEY_TURMAS, null)
        if (jsonTurmas == null) return // Não há turmas, não há o que fazer

        val typeTurmas = object : TypeToken<List<Turma>>() {}.type
        val todasAsTurmas: List<Turma> = Gson().fromJson(jsonTurmas, typeTurmas)

        // 3. Itera sobre cada turma, carrega sua grade e preenche a grade do professor
        val prefsGrades = getSharedPreferences("GradesHorariasPrefs", MODE_PRIVATE)
        val typeGrade = object : TypeToken<List<CelulaHorario>>() {}.type

        todasAsTurmas.forEach { turma ->
            val jsonGradeDaTurma = prefsGrades.getString("grade_${turma.id}", null)
            if (jsonGradeDaTurma != null) {
                val gradeDaTurma: List<CelulaHorario> = Gson().fromJson(jsonGradeDaTurma, typeGrade)

                gradeDaTurma.forEachIndexed { index, celulaDaTurma ->
                    // Se a célula pertence ao nosso professor, copia para a grade dele
                    if (celulaDaTurma.professor?.id == professorSelecionado?.id) {
                        if (index < gradeDoProfessor.size) {
                            gradeDoProfessor[index] = celulaDaTurma
                        }
                    }
                }
            }
        }
    }

    private fun configurarRecyclerView() {
        val adapter = GradeHorariaAdapter(
            listaDeCelulas = gradeDoProfessor,
            onItemClicked = { _, _ ->
                // O clique na grade do professor não faz nada por enquanto
            },
            isProfessorView = true // Flag para mostrar turma em vez de professor
        )
        binding.rvGradeHoraria.layoutManager = GridLayoutManager(this, DIAS_NA_SEMANA)
        binding.rvGradeHoraria.adapter = adapter
    }
}