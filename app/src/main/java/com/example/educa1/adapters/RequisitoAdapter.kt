package com.example.educa1.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.educa1.databinding.ItemRequisitoDisciplinaBinding
import com.example.educa1.models.RequisitoDisciplina

class RequisitoAdapter(
    private val listaDeRequisitos: List<RequisitoDisciplina>,
    private val onRequisitoChanged: (RequisitoDisciplina) -> Unit
) : RecyclerView.Adapter<RequisitoAdapter.RequisitoViewHolder>() {

    inner class RequisitoViewHolder(val binding: ItemRequisitoDisciplinaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequisitoViewHolder {
        val binding = ItemRequisitoDisciplinaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RequisitoViewHolder(binding)
    }

    override fun getItemCount(): Int = listaDeRequisitos.size

    override fun onBindViewHolder(holder: RequisitoViewHolder, position: Int) {
        val requisito = listaDeRequisitos[position]
        holder.binding.tvNomeDisciplinaRequisito.text = requisito.nomeDisciplina
        holder.binding.tvQuantidadeAulas.text = requisito.aulasPorSemana.toString()

        holder.binding.btnAumentarAulas.setOnClickListener {
            requisito.aulasPorSemana++
            holder.binding.tvQuantidadeAulas.text = requisito.aulasPorSemana.toString()
            onRequisitoChanged(requisito)
        }

        holder.binding.btnDiminuirAulas.setOnClickListener {
            if (requisito.aulasPorSemana > 0) {
                requisito.aulasPorSemana--
                holder.binding.tvQuantidadeAulas.text = requisito.aulasPorSemana.toString()
                onRequisitoChanged(requisito)
            }
        }
    }
}