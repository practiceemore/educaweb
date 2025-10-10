package com.example.educa1.activitys

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.educa1.R
import com.example.educa1.adapters.GradeHorariaAdapter
import com.example.educa1.databinding.ActivityGradeAlunoBinding
import com.example.educa1.models.CelulaHorario
import com.example.educa1.models.Turma
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class GradeAlunoActivity : BaseActivity() {

    private lateinit var binding: ActivityGradeAlunoBinding
    private var turmaSelecionada: Turma? = null
    private val listaDeCelulas = mutableListOf<CelulaHorario>()

    private val DIAS_NA_SEMANA = 5
    private val AULAS_POR_DIA = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGradeAlunoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarGradeHoraria) // Configura a Toolbar

        recuperarTurma()

        turmaSelecionada?.let {
            supportActionBar?.title = getString(R.string.horario_turma, it.nome)
            carregarGradeDaTurma()
            configurarRecyclerView()
        }
    }

    // Lógica para adicionar o menu de compartilhar (opcional, mas recomendado)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.grade_horaria_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {
                // Reutiliza a mesma lógica de gerar PDF da GradeHorariaActivity
                gerarECompartilharPdf()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun recuperarTurma() {
        turmaSelecionada = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("TURMA_SELECIONADA", Turma::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("TURMA_SELECIONADA")
        }
    }

    private fun carregarGradeDaTurma() {
        val prefs = getSharedPreferences("GradesHorariasPrefs", MODE_PRIVATE)
        val json = prefs.getString("grade_${turmaSelecionada?.id}", null)

        if (json != null) {
            val type = object : TypeToken<List<CelulaHorario>>() {}.type
            val gradeSalva: List<CelulaHorario> = Gson().fromJson(json, type)
            listaDeCelulas.clear()
            listaDeCelulas.addAll(gradeSalva)
        } else {
            // Se não houver grade salva, gera uma vazia para exibir
            for (i in 0 until (DIAS_NA_SEMANA * AULAS_POR_DIA)) {
                listaDeCelulas.add(CelulaHorario(id = i.toString()))
            }
            Toast.makeText(this, "Horário ainda não disponível para esta turma.", Toast.LENGTH_LONG).show()
        }
    }

    private fun configurarRecyclerView() {
        // O listener de clique é vazio, pois esta é uma tela de visualização
        val adapter = GradeHorariaAdapter(
            listaDeCelulas = listaDeCelulas,
            onItemClicked = { _, _ -> },
            isProfessorView = false // Visão do aluno (mostra professor)
        )
        binding.rvGradeHoraria.layoutManager = GridLayoutManager(this, DIAS_NA_SEMANA)
        binding.rvGradeHoraria.adapter = adapter
    }

    // A função gerarECompartilharPdf() pode ser copiada diretamente da GradeHorariaActivity
    private fun gerarECompartilharPdf() {
        // ... cole aqui a mesma função da GradeHorariaActivity ...
    }
}