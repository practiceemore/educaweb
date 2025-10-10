package com.example.educa1.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize // <-- Adicione esta linha
data class Professor(
    val id: Long,
    var nome: String,
    var disciplinas: MutableList<String> = mutableListOf(),
    var indisponibilidades: MutableList<String> = mutableListOf(),
    var aulasContratadas: Int = 0 // <<< ADICIONE ESTA LINHA
) : Parcelable // <-- Adicione esta parte