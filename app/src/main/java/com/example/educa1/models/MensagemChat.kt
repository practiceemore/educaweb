package com.example.educa1.models

import java.text.SimpleDateFormat
import java.util.*

data class MensagemChat(
    val id: String = UUID.randomUUID().toString(),
    val texto: String,
    val isIA: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val tipo: TipoMensagem = TipoMensagem.TEXTO
) {
    enum class TipoMensagem {
        TEXTO,
        SUGESTAO_GRADE,
        ERRO,
        CONFIRMACAO,
        ALERTA
    }

    fun getHorarioFormatado(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
} 