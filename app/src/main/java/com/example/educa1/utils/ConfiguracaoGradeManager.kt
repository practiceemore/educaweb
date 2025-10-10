package com.example.educa1.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.educa1.models.ConfiguracaoGrade
import com.google.gson.Gson

class ConfiguracaoGradeManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "ConfiguracaoGradePrefs"
        private const val KEY_CONFIGURACAO = "configuracao_grade_global"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun salvarConfiguracao(configuracao: ConfiguracaoGrade) {
        val json = gson.toJson(configuracao)
        prefs.edit().putString(KEY_CONFIGURACAO, json).apply()
    }
    
    fun carregarConfiguracao(): ConfiguracaoGrade {
        val json = prefs.getString(KEY_CONFIGURACAO, null)
        return if (json != null) {
            gson.fromJson(json, ConfiguracaoGrade::class.java)
        } else {
            ConfiguracaoGrade() // Configuração padrão: 5 aulas
        }
    }
    
    fun resetarParaPadrao() {
        prefs.edit().remove(KEY_CONFIGURACAO).apply()
    }
} 