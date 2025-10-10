package com.example.educa1.activitys

import android.content.Intent
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.educa1.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

// Extensões para HomeActivity para funcionalidades de Firebase Auth
fun HomeActivity.initializeFirebaseAuth() {
    val auth = Firebase.auth
    
    // Verificar se o usuário está logado
    if (auth.currentUser == null) {
        Log.d("HomeActivity", "Usuário não está logado, redirecionando para Login")
        startLoginActivity()
        return
    }
    
    Log.d("HomeActivity", "Usuário logado: ${auth.currentUser?.displayName}")
}

fun HomeActivity.setupFirebaseMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_home, menu)
    return true
}

fun HomeActivity.handleFirebaseMenuSelection(item: MenuItem): Boolean {
    return when (item.itemId) {
        R.id.action_logout -> {
            mostrarDialogoLogout()
            true
        }
        R.id.action_perfil -> {
            mostrarInformacoesPerfil()
            true
        }
        else -> false
    }
}

private fun HomeActivity.mostrarDialogoLogout() {
    AlertDialog.Builder(this)
        .setTitle("Sair")
        .setMessage("Tem certeza de que deseja sair da sua conta?")
        .setPositiveButton("Sair") { _, _ ->
            fazerLogout()
        }
        .setNegativeButton("Cancelar", null)
        .show()
}

private fun HomeActivity.fazerLogout() {
    Log.d("HomeActivity", "Fazendo logout do usuário")
    
    // Fazer logout do Firebase
    Firebase.auth.signOut()
    
    // Mostrar mensagem
    Toast.makeText(this, "Você saiu da sua conta", Toast.LENGTH_SHORT).show()
    
    // Redirecionar para LoginActivity
    startLoginActivity()
}

fun HomeActivity.startLoginActivity() {
    val intent = Intent(this, LoginActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    startActivity(intent)
    finish()
}

private fun HomeActivity.mostrarInformacoesPerfil() {
    val user = Firebase.auth.currentUser
    if (user != null) {
        val mensagem = "Nome: ${user.displayName}\nEmail: ${user.email}"
        AlertDialog.Builder(this)
            .setTitle("Perfil do Usuário")
            .setMessage(mensagem)
            .setPositiveButton("OK", null)
            .show()
    }
}
