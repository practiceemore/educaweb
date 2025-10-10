package com.example.educa1

import android.content.Context
import android.util.Log
import com.example.educa1.utils.LocaleHelper
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.Locale

data class SugestaoGradeIA(
    val sugestoes: List<SugestaoAula>,
    val observacoes: String,
    val conflitos: List<String>,
    val aulasNaoAlocadas: List<String>
)

data class SugestaoAula(
    val posicao: Int,
    val disciplina: String,
    val professor: String,
    val sala: String?,
    val tipo: String, // "dupla" ou "individual"
    val prioridade: Int
)

/**
 * Deserializer customizado para converter objetos em strings
 */
class StringListDeserializer : JsonDeserializer<List<String>> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: java.lang.reflect.Type?,
        context: JsonDeserializationContext?
    ): List<String> {
        if (json == null || !json.isJsonArray) {
            return emptyList()
        }
        
        val array = json.asJsonArray
        val result = mutableListOf<String>()
        
        for (element in array) {
            when {
                element.isJsonPrimitive -> {
                    result.add(element.asString)
                }
                element.isJsonObject -> {
                    result.add(element.toString())
                }
                element.isJsonArray -> {
                    result.add(element.toString())
                }
            }
        }
        
        return result
    }
}

class GeminiManager(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
//    private val baseUrl = "http://192.168.0.38:8080"
    private val baseUrl = "https://educa-backend-560531251903.us-central1.run.app"
    
    private val gson = GsonBuilder()
                    .registerTypeAdapter(object : TypeToken<List<String>>() {}.type, StringListDeserializer())
                    .create()
                
    private val idiomaAtual: String
        get() = LocaleHelper.getLanguage(context)

    suspend fun gerarRelatorioComIA(
        solicitacao: String, 
        dadosAnaliseJson: String,
        idioma: String = "pt"
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = callBackendPython("/gerar-relatorio", mapOf(
                    "solicitacao" to solicitacao,
                    "dados_analise" to dadosAnaliseJson,
                    "idioma" to idioma
                ))
                
                response ?: "Erro ao gerar relatório"
            } catch (e: Exception) {
                Log.e("GeminiManager", "Erro ao gerar relatório", e)
                "Erro ao gerar relatório: ${e.message}"
            }
        }
    }

    suspend fun gerarRespostaConsultor(
        mensagem: String, 
        dadosEscolaJson: String,
        contextoHistorico: String = "",
        idioma: String = "pt"
    ): String {
        try {
            // === LOG PROMPT EXATO ===
            Log.d("promptexato", "=== CONSULTOR IA - PROMPT EXATO ===")
            Log.d("promptexato", "Mensagem do usuário: $mensagem")
            Log.d("promptexato", "Contexto histórico: $contextoHistorico")
            Log.d("promptexato", "Dados da escola (JSON): $dadosEscolaJson")
            Log.d("promptexato", "Endpoint: /consultor")
            
            val response = callBackendPython("/consultor", mapOf(
                "mensagem" to mensagem,
                "dados_escola" to dadosEscolaJson,
                "contexto_historico" to contextoHistorico,
                "idioma" to idioma
            ))
            
            return response ?: context.getString(R.string.erro_ia_consultor)
        } catch (e: Exception) {
            Log.e("GeminiManager", "Erro ao processar consultor", e)
            throw Exception("Erro ao processar consultor: ${e.message}")
        }
    }

    suspend fun gerarRespostaGradeTexto(
        solicitacao: String, 
        dadosGradeJson: String,
        idioma: String = "pt"
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = callBackendPython("/gerar-grade", mapOf(
                    "solicitacao" to solicitacao,
                    "dados_grade" to dadosGradeJson,
                    "idioma" to idioma
                ))
                
                response ?: "Erro ao gerar grade"
        } catch (e: Exception) {
                Log.e("GeminiManager", "Erro ao gerar grade", e)
                "Erro ao gerar grade: ${e.message}"
        }
    }
    }

    private suspend fun callBackendPython(endpoint: String, data: Map<String, Any>): String? {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = JSONObject().apply {
                    data.forEach { (key, value) ->
                        put(key, value)
                    }
                }.toString()

                val request = Request.Builder()
                    .url("$baseUrl$endpoint")
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .build()

                Log.d("GeminiManager", "Enviando requisição para: $baseUrl$endpoint")
                Log.d("GeminiManager", "Dados: ${requestBody.take(200)}...")

                val response = httpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d("GeminiManager", "Resposta recebida: ${responseBody?.take(200)}...")
                    
                    val jsonResponse = JSONObject(responseBody ?: "")
                    jsonResponse.getString("resposta")
                } else {
                    val errorBody = response.body?.string()
                    Log.e("GeminiManager", "Erro do servidor: ${response.code} - $errorBody")
                    throw Exception("Erro do servidor: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("GeminiManager", "Erro ao chamar backend", e)
                throw e
            }
        }
    }
}