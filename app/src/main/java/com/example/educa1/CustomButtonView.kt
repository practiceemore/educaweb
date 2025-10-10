package com.example.educa1

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.educa1.databinding.BotaoInicialBinding

//import com.seupacote.seuprojeto.R // Importe o R do seu projeto
//import com.seupacote.seuprojeto.databinding.BotaoInicialBinding // Importe o binding do SEU BOTÃO

// @JvmOverloads permite que a View seja criada tanto via XML quanto via código
class CustomButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    // Usa o ViewBinding para o layout do botão, muito mais seguro!
    private val binding: BotaoInicialBinding

    // Propriedade pública para acessar e modificar o texto
    var text: String
        get() = binding.nomeBotao.text.toString()
        set(value) {
            binding.nomeBotao.text = value
        }

    init {
        // Infla o layout do botão e o anexa a este componente (o 'this' no final)
        binding = BotaoInicialBinding.inflate(LayoutInflater.from(context), this, true)

        // Pega os atributos definidos no XML (como o app:buttonText)
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.CustomButtonView, 0, 0)
            text = typedArray.getString(R.styleable.CustomButtonView_buttonText) ?: ""

            // --- LÓGICA DE VISIBILIDADE ADICIONADA AQUI ---

            // Pega o valor do XML. Se não for definido, o padrão é 'true' (mostrar).
            val showEdit = typedArray.getBoolean(R.styleable.CustomButtonView_showEditIcon, true)
            val showDelete = typedArray.getBoolean(R.styleable.CustomButtonView_showDeleteIcon, true)

            // Aplica a visibilidade com base no valor lido
            binding.icEdit.visibility = if (showEdit) View.VISIBLE else View.GONE
            binding.icDelete.visibility = if (showDelete) View.VISIBLE else View.GONE

            typedArray.recycle()
        }
    }

    // Funções públicas para expor a lógica de clique, escondendo os detalhes internos
    fun setOnDeleteClickListener(listener: () -> Unit) {
        binding.icDelete.setOnClickListener { listener() }
    }

    fun setOnEditClickListener(listener: () -> Unit) {
        binding.icEdit.setOnClickListener { listener() }
    }

    fun setEditIconVisibility(visible: Boolean) {
        binding.icEdit.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setDeleteIconVisibility(visible: Boolean) {
        binding.icDelete.visibility = if (visible) View.VISIBLE else View.GONE
    }

}