package com.my.app.pytide.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PytIdeApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var code by remember { mutableStateOf(TextFieldValue("print(\"Hello, PytIDE!\")\n")) }
    var fileName by remember { mutableStateOf("new.py") }
    var output by remember { mutableStateOf("") }
    var showOutput by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }

    var showNewDialog by remember { mutableStateOf(false) }
    var showInstallDialog by remember { mutableStateOf(false) }
    var installPackageName by remember { mutableStateOf("") }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/x-python")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(code.text.toByteArray())
                }
                fileName = uri.lastPathSegment?.substringAfterLast('/') ?: fileName
            } catch (e: Exception) {
                output = "Ошибка сохранения: ${e.message}"
                showOutput = true
            }
        }
    }

    fun runCode() {
        isRunning = true
        showOutput = true
        output = ""
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    PythonRunner.runCode(code.text)
                } catch (e: Exception) {
                    "Ошибка запуска Python: ${e.message}"
                }
            }
            output = result.trimEnd() + "\n\nProgram finished"
            isRunning = false
        }
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
                    IconButton(onClick = { saveLauncher.launch(fileName) }) {
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
            if (!showOutput) {
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
            }
        },
        containerColor = Color.White
    ) { padding ->
        if (showOutput) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color(0xFF0C0C0C))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A1A))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(fileName, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFB0B0B0))
                    IconButton(onClick = { showOutput = false }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = Color.White)
                    }
                }
                Text(
                    if (isRunning) "Выполнение..." else output,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                CodeEditorField(value = code, onValueChange = { code = it })
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
                    fileName = "new.py"
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
