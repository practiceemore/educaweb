package com.example.educa1.activitys

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

open class BaseActivity : AppCompatActivity() {
    
    private val TAG = "BaseActivity"

    override fun attachBaseContext(newBase: Context) {
        Log.d(TAG, "=== ATTACHBASE CONTEXT CHAMADO ===")
        try {
            val idioma = getLanguage(newBase)
            Log.d(TAG, "Idioma obtido do contexto base: $idioma")
            Log.d(TAG, "Anexando contexto base com idioma: $idioma")
            
            // APLICAR IDIOMA DIRETAMENTE NO CONTEXTO BASE
            val locale = when (idioma) {
                "es" -> Locale("es")
                else -> Locale("pt")
            }
            
            Locale.setDefault(locale)
            Log.d(TAG, "Locale padrão definido: ${Locale.getDefault()}")
            
            // CRIAR NOVO CONTEXTO COM IDIOMA APLICADO
            val config = Configuration(newBase.resources.configuration)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                config.setLocale(locale)
                Log.d(TAG, "Configuração criada (API >= 17): ${config.locales[0]}")
            } else {
                config.locale = locale
                Log.d(TAG, "Configuração criada (API < 17): ${config.locale}")
            }
            
            // CRIAR CONTEXTO LOCALIZADO
            val contextoLocalizado = newBase.createConfigurationContext(config)
            Log.d(TAG, "Contexto localizado criado: ${contextoLocalizado.javaClass.simpleName}")
            
            super.attachBaseContext(contextoLocalizado)
            Log.d(TAG, "✅ attachBaseContext concluído com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao anexar contexto base", e)
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "=== ONCREATE CHAMADO ===")
        super.onCreate(savedInstanceState)
        
        // APLICAR IDIOMA AUTOMATICAMENTE EM TODAS AS ACTIVITIES
        aplicarIdiomaSalvo()
    }
    
    private fun aplicarIdiomaSalvo() {
        try {
            val idioma = getLanguage(this)
            Log.d(TAG, "Aplicando idioma na onCreate: $idioma")
            
            // APLICAR IDIOMA DE FORMA AGRESSIVA
            val locale = when (idioma) {
                "es" -> Locale("es")
                else -> Locale("pt")
            }
            Log.d(TAG, "Locale criado: $locale")
            
            Locale.setDefault(locale)
            Log.d(TAG, "Locale padrão definido: ${Locale.getDefault()}")
            
            // FORÇAR ATUALIZAÇÃO DOS RECURSOS
            val config = Configuration(resources.configuration)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                config.setLocale(locale)
                Log.d(TAG, "Configuração atualizada (API >= 17): ${config.locales[0]}")
            } else {
                config.locale = locale
                Log.d(TAG, "Configuração atualizada (API < 17): ${config.locale}")
            }
            
            // APLICAR CONFIGURAÇÃO
            resources.updateConfiguration(config, resources.displayMetrics)
            Log.d(TAG, "Configuração aplicada aos recursos")
            
            // FORÇAR RECARREGAMENTO DOS RECURSOS
            try {
                val newContext = createConfigurationContext(config)
                Log.d(TAG, "Novo contexto de configuração criado: ${newContext.javaClass.simpleName}")
                
                // Aplicar configuração ao contexto atual
                baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
                Log.d(TAG, "Configuração aplicada ao contexto base")
                
                // FORÇAR RECARREGAMENTO DOS RECURSOS DE STRING
                Log.d(TAG, "Forçando recarregamento dos recursos de string...")
                val stringResources = newContext.resources
                Log.d(TAG, "Recursos de string recarregados: ${stringResources.javaClass.simpleName}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao criar contexto de configuração", e)
            }
            
            Log.d(TAG, "✅ Idioma aplicado na onCreate com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao aplicar idioma na onCreate", e)
        }
    }
    
    private fun getLanguage(context: Context): String {
        // LER DAS PREFERÊNCIAS DIRETAS
        val idioma = context.getSharedPreferences("LanguagePrefs", Context.MODE_PRIVATE)
            .getString("language", "pt") ?: "pt"
        Log.d(TAG, "GETLANGUAGE: Idioma obtido das preferências: $idioma")
        return idioma
    }
    
    // FUNÇÃO ESTÁTICA PARA VERIFICAR SE O IDIOMA MUDOU
    companion object {
        private var currentLanguage: String = "pt"
        
        fun updateLanguage(newLanguage: String) {
            Log.d("BaseActivity", "🔥🔥🔥 IDIOMA MUDOU DE $currentLanguage PARA $newLanguage 🔥🔥🔥")
            currentLanguage = newLanguage
        }
        
        fun getCurrentLanguage(): String = currentLanguage
    }
} 