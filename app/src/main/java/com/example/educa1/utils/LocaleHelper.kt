package com.example.educa1.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.util.Log
import java.util.*

object LocaleHelper {
    
    private const val PREF_NAME = "LanguagePrefs"
    private const val PREF_LANGUAGE = "language"
    private const val TAG = "LocaleHelper"
    
    fun setLocale(context: Context, language: String): Context {
        Log.d(TAG, "=== SETLOCALE CHAMADO ===")
        Log.d(TAG, "Contexto recebido: ${context.javaClass.simpleName}")
        Log.d(TAG, "Idioma solicitado: $language")
        Log.d(TAG, "Idioma atual do contexto: ${getLanguage(context)}")

        val locale = when (language) {
            "es" -> Locale("es")
            else -> Locale("pt")
        }
        Log.d(TAG, "Locale criado: $locale")

        Locale.setDefault(locale)
        Log.d(TAG, "Locale padrão definido: ${Locale.getDefault()}")

        val config = Configuration(context.resources.configuration)
        Log.d(TAG, "Configuração original: ${config.locale}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale)
            Log.d(TAG, "Configuração atualizada (API >= 17): ${config.locales[0]}")
        } else {
            config.locale = locale
            Log.d(TAG, "Configuração atualizada (API < 17): ${config.locale}")
        }

        // APLICAR CONFIGURAÇÃO DIRETAMENTE NOS RECURSOS
        try {
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            Log.d(TAG, "Configuração aplicada diretamente aos recursos")
            
            // FORÇAR RECARREGAMENTO DOS RECURSOS
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            Log.d(TAG, "Configuração forçada novamente aos recursos")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao aplicar configuração diretamente", e)
        }

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_LANGUAGE, language)
            .apply()
        Log.d(TAG, "Preferência salva: $language")

        val novoContexto = context.createConfigurationContext(config)
        Log.d(TAG, "Novo contexto criado: ${novoContexto.javaClass.simpleName}")
        Log.d(TAG, "✅ SETLOCALE concluído com sucesso")

        return novoContexto
    }
    
    fun getLanguage(context: Context): String {
        // PRIMEIRO: Tentar ler das preferências diretas
        val idiomaDireto = context.getSharedPreferences("LanguagePrefs", Context.MODE_PRIVATE)
            .getString("language", null)
        
        if (idiomaDireto != null) {
            Log.d(TAG, "GETLANGUAGE: Idioma obtido das preferências diretas: $idiomaDireto")
            return idiomaDireto
        }
        
        // SEGUNDO: Tentar ler das preferências antigas (fallback)
        val idioma = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(PREF_LANGUAGE, "pt") ?: "pt"
        Log.d(TAG, "GETLANGUAGE: Idioma obtido das preferências antigas: $idioma")
        return idioma
    }
    
    fun getLocale(resources: Resources): Locale {
        val config = resources.configuration
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.locales[0]
        } else {
            config.locale
        }
    }
    
    fun updateResources(context: Context, language: String): Context {
        Log.d(TAG, "=== UPDATERESOURCES CHAMADO ===")
        Log.d(TAG, "Contexto recebido: ${context.javaClass.simpleName}")
        Log.d(TAG, "Idioma solicitado: $language")

        val locale = when (language) {
            "es" -> Locale("es")
            else -> Locale("pt")
        }
        Log.d(TAG, "Locale criado: $locale")

        Locale.setDefault(locale)
        Log.d(TAG, "Locale padrão definido: ${Locale.getDefault()}")

        val config = Configuration(context.resources.configuration)
        Log.d(TAG, "Configuração original: ${config.locale}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale)
            Log.d(TAG, "Configuração atualizada (API >= 17): ${config.locales[0]}")
        } else {
            config.locale = locale
            Log.d(TAG, "Configuração atualizada (API < 17): ${config.locale}")
        }

        // APLICAR CONFIGURAÇÃO DIRETAMENTE NOS RECURSOS
        try {
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            Log.d(TAG, "Configuração aplicada diretamente aos recursos")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao aplicar configuração diretamente", e)
        }

        val novoContexto = context.createConfigurationContext(config)
        Log.d(TAG, "Novo contexto criado: ${novoContexto.javaClass.simpleName}")
        Log.d(TAG, "✅ UPDATERESOURCES concluído com sucesso")

        return novoContexto
    }
} 