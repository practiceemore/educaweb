package com.example.educa1.activitys

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.educa1.R
import com.example.educa1.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    
    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Inicializar Firebase Auth
        auth = Firebase.auth
        
        // Verificar se o usuário já está logado
        if (auth.currentUser != null) {
            Log.d(TAG, "Usuário já está logado: ${auth.currentUser?.email}")
            startHomeActivity()
            return
        }
        
        // Configurar listeners
        setupListeners()
        
        Log.d(TAG, "LoginActivity criada com sucesso")
    }
    
    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            Log.d(TAG, "Botão de login clicado")
            realizarLogin()
        }
        
        binding.btnRegistrar.setOnClickListener {
            Log.d(TAG, "Botão de registro clicado")
//            realizarRegistro()
        }
    }
    
    private fun realizarLogin() {
        val email = binding.etEmail.text.toString().trim()
        val senha = binding.etSenha.text.toString().trim()
        
        // Validar campos
        if (!validarCampos(email, senha)) {
            return
        }
        
        Log.d(TAG, "Iniciando login com email: $email")
        mostrarProgresso(true, getString(R.string.loading_login))
        
        auth.signInWithEmailAndPassword(email, senha)
            .addOnCompleteListener(this) { task ->
                mostrarProgresso(false)
                
                if (task.isSuccessful) {
                    Log.d(TAG, "Login bem-sucedido")
                    val user = auth.currentUser
                    Log.d(TAG, "Usuário logado: ${user?.email}")
                    mostrarMensagem("Bem-vindo!")
                    startHomeActivity()
                } else {
                    Log.w(TAG, "Falha no login", task.exception)
                    mostrarMensagem(getString(R.string.error_login_failed))
                }
            }
    }
    
    private fun realizarRegistro() {
        val email = binding.etEmail.text.toString().trim()
        val senha = binding.etSenha.text.toString().trim()
        
        // Validar campos
        if (!validarCampos(email, senha)) {
            return
        }
        
        Log.d(TAG, "Iniciando registro com email: $email")
        mostrarProgresso(true, getString(R.string.loading_register))
        
        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener(this) { task ->
                mostrarProgresso(false)
                
                if (task.isSuccessful) {
                    Log.d(TAG, "Registro bem-sucedido")
                    mostrarMensagem(getString(R.string.register_success))
                    // Não fazer login automático, deixar o usuário fazer login manualmente
                } else {
                    Log.w(TAG, "Falha no registro", task.exception)
                    val errorMessage = task.exception?.message ?: "Erro desconhecido"
                    mostrarMensagem(getString(R.string.error_register_failed, errorMessage))
                }
            }
    }
    
    private fun validarCampos(email: String, senha: String): Boolean {
        // Limpar erros anteriores
        binding.tilEmail.error = null
        binding.tilSenha.error = null
        
        var isValid = true
        
        // Validar email
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_email_empty)
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_email_invalid)
            isValid = false
        }
        
        // Validar senha
        if (senha.isEmpty()) {
            binding.tilSenha.error = getString(R.string.error_password_empty)
            isValid = false
        } else if (senha.length < 6) {
            binding.tilSenha.error = getString(R.string.error_password_short)
            isValid = false
        }
        
        return isValid
    }
    
    private fun startHomeActivity() {
        Log.d(TAG, "Redirecionando para HomeActivity")
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun mostrarProgresso(mostrar: Boolean, texto: String = "") {
        binding.progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.tvCarregando.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.tvCarregando.text = texto
        binding.btnLogin.isEnabled = !mostrar
        binding.btnRegistrar.isEnabled = !mostrar
        binding.etEmail.isEnabled = !mostrar
        binding.etSenha.isEnabled = !mostrar
        
        Log.d(TAG, "Progresso ${if (mostrar) "mostrado" else "ocultado"}")
    }
    
    private fun mostrarMensagem(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
        Log.d(TAG, "Mensagem mostrada: $mensagem")
    }
    
    override fun onStart() {
        super.onStart()
        // Verificar se o usuário já está logado
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "Usuário já está logado em onStart: ${currentUser.email}")
            startHomeActivity()
        }
    }
}
