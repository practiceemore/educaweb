package com.example.educa1.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize // <-- Adicione esta linha
data class Disciplina(
    val id: Long,
    var nome: String
) : Parcelable // <-- Adicione esta parte