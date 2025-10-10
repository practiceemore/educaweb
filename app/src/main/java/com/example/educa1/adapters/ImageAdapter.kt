package com.example.educa1.adapters

import android.net.Uri
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.educa1.activitys.ImagemItem
import com.example.educa1.databinding.ItemImagemBinding


// NOVO: A interface define o contrato de comunicação entre o Adapter e a Activity
// A interface agora define o contrato para os três tipos de interação
interface ImageInteractionListener {
    fun onImageClick(item: ImagemItem)
    fun onImageLongClick(item: ImagemItem, position: Int)
    fun onImageDoubleClick(item: ImagemItem) // NOVO MÉTODO
}

// ALTERAÇÃO 1: O construtor agora aceita uma lista de 'ImagemItem'
class ImageAdapter(
    private val itensDeImagem: List<ImagemItem>,
//    private val onItemClick: (ImagemItem) -> Unit
    private val listener: ImageInteractionListener
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    // O ViewHolder permanece o mesmo
    class ImageViewHolder(val binding: ItemImagemBinding) : RecyclerView.ViewHolder(binding.root)

    // O onCreateViewHolder permanece o mesmo
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImagemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    // O getItemCount permanece o mesmo
    override fun getItemCount(): Int = itensDeImagem.size

    // ALTERAÇÃO 2: A lógica aqui agora trabalha com o objeto 'ImagemItem'
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        // Pega o item completo (que tem o URI e o nome)
        val itemAtual = itensDeImagem[position]

        // Pega a string do URI de dentro do item e a converte de volta para um objeto Uri
        val uri = Uri.parse(itemAtual.uriString)

        // Define a imagem no ImageView, como antes
        holder.binding.ivImagemDoItem.setImageURI(uri)

        // Criamos um detector de gestos para cada item
        val gestureDetector = GestureDetectorCompat(holder.itemView.context, object : GestureDetector.SimpleOnGestureListener() {
            // Chamado quando um toque simples é confirmado.
            // É mais seguro que o onClick normal, pois garante que não foi um toque duplo.
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                listener.onImageClick(itemAtual)
                return true
            }

            // Chamado no segundo toque de um duplo toque.
            override fun onDoubleTap(e: MotionEvent): Boolean {
                listener.onImageDoubleClick(itemAtual)
                return true
            }

            // Chamado quando um clique longo é detectado.
            override fun onLongPress(e: MotionEvent) {
                listener.onImageLongClick(itemAtual, holder.adapterPosition)
            }
        })

        holder.itemView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true // Retorna true para consumir o evento
        }
        // FUTURAMENTE: Se você adicionar um TextView ao seu item_imagem.xml,
        // você poderia definir o nome aqui:
        // holder.binding.tvNomeImagem.text = itemAtual.nome
    }
}