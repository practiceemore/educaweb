package com.example.educa1.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.educa1.R
import com.example.educa1.models.MensagemChat

class ChatAdapter(
    private val mensagens: MutableList<MensagemChat> = mutableListOf(),
    private val onMensagemLongClick: ((MensagemChat, Int) -> Unit)? = null,
    private val onMensagemClick: ((MensagemChat, Int) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_IA = 1
        private const val VIEW_TYPE_USUARIO = 2
    }

    private val mensagensSelecionadas = mutableSetOf<Int>()

    fun getMensagensSelecionadas(): Set<Int> = mensagensSelecionadas.toSet()

    fun isMensagemSelecionada(position: Int): Boolean = mensagensSelecionadas.contains(position)

    fun selecionarMensagem(position: Int) {
        mensagensSelecionadas.add(position)
        notifyItemChanged(position)
    }

    fun desselecionarMensagem(position: Int) {
        mensagensSelecionadas.remove(position)
        notifyItemChanged(position)
    }

    fun limparSelecoes() {
        val posicoesAlteradas = mensagensSelecionadas.toList()
        mensagensSelecionadas.clear()
        posicoesAlteradas.forEach { notifyItemChanged(it) }
    }

    fun getMensagens(): MutableList<MensagemChat> = mensagens

    fun adicionarMensagem(mensagem: MensagemChat) {
        mensagens.add(mensagem)
        notifyItemInserted(mensagens.size - 1)
    }

    fun limparMensagens() {
        mensagens.clear()
        mensagensSelecionadas.clear()
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (mensagens[position].isIA) VIEW_TYPE_IA else VIEW_TYPE_USUARIO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_IA -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_mensagem_ia, parent, false)
                IAMensagemViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_mensagem_usuario, parent, false)
                UsuarioMensagemViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mensagem = mensagens[position]
        val isSelecionada = isMensagemSelecionada(position)

        when (holder) {
            is IAMensagemViewHolder -> {
                holder.bind(mensagem, isSelecionada)
                holder.itemView.setOnLongClickListener {
                    onMensagemLongClick?.invoke(mensagem, position)
                    true
                }
                holder.itemView.setOnClickListener {
                    if (mensagensSelecionadas.isNotEmpty()) {
                        // Modo seleção ativo - alternar seleção
                        if (isSelecionada) {
                            desselecionarMensagem(position)
                        } else {
                            selecionarMensagem(position)
                        }
                        onMensagemClick?.invoke(mensagem, position)
                    }
                }
            }
            is UsuarioMensagemViewHolder -> {
                holder.bind(mensagem, isSelecionada)
                holder.itemView.setOnLongClickListener {
                    onMensagemLongClick?.invoke(mensagem, position)
                    true
                }
                holder.itemView.setOnClickListener {
                    if (mensagensSelecionadas.isNotEmpty()) {
                        // Modo seleção ativo - alternar seleção
                        if (isSelecionada) {
                            desselecionarMensagem(position)
                        } else {
                            selecionarMensagem(position)
                        }
                        onMensagemClick?.invoke(mensagem, position)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = mensagens.size

    inner class IAMensagemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tvMensagem)

        fun bind(mensagem: MensagemChat, isSelecionada: Boolean) {
            textView.text = mensagem.texto
            
            // Aplicar visual de seleção
            if (isSelecionada) {
                itemView.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.selecao_translucida)
                )
            } else {
                itemView.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, android.R.color.transparent)
                )
            }
        }
    }

    inner class UsuarioMensagemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tvMensagem)

        fun bind(mensagem: MensagemChat, isSelecionada: Boolean) {
            textView.text = mensagem.texto
            
            // Aplicar visual de seleção
            if (isSelecionada) {
                itemView.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.selecao_translucida)
                )
            } else {
                itemView.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, android.R.color.transparent)
                )
            }
        }
    }
} 