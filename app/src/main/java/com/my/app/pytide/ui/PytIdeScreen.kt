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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.my.app.pytide.python.PythonRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    fun runCode() {
        isRunning = true
        showOutput = true
        output = "$ python3 ${fileName}\n"
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    PythonRunner.runCode(code.text)
                } catch (e: Exception) {
                    "Ошибка запуска Python: ${e.message}"
                }
            }
            output += result.trimEnd() + "\n\nProgram finished"
            isRunning = false
        }
    }

    fun saveFile(name: String) {
        val workDir = File(context.filesDir, "PytIDE")
        if (!workDir.exists()) workDir.mkdirs()
        val target = File(workDir, if (name.endsWith(".py")) name else "$name.py")
        target.writeText(code.text)
        fileName = target.name
        output = "Сохранено внутри приложения: ${target.name}"
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
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxWidth()
                        .background(Color(0xFF0C0C0C))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1A1A1A))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Terminal", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF9E9E9E))
                        IconButton(onClick = { showOutput = false }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = Color(0xFF9E9E9E))
                        }
                    }
                    Text(
                        output,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFF33FF66),
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    )
                }
            }
        }
    }

    if (showNewDialog) {
        AlertDialog(
            onDismissRequest = { showNewDialog = false },
            title = { Text("Новый файл") },
            text = { Text("Текущий код будет очищен из редактора (сохранённые файлы не удаляются). Продолжить?") },
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
                Column {
                    Text(
                        "Библиотеки в PytIDE зашиваются в само приложение при сборке " +
                            "(без интернета в рантайме). Впиши название пакета — " +
                            "получишь строку, которую нужно добавить в build.gradle.kts, " +
                            "закоммить и запушить: GitHub Actions соберёт новый APK уже с ней.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = installPackageName,
                        onValueChange = { installPackageName = it },
                        label = { Text("Название пакета (pip)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val pkg = installPackageName.trim()
                    if (pkg.isNotBlank()) {
                        output = "Добавь эту строку в app/build.gradle.kts, " +
                            "в блок chaquopy { defaultConfig { pip { ... } } }:\n\n" +
                            "        install(\"$pkg\")\n\n" +
                            "Потом:\ngit add .\ngit commit -m \"add $pkg\"\ngit push\n\n" +
                            "Actions пересоберёт APK с этой библиотекой."
                        showOutput = true
                    }
                    showInstallDialog = false
                    installPackageName = ""
                }) { Text("Показать инструкцию") }
            },
            dismissButton = {
                TextButton(onClick = { showInstallDialog = false }) { Text("Отмена") }
            }
        )
    }
}
