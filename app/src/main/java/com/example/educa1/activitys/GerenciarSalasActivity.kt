package com.example.educa1.activitys

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.educa1.CustomButtonView
import com.example.educa1.R
import com.example.educa1.databinding.ActivityGerenciarSalasBinding
import com.example.educa1.models.Sala
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GerenciarSalasActivity : BaseActivity() {

    private lateinit var binding: ActivityGerenciarSalasBinding
    private val listaDeSalas = mutableListOf<Sala>()

    companion object {
        const val PREFS_NAME = "DirecaoPrefs"
        const val KEY_SALAS = "key_salas"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGerenciarSalasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura a Toolbar
        setSupportActionBar(binding.toolbarGerenciarSalas)
        supportActionBar?.title = getString(R.string.gerenciar_salas)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Adiciona o botão "Voltar"
        binding.toolbarGerenciarSalas.setNavigationIcon(R.drawable.ic_arrow_back_white)

        carregarSalas()

        binding.fabAddSala.setOnClickListener {
            mostrarDialogoNovaSala()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun criarVisualBotao(sala: Sala) {
        val novoBotao = CustomButtonView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val marginBottom = resources.getDimensionPixelSize(R.dimen.margem_inferior_botao_novo)
                setMargins(0, 0, 0, marginBottom)
            }
            text = sala.nome

            setOnClickListener {
                val intent = Intent(this@GerenciarSalasActivity, GradeSalaActivity::class.java).apply {
                    putExtra("SALA_SELECIONADA", sala)
                }
                startActivity(intent)
            }

            setOnEditClickListener { mostrarDialogoEdicao(this, sala) }
            setOnDeleteClickListener { mostrarDialogoDelecao(this, sala) }
        }
        binding.lnaSalas.addView(novoBotao)
    }

    private fun adicionarNovaSala(nomeSala: String) {
        val novaSala = Sala(System.currentTimeMillis(), nomeSala)
        listaDeSalas.add(novaSala)
        criarVisualBotao(novaSala)
        salvarSalas()
        Toast.makeText(this, getString(R.string.sala_adicionada, nomeSala), Toast.LENGTH_SHORT).show()
    }

    private fun mostrarDialogoNovaSala() {
        val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        builder.setTitle(getString(R.string.nova_sala_recurso))
        val input = EditText(builder.context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            hint = getString(R.string.exemplo_sala)
        }
        builder.setView(input)

        builder.setPositiveButton(getString(R.string.btn_salvar)) { dialog, _ ->
            val nomeSala = input.text.toString().trim()
            if (nomeSala.isNotEmpty()) {
                adicionarNovaSala(nomeSala)
            } else {
                Toast.makeText(this, getString(R.string.nome_nao_pode_vazio), Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton(getString(R.string.btn_cancelar)) { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun mostrarDialogoEdicao(botaoParaEditar: CustomButtonView, sala: Sala) {
        val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        builder.setTitle(getString(R.string.editar_nome_sala))
        val input = EditText(builder.context).apply { setText(sala.nome) }
        builder.setView(input)

        builder.setPositiveButton(getString(R.string.btn_salvar)) { _, _ ->
            val nomeNovo = input.text.toString().trim()
            if (nomeNovo.isNotEmpty()) {
                sala.nome = nomeNovo
                botaoParaEditar.text = nomeNovo
                salvarSalas()
            }
        }
        builder.setNegativeButton(getString(R.string.btn_cancelar), null)
        builder.show()
    }

    private fun mostrarDialogoDelecao(botaoParaDeletar: CustomButtonView, sala: Sala) {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle(getString(R.string.confirmar_exclusao))
            .setMessage(getString(R.string.tem_certeza_excluir_sala, sala.nome))
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton(getString(R.string.sim_excluir)) { _, _ ->
                binding.lnaSalas.removeView(botaoParaDeletar)
                listaDeSalas.remove(sala)
                salvarSalas()
                Toast.makeText(this, getString(R.string.sala_excluida), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.btn_cancelar), null)
            .show()
    }

    private fun salvarSalas() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val jsonString = Gson().toJson(listaDeSalas)
        editor.putString(KEY_SALAS, jsonString)
        editor.apply()
    }

    private fun carregarSalas() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val jsonString = sharedPreferences.getString(KEY_SALAS, null)

        binding.lnaSalas.removeAllViews()
        listaDeSalas.clear()

        if (jsonString != null) {
            val type = object : TypeToken<MutableList<Sala>>() {}.type
            val salasSalvas: MutableList<Sala> = Gson().fromJson(jsonString, type)
            listaDeSalas.addAll(salasSalvas)
            listaDeSalas.forEach { sala -> criarVisualBotao(sala) }
        }
    }
}
