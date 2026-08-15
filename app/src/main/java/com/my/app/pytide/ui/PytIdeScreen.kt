package com.my.app.pytide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.my.app.pytide.termux.TermuxRunner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PytIdeApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var code by remember { mutableStateOf(TextFieldValue("print(\"Hello, PytIDE!\")\n")) }
    var fileName by remember { mutableStateOf("main.py") }
    var output by remember { mutableStateOf("") }
    var showOutput by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }

    var showNewDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showInstallDialog by remember { mutableStateOf(false) }
    var saveDialogName by remember { mutableStateOf(fileName) }
    var installPackageName by remember { mutableStateOf("") }

    fun watchOutput(outputFile: File) {
        scope.launch {
            isRunning = true
            output = ""
            var ticks = 0
            while (true) {
                delay(400)
                if (outputFile.exists()) {
                    val text = outputFile.readText()
                    if (text.contains(TermuxRunner.donemarker())) {
                        output = text.replace(TermuxRunner.donemarker(), "").trim()
                            .ifBlank { "(нет вывода)" }
                        break
                    } else if (text.isNotEmpty()) {
                        output = text
                    }
                }
                ticks++
                if (ticks > 150) {
                    output = (output.ifBlank { "" }) +
                        "\n\n[Нет ответа от Termux. Проверь: Termux установлен и открывался хотя бы раз, " +
                        "в ~/.termux/termux.properties включено allow-external-apps=true, " +
                        "разрешение RUN_COMMAND выдано PytIDE.]"
                    break
                }
            }
            isRunning = false
            showOutput = true
        }
    }

    fun runCode() {
        val workDir = TermuxRunner.workDir()
        val scriptFile = File(workDir, fileName.ifBlank { "main.py" })
        scriptFile.writeText(code.text)
        val outFile = File(workDir, "output.log")
        TermuxRunner.runInTermux(context, "python3 '/sdcard/PytIDE/${scriptFile.name}'", outFile)
        watchOutput(outFile)
    }

    fun installLibrary(pkg: String) {
        val workDir = TermuxRunner.workDir()
        val outFile = File(workDir, "pip_output.log")
        TermuxRunner.runInTermux(context, "pip install $pkg", outFile)
        watchOutput(outFile)
    }

    fun saveFile(name: String) {
        val workDir = TermuxRunner.workDir()
        val target = File(workDir, if (name.endsWith(".py")) name else "$name.py")
        target.writeText(code.text)
        fileName = target.name
        output = "Сохранено: /sdcard/PytIDE/${target.name}"
        showOutput = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fileName, fontSize = 16.sp, color = Color(0xFF1A1A1A)) },
                actions = {
                    IconButton(onClick = { showInstallDialog = true }) {
                        Icon(Icons.Filled.Extension, contentDescription = "Установить библиотеку", tint = Color(0xFF1A1A1A))
                    }
                    IconButton(onClick = { showNewDialog = true }) {
                        Icon(Icons.Filled.NoteAdd, contentDescription = "Новый файл", tint = Color(0xFF1A1A1A))
                    }
                    IconButton(onClick = {
                        saveDialogName = fileName
                        showSaveDialog = true
                    }) {
                        Icon(Icons.Filled.Save, contentDescription = "Сохранить", tint = Color(0xFF1A1A1A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1A1A1A)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (!isRunning) runCode() },
                containerColor = if (isRunning) Color(0xFFBDBDBD) else Color(0xFF2962FF)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Запустить", tint = Color.White)
                }
            }
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .weight(if (showOutput) 0.6f else 1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                CodeEditorField(value = code, onValueChange = { code = it })
            }

            if (showOutput) {
                Divider(color = Color(0xFFE0E0E0))
                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxWidth()
                        .background(Color(0xFFFAFAFA))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Вывод", fontSize = 12.sp, color = Color.Gray)
                        IconButton(onClick = { showOutput = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = Color.Gray)
                        }
                    }
                    Text(
                        output.ifBlank { if (isRunning) "Выполнение..." else "" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }

    if (showNewDialog) {
        AlertDialog(
            onDismissRequest = { showNewDialog = false },
            title = { Text("Новый файл") },
            text = { Text("Текущий код будет очищен из редактора (файл на диске не удаляется). Продолжить?") },
            confirmButton = {
                TextButton(onClick = {
                    code = TextFieldValue("")
                    fileName = "main.py"
                    output = ""
                    showOutput = false
                    showNewDialog = false
                }) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { showNewDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Сохранить как") },
            text = {
                OutlinedTextField(
                    value = saveDialogName,
                    onValueChange = { saveDialogName = it },
                    label = { Text("Имя файла") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    saveFile(saveDialogName)
                    showSaveDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showInstallDialog) {
        AlertDialog(
            onDismissRequest = { showInstallDialog = false },
            title = { Text("Установить библиотеку") },
            text = {
                OutlinedTextField(
                    value = installPackageName,
                    onValueChange = { installPackageName = it },
                    label = { Text("Название пакета (pip)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (installPackageName.isNotBlank()) installLibrary(installPackageName.trim())
                    showInstallDialog = false
                    installPackageName = ""
                }) { Text("Установить") }
            },
            dismissButton = {
                TextButton(onClick = { showInstallDialog = false }) { Text("Отмена") }
            }
        )
    }
}
