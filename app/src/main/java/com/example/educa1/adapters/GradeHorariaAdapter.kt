package com.example.educa1.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.educa1.R
import com.example.educa1.databinding.ItemCelulaHorarioBinding
import com.example.educa1.models.CelulaHorario

class GradeHorariaAdapter(
    private val listaDeCelulas: List<CelulaHorario>,
    private val onItemClicked: (CelulaHorario, Int) -> Unit,
    private val isProfessorView: Boolean = false,
    private val isDisciplinaView: Boolean = false
) : RecyclerView.Adapter<GradeHorariaAdapter.CelulaViewHolder>() {

    inner class CelulaViewHolder(val binding: ItemCelulaHorarioBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CelulaViewHolder {
        val binding = ItemCelulaHorarioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CelulaViewHolder(binding)
    }

    override fun getItemCount(): Int = listaDeCelulas.size

    // Em GradeHorariaAdapter.kt

    override fun onBindViewHolder(holder: CelulaViewHolder, position: Int) {
        val celula = listaDeCelulas[position]

        holder.itemView.setOnClickListener {
            onItemClicked(celula, position)
        }

        if (celula.disciplina == null) {
            holder.binding.tvDisciplinaCelula.visibility = View.INVISIBLE
            holder.binding.tvProfessorCelula.visibility = View.INVISIBLE
            holder.binding.tvSalaCelula.visibility = View.INVISIBLE
        } else {
            holder.binding.tvDisciplinaCelula.visibility = View.VISIBLE
            holder.binding.tvProfessorCelula.visibility = View.VISIBLE
            holder.binding.tvSalaCelula.visibility = View.VISIBLE

            holder.binding.tvDisciplinaCelula.text = celula.disciplina?.nome?.take(3)?.uppercase()
            
            // Lógica diferente para visão do professor vs visão da turma vs visão da disciplina
            if (isProfessorView) {
                // Na visão do professor: mostra nome da turma em vez do professor
                holder.binding.tvProfessorCelula.text = celula.turma?.nome ?: "Turma"
                holder.binding.tvSalaCelula.text = celula.sala?.nome
            } else if (isDisciplinaView) {
                // Na visão da disciplina: mostra professor, turma e sala
                holder.binding.tvProfessorCelula.text = celula.professor?.nome
                holder.binding.tvDisciplinaCelula.text = celula.turma?.nome ?: "Turma"
                holder.binding.tvSalaCelula.text = celula.sala?.nome
            } else {
                // Na visão da turma: mostra nome do professor
                holder.binding.tvProfessorCelula.text = celula.professor?.nome
                holder.binding.tvSalaCelula.text = celula.sala?.nome
            }

            // Lógica para conflito de PROFESSOR (já existente)
            if (celula.temConflito) {
                holder.binding.tvProfessorCelula.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark))
            } else {
                holder.binding.tvProfessorCelula.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
            }

            // <<< INÍCIO DA NOVA LÓGICA DE SINALIZAÇÃO DE SALA >>>
            if (celula.temConflitoDeSala) {
                holder.binding.tvSalaCelula.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark))
            } else {
                holder.binding.tvSalaCelula.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
            }
            // <<< FIM DA NOVA LÓGICA DE SINALIZAÇÃO DE SALA >>>
        }
    }
}