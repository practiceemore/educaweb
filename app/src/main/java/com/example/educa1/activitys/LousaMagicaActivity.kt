package com.example.educa1.activitys

import android.os.Bundle
import com.example.educa1.R
import com.example.educa1.databinding.ActivityLousaMagicaBinding

class LousaMagicaActivity : BaseActivity() {

    private lateinit var binding: ActivityLousaMagicaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLousaMagicaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura a Toolbar
        setSupportActionBar(binding.toolbarLousaMagica)
        supportActionBar?.title = getString(R.string.lousa_magica)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Adiciona o botão "Voltar"
        binding.toolbarLousaMagica.setNavigationIcon(R.drawable.ic_arrow_back_white)
    }

    // Adiciona a função para lidar com o clique no botão "Voltar" da Toolbar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
