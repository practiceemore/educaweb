package com.example.educa1.activitys

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.educa1.R
import com.example.educa1.databinding.ActivityDetalhesSalaBinding
import com.example.educa1.models.Sala

class DetalhesSalaActivity : BaseActivity() {

    private lateinit var binding: ActivityDetalhesSalaBinding
    private var sala: Sala? = null

    companion object {
        private const val PREFS_ANOTACOES = "AnotacoesSalasPrefs"
        private const val KEY_ANOTACOES_PREFIX = "anotacoes_sala_"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalhesSalaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recuperarSala()

        sala?.let {
            setSupportActionBar(binding.toolbarDetalhesSala)
            supportActionBar?.title = it.nome
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            binding.toolbarDetalhesSala.setNavigationIcon(R.drawable.ic_arrow_back_white)
        } ?: run {
            Toast.makeText(this, getString(R.string.erro_carregar_sala), Toast.LENGTH_SHORT).show()
            finish()
        }

        // Carregar anotações salvas
        carregarAnotacoes()

        // Configurar listener para salvar automaticamente
        configurarListenerAnotacoes()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun recuperarSala() {
        sala = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("SALA_EXTRA", Sala::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("SALA_EXTRA")
        }
    }

    private fun carregarAnotacoes() {
        sala?.let { sala ->
            val prefs = getSharedPreferences(PREFS_ANOTACOES, MODE_PRIVATE)
            val anotacoes = prefs.getString("${KEY_ANOTACOES_PREFIX}${sala.id}", "")
            binding.etAnotacoesSala.setText(anotacoes)
        }
    }

    private fun salvarAnotacoes() {
        sala?.let { sala ->
            val anotacoes = binding.etAnotacoesSala.text.toString()
            val prefs = getSharedPreferences(PREFS_ANOTACOES, MODE_PRIVATE).edit()
            prefs.putString("${KEY_ANOTACOES_PREFIX}${sala.id}", anotacoes)
            prefs.apply()
        }
    }

    private fun configurarListenerAnotacoes() {
        binding.etAnotacoesSala.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                // Salvar automaticamente após mudanças
                salvarAnotacoes()
            }
        })
    }
}