package com.example.educa1.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Sala(
    val id: Long,
    var nome: String
) : Parcelable