// Em: com.example.educa1.activitys.ImagensActivity.kt

package com.example.educa1.activitys

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.educa1.CustomButtonView
import com.example.educa1.R
import com.example.educa1.interfaces.VoiceCommandListener
import com.example.educa1.databinding.ActivityImagensBinding
import java.util.Locale

// MUDANÇA: A Activity agora implementa a nossa nova interface
class ImagensActivity : BaseActivity(), VoiceCommandListener {

    private lateinit var binding: ActivityImagensBinding
    private val TAG = "ImagensActivity"

    companion object {
        const val PREFS_NAME = "ImagensActivityPrefs"
        const val KEY_BOTOES_SALVOS = "key_botoes_salvos"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagensBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura a Toolbar
        setSupportActionBar(binding.toolbarImagens)
        supportActionBar?.title = getString(R.string.imagens)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Adiciona o botão "Voltar"
        binding.toolbarImagens.setNavigationIcon(R.drawable.ic_arrow_back_white)

        binding.fabAddImagens.setOnClickListener {
            mostrarDialogoNomeBotao()
        }

        carregarBotoesSalvos()
    }

    // Adiciona a função para lidar com o clique no botão "Voltar" da Toolbar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // MUDANÇA: Implementação do método da interface.
    // Esta função será chamada pelo VoskVoiceManager.
    override fun onVoiceCommand(command: String, data: String?) {
        // CASO 1: O comando é para criar uma nova pasta
        if (command == "CREATE_FOLDER" && data != null) {
            Log.d(TAG, "Comando de voz recebido: Criar pasta '$data'")

            // Capitaliza a primeira letra do nome da pasta
            val nomeBotao = data.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            if (nomeBotao.isNotEmpty()) {
                val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                val botoesExistentes = sharedPreferences.getStringSet(KEY_BOTOES_SALVOS, emptySet())
                if (botoesExistentes?.contains(nomeBotao) == true) {
                    Toast.makeText(this, "Comando de voz: Uma pasta com o nome '$nomeBotao' já existe.", Toast.LENGTH_SHORT).show()
                } else {
                    adicionarNovoBotao(nomeBotao)
                }
            }
        }
        // CASO 2: O comando é para encontrar e clicar em uma pasta existente
        else if (command == "FIND_AND_CLICK" && data != null) {
            Log.d(TAG, "Recebido comando para clicar na pasta: '$data'")

            var pastaEncontrada = false // Flag para sabermos se encontramos a pasta
            for (i in 0 until binding.lnaImagens.childCount) {
                val child = binding.lnaImagens.getChildAt(i)
                if (child is CustomButtonView) {
                    if (child.text.toString().equals(data, ignoreCase = true)) {
                        Log.d(TAG, "Pasta '${child.text}' encontrada. Clicando...")
                        child.performClick()
                        pastaEncontrada = true
                        break
                    }
                }
            }

            // Opcional: Dar um feedback se a pasta não for encontrada
            if (!pastaEncontrada) {
                Toast.makeText(this, "Pasta '$data' não encontrada.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun carregarBotoesSalvos() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val botoesSalvosSet = sharedPreferences.getStringSet(KEY_BOTOES_SALVOS, null)
        binding.lnaImagens.removeAllViews()
        botoesSalvosSet?.sorted()?.forEach { nomeBotao ->
            criarVisualBotao(nomeBotao)
        }
    }

    private fun criarVisualBotao(textoDoBotao: String) {
        val novoBotao = CustomButtonView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val marginBottom = resources.getDimensionPixelSize(R.dimen.margem_inferior_botao_novo)
                setMargins(0, 0, 0, marginBottom)
            }
            text = textoDoBotao
            setOnClickListener {
                val intent = Intent(context, PastaDetalhesActivity::class.java)
                intent.putExtra("NOME_PASTA", text)
                context.startActivity(intent)
            }
            setOnEditClickListener { mostrarDialogoEdicao(this) }
            setOnDeleteClickListener { mostrarDialogoDelecao(this) }
        }
        binding.lnaImagens.addView(novoBotao)
    }

    private fun adicionarNovoBotao(textoDoBotao: String) {
        criarVisualBotao(textoDoBotao)
        salvarNomeBotao(textoDoBotao)
        Toast.makeText(this, "Pasta '$textoDoBotao' criada", Toast.LENGTH_SHORT).show()
    }

    private fun mostrarDialogoNomeBotao() {
        val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        builder.setTitle("Nova Pasta")
        val input = EditText(builder.context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.hint = "Digite o nome aqui"
        builder.setView(input)
        builder.setPositiveButton("OK") { dialog, _ ->
            val nomeBotao = input.text.toString().trim()
            if (nomeBotao.isNotEmpty()) {
                val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                val botoesExistentes = sharedPreferences.getStringSet(KEY_BOTOES_SALVOS, emptySet())
                if (botoesExistentes?.contains(nomeBotao) == true) {
                    Toast.makeText(this, "Uma pasta com este nome já existe.", Toast.LENGTH_SHORT).show()
                } else {
                    adicionarNovoBotao(nomeBotao)
                }
            } else {
                Toast.makeText(this, "O nome não pode estar vazio", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    // As funções de salvar, deletar e editar permanecem as mesmas
    private fun salvarNomeBotao(nomeBotao: String) {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val botoesSalvosSet = sharedPreferences.getStringSet(KEY_BOTOES_SALVOS, HashSet())?.toMutableSet() ?: mutableSetOf()
        botoesSalvosSet.add(nomeBotao)
        editor.putStringSet(KEY_BOTOES_SALVOS, botoesSalvosSet)
        editor.apply()
    }

    private fun mostrarDialogoDelecao(botaoParaDeletar: CustomButtonView) {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem certeza que deseja excluir a pasta '${botaoParaDeletar.text}' e todo o seu conteúdo permanentemente?")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("Sim, excluir") { _, _ ->
                val nomeDaPasta = botaoParaDeletar.text.toString()
                val detalhesPrefs = getSharedPreferences("PastaDetalhesPrefs", MODE_PRIVATE)
                detalhesPrefs.edit().remove("itens_imagens_$nomeDaPasta").apply()
                binding.lnaImagens.removeView(botaoParaDeletar)
                removerNomeSalvo(nomeDaPasta)
                Toast.makeText(this, "Pasta e conteúdo excluídos.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEdicao(botaoParaEditar: CustomButtonView) {
        val nomeAntigo = botaoParaEditar.text
        val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        builder.setTitle("Editar Nome da Pasta")
        val input = EditText(builder.context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(nomeAntigo)
        builder.setView(input)
        builder.setPositiveButton("Salvar") { _, _ ->
            val nomeNovo = input.text.toString().trim()
            if (nomeNovo.isNotEmpty() && nomeNovo != nomeAntigo) {
                botaoParaEditar.text = nomeNovo
                atualizarNomeSalvo(nomeAntigo.toString(), nomeNovo)
            }
        }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun removerNomeSalvo(nomeBotao: String) {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val botoesSalvosSet = sharedPreferences.getStringSet(KEY_BOTOES_SALVOS, HashSet())?.toMutableSet()
        botoesSalvosSet?.remove(nomeBotao)
        editor.putStringSet(KEY_BOTOES_SALVOS, botoesSalvosSet)
        editor.apply()
    }

    private fun atualizarNomeSalvo(nomeAntigo: String, nomeNovo: String) {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val botoesSalvosSet = sharedPreferences.getStringSet(KEY_BOTOES_SALVOS, HashSet())?.toMutableSet()
        botoesSalvosSet?.remove(nomeAntigo)
        botoesSalvosSet?.add(nomeNovo)
        editor.putStringSet(KEY_BOTOES_SALVOS, botoesSalvosSet)
        editor.apply()
    }
}