package com.example.educa1.activitys

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.example.educa1.R
import com.example.educa1.databinding.ActivityHomeBinding
import com.example.educa1.interfaces.VoiceCommandListener
import com.example.educa1.utils.LocaleHelper
import android.content.res.Configuration
import android.os.Build
import java.util.Locale
import android.os.Handler
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class HomeActivity : BaseActivity(), VoiceCommandListener {

    private lateinit var binding: ActivityHomeBinding
    private val TAG = "HomeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        

        
        Log.e(TAG, "🔥��🔥 ONCREATE INICIADO ����🔥")

        
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // DEBUG: VERIFICAR SE O TOOLBAR ESTÁ FUNCIONANDO
        Log.e(TAG, "🔥🔥🔥 CONFIGURANDO TOOLBAR... 🔥🔥🔥")
        Log.e(TAG, "🔥🔥🔥 Toolbar encontrada: ${binding.toolbarHome != null} 🔥🔥🔥")

        setSupportActionBar(binding.toolbarHome)
        Log.e(TAG, "🔥🔥🔥 SupportActionBar definido: ${supportActionBar != null} 🔥🔥🔥")
        
        // DEBUG: ADICIONAR TOUCH LISTENER PARA VERIFICAR SE ESTÁ RECEBENDO TOUCHES
        binding.toolbarHome.setOnTouchListener { _, event ->
            Log.e(TAG, "🔥🔥🔥 TOUCH NO TOOLBAR: ${event.action} em (${event.x}, ${event.y}) 🔥🔥🔥")
            false // Não consumir o evento
        }

        binding.toolbarHome.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.ic_more_vert_white))


        // USAR STRING RESOURCE PARA O TÍTULO
        supportActionBar?.title = getString(R.string.titulo_home)
        Log.e(TAG, "🔥🔥🔥 Título da toolbar definido: ${supportActionBar?.title} 🔥🔥🔥")
        
        // Configurar botões
        configurarBotoes()
        
        // Configurar reconhecimento de voz
        configurarReconhecimentoVoz()
        
        Log.e(TAG, "🔥🔥🔥 ONCREATE CONCLUÍDO 🔥🔥🔥")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.e(TAG, "🔥🔥🔥 ONCREATEOPTIONSMENU CHAMADO 🔥🔥🔥")
        try {
            Log.e(TAG, "🔥🔥🔥 MenuInflater disponível: ${menuInflater != null} 🔥🔥🔥")
            Log.e(TAG, "🔥🔥🔥 Menu recebido: ${menu != null} 🔥🔥🔥")
            
            menuInflater.inflate(R.menu.menu_home, menu)
            Log.e(TAG, "🔥🔥🔥 Menu inflado com sucesso 🔥🔥🔥")
            
            // VERIFICAR SE O ITEM DE IDIOMA ESTÁ VISÍVEL
            val itemIdioma = menu.findItem(R.id.action_idioma)
            if (itemIdioma != null) {
                Log.e(TAG, "🔥🔥🔥 Item de idioma encontrado: ${itemIdioma.title} 🔥🔥🔥")
                Log.e(TAG, "🔥🔥🔥 Item visível: ${itemIdioma.isVisible} 🔥🔥🔥")
                Log.e(TAG, "🔥🔥🔥 Item habilitado: ${itemIdioma.isEnabled} 🔥🔥🔥")
                Log.e(TAG, "🔥🔥🔥 Item ID: ${itemIdioma.itemId} 🔥🔥🔥")
                Log.e(TAG, "🔥🔥🔥 Item icon: ${itemIdioma.icon} 🔥🔥🔥")
                
                // VERIFICAR SE O ITEM ESTÁ REALMENTE NO MENU
                val todosItens = mutableListOf<String>()
                for (i in 0 until menu.size()) {
                    val item = menu.getItem(i)
                    todosItens.add("${item.title} (ID: ${item.itemId})")
                }
                Log.e(TAG, "🔥🔥🔥 Todos os itens no menu: ${todosItens.joinToString(", ")} 🔥🔥🔥")
                
            } else {
                Log.e(TAG, "🔥🔥🔥 ❌ Item de idioma NÃO encontrado! 🔥🔥🔥")
            }
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "🔥🔥🔥 ❌ Erro ao inflar menu", e)
            return false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.e(TAG, "🔥🔥🔥 ONOPTIONSITEMSELECTED CHAMADO 🔥🔥🔥")
        Log.e(TAG, "🔥🔥🔥 Item selecionado: ${item.itemId} 🔥🔥🔥")
        
        return when (item.itemId) {
            R.id.action_logout -> {
                Log.d(TAG, "Logout selecionado")
                mostrarDialogoLogout()
                true
            }
            R.id.action_perfil -> {
                Log.d(TAG, "Perfil selecionado")
                mostrarInformacoesPerfil()
                true
            }
            R.id.action_idioma -> {
                Log.e(TAG, "🔥🔥🔥 BOTÃO DE IDIOMA SELECIONADO! 🔥🔥🔥")
                mostrarDialogoIdioma()
                true
            }
            else -> {
                Log.e(TAG, "🔥🔥🔥 Item não reconhecido: ${item.itemId} 🔥🔥🔥")
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun mostrarDialogoLogout() {
        Log.d(TAG, "Mostrando diálogo de logout")
        AlertDialog.Builder(this)
            .setTitle("Sair")
            .setMessage("Tem certeza de que deseja sair da sua conta?")
            .setPositiveButton("Sair") { _, _ ->
                Log.d(TAG, "Usuário confirmou logout")
                fazerLogout()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                Log.d(TAG, "Usuário cancelou logout")
            }
            .show()
    }

    private fun fazerLogout() {
        Log.d(TAG, "Fazendo logout do usuário")

        // Mostrar mensagem
        Toast.makeText(this, "Você saiu da sua conta", Toast.LENGTH_SHORT).show()

        // Redirecionar para LoginActivity
        startLoginActivity()
    }

    private fun startLoginActivity() {
        Log.d(TAG, "Redirecionando para LoginActivity")
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun mostrarInformacoesPerfil() {
        val mensagem = "Usuário logado com sucesso!\n\nEsta é uma versão de demonstração do sistema de login."
        AlertDialog.Builder(this)
            .setTitle("Perfil do Usuário")
            .setMessage(mensagem)
            .setPositiveButton("OK", null)
            .show()
    }


    private fun mostrarDialogoIdioma() {
        Log.e(TAG, "🔥🔥🔥 MOSTRANDO DIALOGO DE IDIOMA 🔥🔥🔥")
        try {
            val idiomas = arrayOf(getString(R.string.idioma_portugues), getString(R.string.idioma_espanhol))
            val idiomaAtual = LocaleHelper.getLanguage(this)
            val indiceAtual = if (idiomaAtual == "pt") 0 else 1
            Log.e(TAG, "🔥🔥🔥 Idioma atual: $idiomaAtual, Índice selecionado: $indiceAtual 🔥🔥🔥")
            Log.e(TAG, "🔥🔥🔥 Opções disponíveis: ${idiomas.joinToString(", ")} ��🔥��")

            val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.menu_idioma))
                .setSingleChoiceItems(idiomas, indiceAtual) { dialog, which ->
                    Log.e(TAG, "🔥🔥🔥 Usuário selecionou índice: $which 🔥🔥🔥")
                    val novoIdioma = if (which == 0) "pt" else "es"
                    Log.e(TAG, "🔥🔥🔥 Novo idioma selecionado: $novoIdioma 🔥🔥🔥")
                    if (novoIdioma != idiomaAtual) {
                        Log.e(TAG, "🔥🔥🔥 Idioma diferente do atual, iniciando troca... 🔥🔥🔥")
                        trocarIdioma(novoIdioma)
                    } else {
                        Log.e(TAG, "🔥🔥🔥 Mesmo idioma selecionado, não fazendo nada 🔥🔥🔥")
                    }
                    
                    // Fechar o dialog após a seleção
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.btn_cancelar), null)
                .create()
                
            dialog.show()
            Log.e(TAG, "🔥🔥🔥 Dialogo de idioma exibido com sucesso 🔥🔥🔥")
        } catch (e: Exception) {
            Log.e(TAG, "🔥🔥🔥 ❌ Erro ao mostrar diálogo de idioma", e)
        }
    }

    private fun trocarIdioma(novoIdioma: String) {
        Log.e(TAG, "🔥🔥🔥 INICIANDO TROCA DE IDIOMA SIMPLIFICADA ��🔥🔥")
        Log.e(TAG, "🔥🔥🔥 Novo idioma solicitado: $novoIdioma 🔥🔥🔥")

        try {
            // 1. SALVAR PREFERÊNCIA
            Log.e(TAG, "🔥🔥🔥 1. Salvando preferência... 🔥🔥🔥")
            getSharedPreferences("LanguagePrefs", Context.MODE_PRIVATE)
                .edit()
                .putString("language", novoIdioma)
                .commit()
            Log.e(TAG, "🔥🔥🔥 Preferência salva: $novoIdioma 🔥🔥🔥")

            // 2. NOTIFICAR BASEACTIVITY
            Log.e(TAG, "🔥🔥🔥 2. Notificando BaseActivity... 🔥🔥🔥")
            BaseActivity.updateLanguage(novoIdioma)

            // 3. APLICAR IDIOMA IMEDIATAMENTE
            Log.e(TAG, "🔥🔥🔥 3. Aplicando idioma imediatamente... 🔥🔥🔥")
            val locale = when (novoIdioma) {
                "es" -> Locale("es")
                else -> Locale("pt")
            }

            Locale.setDefault(locale)
            val config = Configuration(resources.configuration)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                config.setLocale(locale)
            } else {
                config.locale = locale
            }

            // FORÇAR ATUALIZAÇÃO IMEDIATA
            resources.updateConfiguration(config, resources.displayMetrics)

            // ATUALIZAR UI IMEDIATAMENTE
            atualizarUIComNovoIdioma(this)

            // 4. MOSTRAR MENSAGEM
            val mensagem = if (novoIdioma == "pt") {
                "Idioma alterado para Português"
            } else {
                "Idioma cambiado a Español"
            }
            Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()

            // 5. LIMPAR HISTÓRICO DO CHAT
            Log.e(TAG, "🔥��🔥 5. Limpando histórico do chat... 🔥🔥🔥")
            limparHistoricoChat()
                    
            Log.e(TAG, "🔥🔥🔥 ✅ Troca de idioma concluída! 🔥🔥��")
                    
                } catch (e: Exception) {
            Log.e(TAG, "🔥🔥�� ❌ Erro na troca de idioma", e)
        }
                }

    private fun limparHistoricoChat() {
        try {
            Log.e(TAG, "��🔥�� Limpando histórico do chat... 🔥🔥🔥")

            val prefs = getSharedPreferences("ConsultorChatPrefs", Context.MODE_PRIVATE).edit()
            prefs.remove("historico_chat_consultor")
            prefs.apply()

            Log.e(TAG, "🔥🔥🔥 ✅ Histórico do chat limpo com sucesso! 🔥🔥🔥")
        } catch (e: Exception) {
            Log.e(TAG, "🔥��🔥 ❌ Erro ao limpar histórico do chat", e)
        }
    }
    
    private fun atualizarUIComNovoIdioma(contextoLocalizado: Context) {
        try {
            Log.d(TAG, "=== ATUALIZANDO UI COM NOVO IDIOMA ===")
            
            // Atualizar título da toolbar
            val tituloNovo = contextoLocalizado.getString(R.string.titulo_home)
            Log.d(TAG, "Título atualizado: $tituloNovo")
            supportActionBar?.title = tituloNovo
            
            // Atualizar texto dos botões
            binding.btnDirecao.text = contextoLocalizado.getString(R.string.direcao)
            binding.btnProfessores.text = contextoLocalizado.getString(R.string.professores)
            binding.btnAlunos.text = contextoLocalizado.getString(R.string.alunos)
            binding.btnJuegos.text = contextoLocalizado.getString(R.string.juegos)
            
            Log.d(TAG, "✅ UI atualizada com novo idioma")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao atualizar UI", e)
        }
    }

    override fun onVoiceCommand(command: String, data: String?) {
        if (command == "FIND_AND_CLICK" && data != null) {
            Log.d(TAG, "Recebido comando para clicar em: '$data'")

            when {
                data.equals("direção", ignoreCase = true) -> binding.btnDirecao.performClick()
                data.equals("professores", ignoreCase = true) -> binding.btnProfessores.performClick()
                data.equals("alunos", ignoreCase = true) -> binding.btnAlunos.performClick()
            }
        }
    }

    private fun configurarBotoes() {
        // Configurar o clique para o botão "Alunos"
        binding.btnAlunos.setOnClickListener {
            val intent = Intent(this, SelecaoTurmaActivity::class.java)
            startActivity(intent)
        }

        // Configurar os cliques para Direção e Professores
        binding.btnDirecao.setOnClickListener {
            val intent = Intent(this, DirecaoMainActivity::class.java)
            startActivity(intent)
        }
        binding.btnProfessores.setOnClickListener {
            val intent = Intent(this, SelecaoProfessorActivity::class.java)
            startActivity(intent)
        }
        binding.btnJuegos.setOnClickListener {
            val intent = Intent(this, InicialActivity::class.java)
            startActivity(intent)
        }
    }

    private fun configurarReconhecimentoVoz() {
        // TODO: Implementar reconhecimento de voz
    }
}