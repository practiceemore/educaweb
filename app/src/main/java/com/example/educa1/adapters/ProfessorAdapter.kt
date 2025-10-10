package com.example.educa1.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.educa1.databinding.ItemProfessorBinding
import com.example.educa1.models.Professor

class ProfessorAdapter(
    private val listaDeProfessores: List<Professor>,
    // ADICIONE UM LISTENER NO CONSTRUTOR:
    private val onItemClicked: (Professor) -> Unit
) : RecyclerView.Adapter<ProfessorAdapter.ProfessorViewHolder>() {

    inner class ProfessorViewHolder(val binding: ItemProfessorBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfessorViewHolder {
        val binding = ItemProfessorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProfessorViewHolder(binding)
    }

    override fun getItemCount(): Int = listaDeProfessores.size

    override fun onBindViewHolder(holder: ProfessorViewHolder, position: Int) {
        val professorAtual = listaDeProfessores[position]
        holder.binding.tvNomeProfessor.text = professorAtual.nome

        // ADICIONE O CLIQUE AO ITEM INTEIRO:
        holder.itemView.setOnClickListener {
            onItemClicked(professorAtual)
        }
    }
}