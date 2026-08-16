package com.my.app.pytide.python

import android.os.Handler
import android.os.Looper
import java.util.concurrent.LinkedBlockingQueue

class TerminalIO(
    private val onOutput: (String) -> Unit,
    private val onWaitingChanged: (Boolean) -> Unit
) {
    private val inputQueue = LinkedBlockingQueue<String>()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun write(s: String) {
        if (s.isEmpty()) return
        mainHandler.post { onOutput(s) }
    }

    fun readLine(): String {
        mainHandler.post { onWaitingChanged(true) }
        val value = inputQueue.take()
        mainHandler.post { onWaitingChanged(false) }
        return value
    }

    fun submitInput(text: String) {
        inputQueue.put(text)
    }
}
