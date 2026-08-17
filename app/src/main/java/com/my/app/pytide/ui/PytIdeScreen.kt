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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

private data class OpenFile(
    val id: Int,
    val name: String,
    val code: TextFieldValue
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PytIdeApp() {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val terminalFocusRequester = remember { FocusRequester() }

    var openFiles by remember {
        mutableStateOf(listOf(OpenFile(0, "new.py", TextFieldValue("print(\"Hello, PytIDE!\")\n"))))
    }
    var activeFileId by remember { mutableStateOf(0) }
    var nextId by remember { mutableStateOf(1) }
    val activeFile = openFiles.first { it.id == activeFileId }

    var output by remember { mutableStateOf("") }
    var showOutput by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var isWaitingForInput by remember { mutableStateOf(false) }
    var terminalInput by remember { mutableStateOf("") }
    var currentIO by remember { mutableStateOf<TerminalIO?>(null) }
    var runnerThread by remember { mutableStateOf<Thread?>(null) }

    var showInstallDialog by remember { mutableStateOf(false) }
    var installPackageName by remember { mutableStateOf("") }
    var savingFileId by remember { mutableStateOf<Int?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/x-python")
    ) { uri: Uri? ->
        val targetId = savingFileId
        if (uri != null && targetId != null) {
            try {
                val fileToSave = openFiles.first { it.id == targetId }
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(fileToSave.code.text.toByteArray())
                }
                val newName = uri.lastPathSegment?.substringAfterLast('/') ?: fileToSave.name
                openFiles = openFiles.map {
                    if (it.id == targetId) it.copy(name = newName) else it
                }
            } catch (e: Exception) {
                output += "\nОшибка сохранения: ${e.message}"
            }
        }
        savingFileId = null
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

        val codeToRun = activeFile.code.text
        val io = TerminalIO(
            onOutput = { s -> output += s },
            onWaitingChanged = { waiting -> isWaitingForInput = waiting }
        )
        currentIO = io

        val thread = Thread {
            try {
                PythonRunner.runCode(codeToRun, io)
            } catch (e: Exception) {
                io.write("Ошибка запуска Python: ${e.message}\n")
            }
            io.write("\nProgram finished")
            Handler(Looper.getMainLooper()).post {
                isRunning = false
                isWaitingForInput = false
                currentIO = null
                runnerThread = null
            }
        }
        runnerThread = thread
        thread.start()
    }

    fun exitTerminal() {
        showOutput = false
        runnerThread?.interrupt()
        runnerThread = null
        isRunning = false
        isWaitingForInput = false
        currentIO = null
    }

    fun createNewFile() {
        val id = nextId
        nextId += 1
        openFiles = openFiles + OpenFile(id, "new.py", TextFieldValue(""))
        activeFileId = id
    }

    AnimatedContent(
        targetState = showOutput,
        transitionSpec = {
            if (targetState) {
                (slideInHorizontally(animationSpec = tween(320)) { fullWidth -> fullWidth } + fadeIn(tween(320)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(320)) { fullWidth -> -fullWidth / 4 } + fadeOut(tween(220)))
            } else {
                (slideInHorizontally(animationSpec = tween(320)) { fullWidth -> -fullWidth / 4 } + fadeIn(tween(320)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(320)) { fullWidth -> fullWidth } + fadeOut(tween(220)))
            }
        },
        label = "screen-transition"
    ) { isTerminal ->
        if (isTerminal) {
            val scrollState = rememberScrollState()

            LaunchedEffect(output, terminalInput, isWaitingForInput) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
            LaunchedEffect(isWaitingForInput) {
                if (isWaitingForInput) {
                    terminalFocusRequester.requestFocus()
                    keyboardController?.show()
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { exitTerminal() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Назад в редактор", tint = Color(0xFF1A1A1A))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF1A1A1A)
                    )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .verticalScroll(scrollState)
                        .padding(8.dp)
                ) {
                    val lastNewline = output.lastIndexOf('\n')
                    val priorLines = if (lastNewline >= 0) output.substring(0, lastNewline + 1) else ""
                    val currentLinePrefix = if (lastNewline >= 0) output.substring(lastNewline + 1) else output

                    if (priorLines.isNotEmpty()) {
                        Text(
                            priorLines,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            color = Color.White
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentLinePrefix.isNotEmpty()) {
                            Text(
                                currentLinePrefix,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                color = Color.White
                            )
                        }
                        BasicTextField(
                            value = terminalInput,
                            onValueChange = { terminalInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(terminalFocusRequester),
                            textStyle = TextStyle(
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(Color.White),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { submitTerminalInput() })
                        )
                    }
                }
            }
        } else {
            Scaffold(
                topBar = {
                    Column {
                        TopAppBar(
                            title = { Text(activeFile.name, fontSize = 16.sp, color = Color(0xFF1A1A1A)) },
                            actions = {
                                IconButton(onClick = { showInstallDialog = true }) {
                                    Icon(Icons.Filled.Extension, contentDescription = "Установить библиотеку", tint = Color(0xFF1A1A1A))
                                }
                                IconButton(onClick = { createNewFile() }) {
                                    Icon(Icons.Filled.NoteAdd, contentDescription = "Новый файл", tint = Color(0xFF1A1A1A))
                                }
                                IconButton(onClick = {
                                    savingFileId = activeFile.id
                                    saveLauncher.launch(activeFile.name)
                                }) {
                                    Icon(Icons.Filled.Save, contentDescription = "Сохранить", tint = Color(0xFF1A1A1A))
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.White,
                                titleContentColor = Color(0xFF1A1A1A)
                            )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF3F3F3))
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            openFiles.forEach { file ->
                                val isActive = file.id == activeFileId
                                Box(
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .background(
                                            if (isActive) Color(0xFF2962FF) else Color(0xFFE0E0E0),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { activeFileId = file.id }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        file.name,
                                        fontSize = 12.sp,
                                        color = if (isActive) Color.White else Color(0xFF444444)
                                    )
                                }
                            }
                        }
                    }
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
                    CodeEditorField(
                        value = activeFile.code,
                        onValueChange = { newValue ->
                            openFiles = openFiles.map {
                                if (it.id == activeFileId) it.copy(code = newValue) else it
                            }
                        }
                    )
                }
            }
        }
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
