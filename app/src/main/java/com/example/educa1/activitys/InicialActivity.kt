package com.example.educa1.activitys

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.educa1.GoogleSpeechManager
import com.example.educa1.databinding.ActivityInicialBinding
import com.example.educa1.interfaces.VoiceCommandListener
import com.example.educa1.R

class InicialActivity : BaseActivity(), VoiceCommandListener {
    private lateinit var binding: ActivityInicialBinding
    private val PERMISSIONS_REQUEST_RECORD_AUDIO = 1
    private val TAG = "InicialActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInicialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura a Toolbar
        setSupportActionBar(binding.toolbarInicial)
        supportActionBar?.title = getString(R.string.area_aluno)
        // Adiciona o botão "Voltar" (Up) na Toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarInicial.setNavigationIcon(R.drawable.ic_arrow_back_white)

        Log.d(TAG, "onCreate chamado.")

        // Configurando os listeners dos botões
        binding.btnImagensInicial.setOnClickListener {
            val intent = Intent(this, ImagensActivity::class.java)
            startActivity(intent)
        }

        binding.btnInstrumentosInicial.setOnClickListener {
            val intent = Intent(this, InstrumentosActivity::class.java)
            startActivity(intent)
        }

        binding.btnLousaMagicaInicial.setOnClickListener {
            val intent = Intent(this, LousaMagicaActivity::class.java)
            startActivity(intent)
        }
    }

    // Lida com o clique no botão "Voltar" (Up) da Toolbar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onVoiceCommand(command: String, data: String?) {
        if (command == "FIND_AND_CLICK" && data != null) {
            Log.d(TAG, "Recebido comando para clicar em: '$data'")

            when {
                data.equals("imagens", ignoreCase = true) -> {
                    Log.d(TAG, "Clicando em Imagens por voz.")
                    binding.btnImagensInicial.performClick()
                }
                data.equals("instrumentos virtuais", ignoreCase = true) -> {
                    Log.d(TAG, "Clicando em Instrumentos virtuais por voz.")
                    binding.btnInstrumentosInicial.performClick()
                }
                data.equals("lousa mágica", ignoreCase = true) -> {
                    Log.d(TAG, "Clicando em Lousa Mágica por voz.")
                    binding.btnLousaMagicaInicial.performClick()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume chamado, verificando permissões...")
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissionCheck = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permissão já estava concedida. Informando o GoogleSpeechManager.")
            GoogleSpeechManager.onPermissionResult(true)
        } else {
            Log.w(TAG, "Permissão não encontrada. Solicitando ao usuário...")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSIONS_REQUEST_RECORD_AUDIO)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            Log.i(TAG, "Resultado da solicitação de permissão: Concedida = $granted. Informando o GoogleSpeechManager.")
            GoogleSpeechManager.onPermissionResult(granted)
            if (!granted) {
                Toast.makeText(this, "Permissão de áudio negada. Comandos de voz não funcionarão.", Toast.LENGTH_LONG).show()
            }
        }
    }
}