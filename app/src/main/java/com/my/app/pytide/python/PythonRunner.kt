package com.my.app.pytide.python

import com.chaquo.python.Python

object PythonRunner {
    fun runCode(code: String, io: TerminalIO) {
        val py = Python.getInstance()
        val module = py.getModule("pytide_runner")
        module.callAttr("run", code, io, io)
    }
}
