package com.my.app.pytide.termux

import android.content.Context
import android.content.Intent
import android.os.Environment
import java.io.File

object TermuxRunner {

    private const val TERMUX_PACKAGE = "com.termux"
    private const val RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
    private const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"

    private const val DONE_MARKER = "__PYTIDE_DONE__"

    fun workDir(): File {
        val dir = File(Environment.getExternalStorageDirectory(), "PytIDE")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun donemarker(): String = DONE_MARKER

    fun runInTermux(context: Context, shellCommand: String, outputFile: File) {
        outputFile.writeText("")
        val wrapped = "$shellCommand > '${outputFile.absolutePath}' 2>&1; echo '$DONE_MARKER' >> '${outputFile.absolutePath}'"

        val intent = Intent()
        intent.setClassName(TERMUX_PACKAGE, RUN_COMMAND_SERVICE)
        intent.action = ACTION_RUN_COMMAND
        intent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
        intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", wrapped))
        intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            outputFile.writeText("Не удалось обратиться к Termux: ${e.message}\n$DONE_MARKER")
        }
    }
}
