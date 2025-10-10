// Em: com.example.educa1.MeuApp.kt

package com.example.educa1

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.educa1.utils.LocaleHelper

class MeuApp : Application(), Application.ActivityLifecycleCallbacks {

    private var activityReferences = 0
    private var isActivityChangingConfigurations = false
    private val TAG = "MeuApp"

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, ">>>> APLICATIVO INICIADO <<<<")
        registerActivityLifecycleCallbacks(this)
        
        // APLICAR IDIOMA SELECIONADO GLOBALMENTE
        aplicarIdiomaGlobal()
        
        // MUDANÇA AQUI
        GoogleSpeechManager.initialize(this)
    }

    private fun aplicarIdiomaGlobal() {
        try {
            val idioma = LocaleHelper.getLanguage(this)
            Log.i(TAG, "Aplicando idioma global: $idioma")
            
            // Aplicar idioma ao contexto da aplicação
            val novoContexto = LocaleHelper.setLocale(this, idioma)
            Log.i(TAG, "Idioma aplicado com sucesso")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao aplicar idioma global", e)
        }
    }

    override fun attachBaseContext(base: Context) {
        try {
            val idioma = LocaleHelper.getLanguage(base)
            val contextoLocalizado = LocaleHelper.setLocale(base, idioma)
            super.attachBaseContext(contextoLocalizado)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao anexar contexto base", e)
            super.attachBaseContext(base)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            Log.i(TAG, "App está vindo para o PRIMEIRO PLANO. Informando o GoogleSpeechManager.")
            // MUDANÇA AQUI
            GoogleSpeechManager.setAppInForeground(true)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            Log.i(TAG, "App está indo para o SEGUNDO PLANO. Informando o GoogleSpeechManager.")
            // MUDANÇA AQUI
            GoogleSpeechManager.setAppInForeground(false)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity is AppCompatActivity) {
            // MUDANÇA AQUI
            GoogleSpeechManager.registerActivity(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (activity is AppCompatActivity) {
            // MUDANÇA AQUI
            GoogleSpeechManager.unregisterActivity(activity)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}