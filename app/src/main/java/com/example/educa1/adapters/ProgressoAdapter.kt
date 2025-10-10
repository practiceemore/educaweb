package com.example.educa1.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.educa1.databinding.ItemProgressoDisciplinaBinding

// Data class simples para passar os dados para o adapter
data class ProgressoData(
    val nomeDisciplina: String,
    val aulasAlocadas: Int,
    val aulasRequeridas: Int
)

class ProgressoAdapter(
    private val listaDeProgresso: List<ProgressoData>
) : RecyclerView.Adapter<ProgressoAdapter.ProgressoViewHolder>() {

    inner class ProgressoViewHolder(val binding: ItemProgressoDisciplinaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressoViewHolder {
        val binding = ItemProgressoDisciplinaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProgressoViewHolder(binding)
    }

    override fun getItemCount(): Int = listaDeProgresso.size

    override fun onBindViewHolder(holder: ProgressoViewHolder, position: Int) {
        val progresso = listaDeProgresso[position]
        holder.binding.tvNomeDisciplinaProgresso.text = progresso.nomeDisciplina.take(3).uppercase()
        holder.binding.tvContagemProgresso.text = "${progresso.aulasAlocadas} / ${progresso.aulasRequeridas}"

        holder.binding.progressBarProgresso.max = progresso.aulasRequeridas
        holder.binding.progressBarProgresso.progress = progresso.aulasAlocadas
    }
}