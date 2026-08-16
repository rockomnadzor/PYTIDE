package com.my.app.pytide.ui

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.my.app.pytide.python.PythonRunner
import com.my.app.pytide.python.TerminalIO

@Composable
fun PytIdeApp() {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val terminalFocusRequester = remember { FocusRequester() }

    var code by remember { mutableStateOf(TextFieldValue("print(\"Hello, PytIDE!\")\n")) }
    var fileName by remember { mutableStateOf("new.py") }
    var output by remember { mutableStateOf("") }
    var showOutput by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var isWaitingForInput by remember { mutableStateOf(false) }
    var terminalInput by remember { mutableStateOf("") }
    var currentIO by remember { mutableStateOf<TerminalIO?>(null) }

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
                output += "\nОшибка сохранения: ${e.message}"
            }
        }
    }

    fun submitTerminalInput() {
        val text = terminalInput
        currentIO?.let { io ->
            output += "$text\n"
            io.submitInput(text)
        }
        terminalInput = ""
    }

    fun runCode() {
        isRunning = true
        isWaitingForInput = false
        showOutput = true
        output = ""

        val io = TerminalIO(
            onOutput = { s -> output += s },
            onWaitingChanged = { waiting -> isWaitingForInput = waiting }
        )
        currentIO = io

        Thread {
            try {
                PythonRunner.runCode(code.text, io)
            } catch (e: Exception) {
                io.write("Ошибка запуска Python: ${e.message}\n")
            }
            io.write("\nProgram finished")
            Handler(Looper.getMainLooper()).post {
                isRunning = false
                isWaitingForInput = false
                currentIO = null
            }
        }.start()
    }

    AnimatedContent(
        targetState = showOutput,
        transitionSpec = {
            (fadeIn(animationSpec = tween(280)) + scaleIn(initialScale = 0.96f, animationSpec = tween(280)))
                .togetherWith(fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.96f, animationSpec = tween(180)))
        },
        label = "screen-transition"
    ) { isTerminal ->
        if (isTerminal) {
            LaunchedEffect(isWaitingForInput) {
                if (isWaitingForInput) {
                    terminalFocusRequester.requestFocus()
                    keyboardController?.show()
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    Text(
                        output,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        color = Color.White
                    )
                    BasicTextField(
                        value = terminalInput,
                        onValueChange = { terminalInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(terminalFocusRequester),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 15.sp
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(Color.White),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { submitTerminalInput() })
                    )
                }

                IconButton(
                    onClick = { showOutput = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(4.dp)
                        .size(28.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = Color(0xFF555555))
                }
            }
        } else {
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
