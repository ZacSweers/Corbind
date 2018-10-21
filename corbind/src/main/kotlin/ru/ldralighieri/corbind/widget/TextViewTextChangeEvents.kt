package ru.ldralighieri.corbind.widget

import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------

data class TextViewTextChangeEvent(
        val view: TextView,
        val text: CharSequence,
        val start: Int,
        val before: Int,
        val count: Int
)

// -----------------------------------------------------------------------------------------------


fun TextView.textChangeEvents(
        scope: CoroutineScope,
        action: suspend (TextViewTextChangeEvent) -> Unit
) {
    val events = scope.actor<TextViewTextChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this))
    val listener = listener(this, events::offer)
    addTextChangedListener(listener)
    events.invokeOnClose { removeTextChangedListener(listener) }
}

suspend fun TextView.textChangeEvents(
        action: suspend (TextViewTextChangeEvent) -> Unit
) = coroutineScope {
    val events = actor<TextViewTextChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this@textChangeEvents))
    val listener = listener(this@textChangeEvents, events::offer)
    addTextChangedListener(listener)
    events.invokeOnClose { removeTextChangedListener(listener) }
}


// -----------------------------------------------------------------------------------------------


fun TextView.textChangeEvents(
        scope: CoroutineScope
): ReceiveChannel<TextViewTextChangeEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(initialValue(this@textChangeEvents))
    val listener = listener(this@textChangeEvents, ::offer)
    addTextChangedListener(listener)
    invokeOnClose { removeTextChangedListener(listener) }
}

suspend fun TextView.textChangeEvents(): ReceiveChannel<TextViewTextChangeEvent> = coroutineScope {

    produce<TextViewTextChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        offer(initialValue(this@textChangeEvents))
        val listener = listener(this@textChangeEvents, ::offer)
        addTextChangedListener(listener)
        invokeOnClose { removeTextChangedListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun initialValue(textView: TextView): TextViewTextChangeEvent =
        TextViewTextChangeEvent(textView, textView.editableText, 0, 0, 0)


// -----------------------------------------------------------------------------------------------


private fun listener(
        textView: TextView,
        emitter: (TextViewTextChangeEvent) -> Boolean
) = object : TextWatcher {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {  }
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        emitter(TextViewTextChangeEvent(textView, s, start, before, count))
    }

    override fun afterTextChanged(s: Editable) {  }
}