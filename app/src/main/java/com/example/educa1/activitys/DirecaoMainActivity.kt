
package com.example.educa1.activitys

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.educa1.R
import com.example.educa1.databinding.ActivityDirecaoMainBinding
import android.view.Menu
import android.view.MenuItem
import com.example.educa1.utils.ConfiguracaoGradeManager
import com.example.educa1.models.ConfiguracaoGrade
import android.app.AlertDialog
import android.widget.Toast
import android.util.Log
import android.widget.NumberPicker
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Button
import android.widget.TextView
import android.widget.ArrayAdapter
import android.widget.AdapterView
//import android.content.ContextCompat
import android.graphics.PorterDuff
import androidx.core.content.ContextCompat

class DirecaoMainActivity : BaseActivity() {

    private lateinit var binding: ActivityDirecaoMainBinding
    private lateinit var configuracaoManager: ConfiguracaoGradeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDirecaoMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar toolbar corretamente
        setSupportActionBar(binding.toolbarDirecao)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.painel_direcao)
        binding.toolbarDirecao.setNavigationIcon(R.drawable.ic_arrow_back_white)

        configuracaoManager = ConfiguracaoGradeManager(this)

        binding.btnGerenciarDisciplinas.setOnClickListener {
            val intent = Intent(this, GerenciarDisciplinasActivity::class.java)
            startActivity(intent)
        }

        binding.btnGerenciarProfessores.setOnClickListener {
            val intent = Intent(this, GerenciarProfessoresActivity::class.java)
            startActivity(intent)
        }

        binding.btnGerenciarTurmas.setOnClickListener {
            val intent = Intent(this, GerenciarTurmasActivity::class.java)
            startActivity(intent)
        }

        binding.btnGerenciarSalas.setOnClickListener {
            val intent = Intent(this, GerenciarSalasActivity::class.java)
            startActivity(intent)
        }

        binding.btnRelatorios.setOnClickListener {
            val intent = Intent(this, ConsultorIAActivity::class.java)
            startActivity(intent)
        }

        // REMOVIDO: btnConfiguracaoGrade click listener (agora está no menu)
    }

    private fun mostrarConfiguracaoGrade() {
        Log.d("DirecaoMainActivity", " mostrarConfiguracaoGrade() chamado")
        
        val configuracaoAtual = configuracaoManager.carregarConfiguracao()
        Log.d("DirecaoMainActivity", "📊 Configuração atual: ${configuracaoAtual.aulasPorDia} aulas")
        
        // Criar um Spinner
        val spinner = Spinner(this)
        val opcoes = arrayOf("3 aulas", "4 aulas", "5 aulas", "6 aulas", "7 aulas", "8 aulas")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, opcoes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(configuracaoAtual.aulasPorDia - 3)
        
        // Layout para o spinner
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(32, 32, 32, 32)
        layout.addView(spinner)
        
        val dialog = AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle(getString(R.string.configuracao_grade)) // Usar string multilíngue
//            .setMessage(getString(R.string.configuracao_grade_descricao)) // Usar string multilíngue
            .setView(layout)
            .setCancelable(true)
            .create()
        
        // Salvar automaticamente quando o valor for alterado
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val novasAulas = position + 3 // 0=3, 1=4, 2=5, etc.
                Log.d("DirecaoMainActivity", "🔄 Novas aulas selecionadas: $novasAulas")
                
                if (novasAulas != configuracaoAtual.aulasPorDia) {
                    Log.d("DirecaoMainActivity", "⚠️ Configuração será alterada de ${configuracaoAtual.aulasPorDia} para $novasAulas")
                    aplicarNovaConfiguracao(novasAulas)
                    // Fechar o dialog automaticamente após salvar
                    dialog.dismiss()
                } else {
                    Log.d("DirecaoMainActivity", "ℹ️ Configuração já está em $novasAulas aulas")
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Não fazer nada
            }
        }
        
        dialog.show()
        
        Log.d("DirecaoMainActivity", "�� Dialog com Spinner exibido")
    }

    private fun aplicarNovaConfiguracao(novasAulas: Int) {
        Log.d("DirecaoMainActivity", "💾 Aplicando nova configuração: $novasAulas aulas")
        
        val novaConfiguracao = ConfiguracaoGrade(aulasPorDia = novasAulas)
        configuracaoManager.salvarConfiguracao(novaConfiguracao)
        
        Log.d("DirecaoMainActivity", "✅ Configuração salva com sucesso")
        // Usar string multilíngue com formatação
        val mensagem = getString(R.string.configuracao_grade_salva, novasAulas)
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()
    }

    // NOVO: Inflar o menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_direcao_main, menu)
        return true
    }

    // NOVO: Lidar com cliques no menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d("DirecaoMainActivity", "🔧 onOptionsItemSelected chamado: ${item.itemId}")
        
        return when (item.itemId) {
            R.id.action_configuracao_grade -> {
                Log.d("DirecaoMainActivity", "⚙️ Botão configuração grade clicado")
                mostrarConfiguracaoGrade()
                true
            }
            else -> {
                Log.d("DirecaoMainActivity", "❓ Item não reconhecido: ${item.itemId}")
                super.onOptionsItemSelected(item)
            }
        }
    }

    // Função para lidar com o clique no botão "Voltar" da Toolbar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}