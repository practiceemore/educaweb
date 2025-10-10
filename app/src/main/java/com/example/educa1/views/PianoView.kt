package com.example.educa1.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

// Data class para representar cada tecla
data class Key(
    val rect: RectF,
    val isBlack: Boolean,
    val frequency: Double, // Agora armazena a frequência da nota
    var isPressed: Boolean = false
)

class PianoView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val whiteKeyPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
    private val blackKeyPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
    private val pressedKeyPaint = Paint().apply { color = Color.LTGRAY; style = Paint.Style.FILL }
    private val linePaint = Paint().apply { color = Color.BLACK; strokeWidth = 3f }

    private var keys = listOf<Key>()
    // Listener modificado para informar a frequência e o estado (pressionado/solto)
    private var onKeyAction: ((frequency: Double, isPressed: Boolean) -> Unit)? = null

    // Mapa para rastrear qual dedo (pointerId) está pressionando qual tecla
    private val activePointers = mutableMapOf<Int, Key>()

    // Função para a Activity configurar as teclas e o listener
    fun setKeys(keys: List<Key>, listener: (frequency: Double, isPressed: Boolean) -> Unit) {
        this.keys = keys
        this.onKeyAction = listener
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Desenha as teclas brancas primeiro
        keys.filter { !it.isBlack }.forEach { key ->
            val paint = if (key.isPressed) pressedKeyPaint else whiteKeyPaint
            canvas.drawRect(key.rect, paint)
            canvas.drawRect(key.rect, linePaint.apply { style = Paint.Style.STROKE })
        }

        // Desenha as teclas pretas por cima
        keys.filter { it.isBlack }.forEach { key ->
            val paint = if (key.isPressed) pressedKeyPaint else blackKeyPaint
            canvas.drawRect(key.rect, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val actionIndex = event.actionIndex
        val pointerId = event.getPointerId(actionIndex)
        val x = event.getX(actionIndex)
        val y = event.getY(actionIndex)

        when (event.actionMasked) {
            // Um dedo tocou a tela
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // Procura a tecla pressionada, checando as pretas primeiro
                val key = keys.lastOrNull { it.rect.contains(x, y) }
                key?.let {
                    if (!it.isPressed) {
                        it.isPressed = true
                        activePointers[pointerId] = it
                        // Informa a activity que uma nota começou a tocar
                        onKeyAction?.invoke(it.frequency, true)
                    }
                }
            }
            // Um dedo foi levantado da tela
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val key = activePointers[pointerId]
                key?.let {
                    it.isPressed = false
                    // Informa a activity que a nota parou de tocar
                    onKeyAction?.invoke(it.frequency, false)
                    activePointers.remove(pointerId)
                }
            }
            // Evento cancelado (ex: o sistema tomou controle do gesto)
            MotionEvent.ACTION_CANCEL -> {
                activePointers.forEach { (_, key) ->
                    if (key.isPressed) {
                        key.isPressed = false
                        // Informa a activity que todas as notas pararam
                        onKeyAction?.invoke(key.frequency, false)
                    }
                }
                activePointers.clear()
            }
        }
        invalidate() // Redesenha a view para mostrar o estado pressionado/solto
        return true
    }
}