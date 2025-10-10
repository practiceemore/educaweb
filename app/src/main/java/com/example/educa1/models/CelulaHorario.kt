package com.example.educa1.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CelulaHorario(
    val id: String,
    var disciplina: Disciplina? = null,
    var professor: Professor? = null,
    var turma: Turma? = null,
    var sala: Sala? = null,
    var temConflito: Boolean = false,
    var temConflitoDeSala: Boolean = false
) : Parcelable