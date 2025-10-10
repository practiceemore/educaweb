package com.example.educa1.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RequisitoDisciplina(
    val nomeDisciplina: String,
    var aulasPorSemana: Int = 0
) : Parcelable