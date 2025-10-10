package com.example.educa1.models

data class ConfiguracaoGrade(
    val aulasPorDia: Int = 5
) {
    fun getTotalCelulas(): Int = 5 * aulasPorDia // 5 dias da semana
    
    fun getNomePeriodo(index: Int): String = "${index + 1}ª Aula"
    
    fun gerarNomesPeriodos(): List<String> {
        return (1..aulasPorDia).map { "${it}ª Aula" }
    }
} 