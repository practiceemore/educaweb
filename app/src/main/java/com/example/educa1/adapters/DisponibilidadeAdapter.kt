package com.example.educa1.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.educa1.R
import com.example.educa1.databinding.ItemCelulaDisponibilidadeBinding

class DisponibilidadeAdapter(
    private val totalDeSlots: Int,
    private val slotsIndisponiveis: Set<String>,
    private val onItemClicked: (String, Boolean) -> Unit // Retorna o ID do slot e se ele estava indisponível
) : RecyclerView.Adapter<DisponibilidadeAdapter.SlotViewHolder>() {

    inner class SlotViewHolder(val binding: ItemCelulaDisponibilidadeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val binding = ItemCelulaDisponibilidadeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SlotViewHolder(binding)
    }

    override fun getItemCount(): Int = totalDeSlots

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        val dia = position % 5
        val horario = position / 5
        val slotId = "${dia}_${horario}"
        val estaIndisponivel = slotsIndisponiveis.contains(slotId)

        val corDeFundo = if (estaIndisponivel) {
            ContextCompat.getColor(holder.itemView.context, R.color.cor_indisponivel) // Cor escura/vermelha
        } else {
            ContextCompat.getColor(holder.itemView.context, R.color.cor_disponivel) // Cor clara/verde
        }
        holder.itemView.setBackgroundColor(corDeFundo)

        holder.itemView.setOnClickListener {
            onItemClicked(slotId, estaIndisponivel)
        }
    }
}