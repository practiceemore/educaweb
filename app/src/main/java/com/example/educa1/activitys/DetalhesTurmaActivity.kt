package com.example.educa1.activitys

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.educa1.R
import com.example.educa1.adapters.RequisitoAdapter
import com.example.educa1.databinding.ActivityDetalhesTurmaBinding
import com.example.educa1.models.Disciplina
import com.example.educa1.models.RequisitoDisciplina
import com.example.educa1.models.Turma
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DetalhesTurmaActivity : BaseActivity() {

    private lateinit var binding: ActivityDetalhesTurmaBinding
    private var turma: Turma? = null
    private val listaDeRequisitos = mutableListOf<RequisitoDisciplina>()
    private lateinit var adapter: RequisitoAdapter

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalhesTurmaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura a Toolbar
        setSupportActionBar(binding.toolbarDetalhesTurma)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Adiciona o botão "Voltar"
        binding.toolbarDetalhesTurma.setNavigationIcon(R.drawable.ic_arrow_back_white)

        recuperarTurma()

        turma?.let {
            // Define o nome da turma como título da Toolbar
            supportActionBar?.title = getString(R.string.requisitos_turma, it.nome)
            carregarRequisitos()
            configurarRecyclerView()
        } ?: run {
            Toast.makeText(this, getString(R.string.erro_carregar_turma), Toast.LENGTH_SHORT).show()
            finish()
        }

        // Botão "Grade Horária" removido - agora usa apenas a seta voltar da Toolbar
    }

    // Adiciona a função para lidar com o clique no botão "Voltar" da Toolbar
    override fun onSupportNavigateUp(): Boolean {
        // Salva os requisitos antes de voltar
        salvarRequisitos()
        
        // Volta naturalmente para a activity anterior
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onPause() {
        super.onPause()
        // Salva os requisitos sempre que o usuário sai da tela
        salvarRequisitos()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun recuperarTurma() {
        turma =
            intent.getParcelableExtra("TURMA_EXTRA", Turma::class.java)
    }

    private fun configurarRecyclerView() {
        adapter = RequisitoAdapter(listaDeRequisitos) { requisitoAlterado ->
            // O onPause já vai salvar, mas podemos adicionar lógica extra aqui se necessário
        }
        binding.rvRequisitosDisciplinas.layoutManager = LinearLayoutManager(this)
        binding.rvRequisitosDisciplinas.adapter = adapter
    }

    private fun carregarRequisitos() {
        // 1. Carrega todas as disciplinas cadastradas na escola
        val prefsDisciplinas = getSharedPreferences(GerenciarDisciplinasActivity.PREFS_NAME, MODE_PRIVATE)
        val jsonDisciplinas = prefsDisciplinas.getString(GerenciarDisciplinasActivity.KEY_DISCIPLINAS, null)
        val todasDisciplinas: List<Disciplina> = if (jsonDisciplinas != null) {
            val type = object : TypeToken<List<Disciplina>>() {}.type
            Gson().fromJson(jsonDisciplinas, type)
        } else {
            emptyList()
        }

        // 2. Carrega os requisitos já salvos para ESTA turma
        val prefsRequisitos = getSharedPreferences("RequisitosTurmasPrefs", MODE_PRIVATE)
        val jsonRequisitos = prefsRequisitos.getString("requisitos_${turma?.id}", null)
        val requisitosSalvos: List<RequisitoDisciplina> = if (jsonRequisitos != null) {
            val type = object : TypeToken<List<RequisitoDisciplina>>() {}.type
            Gson().fromJson(jsonRequisitos, type)
        } else {
            emptyList()
        }

        // 3. Monta a lista final, garantindo que todas as disciplinas apareçam
        listaDeRequisitos.clear()
        todasDisciplinas.forEach { disciplina ->
            val requisitoExistente = requisitosSalvos.find { it.nomeDisciplina == disciplina.nome }
            if (requisitoExistente != null) {
                listaDeRequisitos.add(requisitoExistente)
            } else {
                listaDeRequisitos.add(RequisitoDisciplina(disciplina.nome))
            }
        }
    }

    private fun salvarRequisitos() {
        turma?.let {
            val prefs = getSharedPreferences("RequisitosTurmasPrefs", MODE_PRIVATE).edit()
            val json = Gson().toJson(listaDeRequisitos)
            prefs.putString("requisitos_${it.id}", json)
            prefs.apply()
            // Opcional: Toast de feedback
            // Toast.makeText(this, "Requisitos salvos.", Toast.LENGTH_SHORT).show()
        }
    }
}