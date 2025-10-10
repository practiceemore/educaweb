package com.example.educa1.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.educa1.databinding.ItemRelatorioCargaHorariaBinding

/**
 * Data class para transportar os dados calculados para cada linha do relatório.
 */
data class RelatorioData(
    val professorNome: String,
    val aulasAlocadas: Int,
    val aulasContratadas: Int
)

/**
 * Adapter para exibir a lista de professores e suas cargas horárias no relatório.
 */
class RelatorioCargaHorariaAdapter(
    private val listaDadosRelatorio: List<RelatorioData>
) : RecyclerView.Adapter<RelatorioCargaHorariaAdapter.RelatorioViewHolder>() {

    inner class RelatorioViewHolder(val binding: ItemRelatorioCargaHorariaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelatorioViewHolder {
        val binding = ItemRelatorioCargaHorariaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RelatorioViewHolder(binding)
    }

    override fun getItemCount(): Int = listaDadosRelatorio.size

    override fun onBindViewHolder(holder: RelatorioViewHolder, position: Int) {
        val dados = listaDadosRelatorio[position]

        // Preenche os TextViews com os dados
        holder.binding.tvNomeProfessorRelatorio.text = dados.professorNome
        holder.binding.tvContagemRelatorio.text = "${dados.aulasAlocadas} / ${dados.aulasContratadas}"

        // Configura a ProgressBar
        // Se o professor não tiver aulas contratadas, a barra fica em 0% para evitar divisão por zero.
        if (dados.aulasContratadas > 0) {
            holder.binding.progressBarRelatorio.max = dados.aulasContratadas
            holder.binding.progressBarRelatorio.progress = dados.aulasAlocadas
        } else {
            holder.binding.progressBarRelatorio.max = 1
            holder.binding.progressBarRelatorio.progress = 0
        }
    }
}