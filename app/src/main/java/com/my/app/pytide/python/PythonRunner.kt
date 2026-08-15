package com.my.app.pytide.python

import com.chaquo.python.Python

object PythonRunner {
    fun runCode(code: String): String {
        val py = Python.getInstance()
        val module = py.getModule("pytide_runner")
        return module.callAttr("run", code).toString()
    }
}
