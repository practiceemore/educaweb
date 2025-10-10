package com.example.educa1.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.educa1.databinding.ItemDisciplinaBinding
import com.example.educa1.models.Disciplina

class DisciplinaAdapter(
    private val listaDeDisciplinas: List<Disciplina>
) : RecyclerView.Adapter<DisciplinaAdapter.DisciplinaViewHolder>() {

    inner class DisciplinaViewHolder(val binding: ItemDisciplinaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisciplinaViewHolder {
        val binding = ItemDisciplinaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DisciplinaViewHolder(binding)
    }

    override fun getItemCount(): Int = listaDeDisciplinas.size

    override fun onBindViewHolder(holder: DisciplinaViewHolder, position: Int) {
        val disciplinaAtual = listaDeDisciplinas[position]
        holder.binding.text1.text = disciplinaAtual.nome // text1 é o ID que definimos em item_disciplina.xml
    }
}