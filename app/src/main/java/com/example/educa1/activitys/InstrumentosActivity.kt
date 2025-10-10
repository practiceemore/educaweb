package com.example.educa1.activitys

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.educa1.databinding.ActivityInstrumentosBinding
import com.example.educa1.R
import com.example.educa1.interfaces.VoiceCommandListener

class InstrumentosActivity : BaseActivity(), VoiceCommandListener {

    private lateinit var binding: ActivityInstrumentosBinding
    private val TAG = "InstrumentosActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstrumentosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura a Toolbar
        setSupportActionBar(binding.toolbarInstrumentos)
        supportActionBar?.title = getString(R.string.instrumentos_virtuais)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Adiciona o botão "Voltar"
        binding.toolbarInstrumentos.setNavigationIcon(R.drawable.ic_arrow_back_white)

        Log.d(TAG, "InstrumentosActivity criada com sucesso.")

        // Configura os listeners dos botões
        binding.btnPianoInstrumentos.setOnClickListener {
            val intent = Intent(this, PianoActivity::class.java)
            startActivity(intent)
        }

        binding.btnTeremimInstrumentos.setOnClickListener {
            val intent = Intent(this, TeremimActivity::class.java)
            startActivity(intent)
        }

        binding.btnBateriaInstrumentos.setOnClickListener {
            Toast.makeText(this, getString(R.string.botao_bateria_clicado), Toast.LENGTH_SHORT).show()
            // TODO: Abrir a tela da Bateria
        }
    }

    // Adiciona a função para lidar com o clique no botão "Voltar" da Toolbar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onVoiceCommand(command: String, data: String?) {
        if (command == "FIND_AND_CLICK" && data != null) {
            Log.d(TAG, "Recebido comando de voz para clicar em: '$data'")
            when {
                data.equals("piano", ignoreCase = true) -> binding.btnPianoInstrumentos.performClick()
                data.equals("teremim", ignoreCase = true) -> binding.btnTeremimInstrumentos.performClick()
                data.equals("bateria", ignoreCase = true) -> binding.btnBateriaInstrumentos.performClick()
            }
        }
    }
}
