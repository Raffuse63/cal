package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HistoryItem
import com.example.data.HistoryRepository
import com.example.util.CalculatorEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.*

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = HistoryRepository(db.historyDao())

    val historyList: StateFlow<List<HistoryItem>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI States
    val isDarkTheme = MutableStateFlow(false)
    val basicMode = MutableStateFlow(true)
    val expr = MutableStateFlow("")
    val cursorPosition = MutableStateFlow<Int?>(null)
    val result = MutableStateFlow("Welcome to Hridoy's World")
    val isWelcome = MutableStateFlow(true)

    val shift = MutableStateFlow(false)
    val alpha = MutableStateFlow(false)
    val mode = MutableStateFlow("DEG") // DEG or RAD
    val calcBase = MutableStateFlow(10) // 10, 2, 8, 16
    val waitingForSto = MutableStateFlow(false)
    val justCalculated = MutableStateFlow(false)
    val variables = MutableStateFlow<Map<String, Double>>(
        mapOf("A" to 0.0, "B" to 0.0, "C" to 0.0, "D" to 0.0, "E" to 0.0, "F" to 0.0)
    )

    // Show popup flags
    val showHistory = MutableStateFlow(false)
    val showTableMode = MutableStateFlow(false)
    val showBaseConverter = MutableStateFlow(false)
    val showEquationSolver = MutableStateFlow(false)
    val showDevInfo = MutableStateFlow(false)
    val showConstants = MutableStateFlow(false)
    val showUnitConverter = MutableStateFlow(false)

    // Table view state
    val tableFx = MutableStateFlow("x^2")
    val tableStart = MutableStateFlow("1")
    val tableEnd = MutableStateFlow("5")
    val tableStep = MutableStateFlow("1")
    val tableRows = MutableStateFlow<List<Pair<Double, String>>>(emptyList())
    val tableError = MutableStateFlow<String?>(null)

    // Base converter state
    val baseFrom = MutableStateFlow(10)
    val baseInput = MutableStateFlow("")
    val baseResultBin = MutableStateFlow("")
    val baseResultOct = MutableStateFlow("")
    val baseResultDec = MutableStateFlow("")
    val baseResultHex = MutableStateFlow("")
    val baseError = MutableStateFlow<String?>(null)

    // Equation Solver state
    val eqnType = MutableStateFlow(1) // 1: ax+b=0, 2: quadratic, 3: cubic, 4: 2x2, 5: 3x3
    val eqnInputs = MutableStateFlow<Map<String, String>>(emptyMap())
    val eqnResult = MutableStateFlow("")

    val showExact = MutableStateFlow(true)

    private val engine = CalculatorEngine()

    init {
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            isWelcome.value = false
            result.value = "0"
            expr.value = ""
        }
    }

    fun toggleTheme() {
        isDarkTheme.value = !isDarkTheme.value
    }

    fun toggleLive() {
        basicMode.value = !basicMode.value
        if (basicMode.value) {
            expr.value = ""
            cursorPosition.value = null
            result.value = "0"
            calcBase.value = 10
            engine.calcBase = 10
        }
    }

    fun moveCursorLeft() {
        val pos = cursorPosition.value ?: return
        if (pos > 0) {
            cursorPosition.value = pos - 1
        }
    }

    fun moveCursorRight() {
        val pos = cursorPosition.value ?: return
        if (pos < expr.value.length) {
            cursorPosition.value = pos + 1
        }
    }

    fun toggleCursor() {
        if (cursorPosition.value == null) {
            cursorPosition.value = expr.value.length
        } else {
            cursorPosition.value = null
        }
    }

    fun insertTextAtCursor(text: String) {
        val current = expr.value
        val pos = cursorPosition.value
        if (pos != null && pos in 0..current.length) {
            val updated = current.substring(0, pos) + text + current.substring(pos)
            expr.value = updated
            cursorPosition.value = pos + text.length
        } else {
            expr.value = current + text
            cursorPosition.value = null
        }
    }

    fun deleteTextAtCursor() {
        val current = expr.value
        if (current.isEmpty()) return
        val pos = cursorPosition.value
        if (pos != null && pos > 0 && pos <= current.length) {
            val updated = current.substring(0, pos - 1) + current.substring(pos)
            expr.value = updated
            cursorPosition.value = pos - 1
        } else if (pos == null) {
            expr.value = current.dropLast(1)
            cursorPosition.value = null
        }
    }

    private fun resetModifiers() {
        shift.value = false
        alpha.value = false
    }

    fun pressShift() {
        shift.value = !shift.value
        waitingForSto.value = false
    }

    fun pressAlpha() {
        alpha.value = !alpha.value
    }

    fun pressModeMenu() {
        when {
            shift.value && alpha.value -> {
                resetModifiers()
                showEquationSolver.value = true
            }
            alpha.value -> {
                resetModifiers()
                showBaseConverter.value = true
            }
            shift.value -> {
                resetModifiers()
                showTableMode.value = true
            }
            else -> {
                mode.value = if (mode.value == "DEG") "RAD" else "DEG"
                engine.mode = mode.value
                resetModifiers()
                runLiveCalculation()
            }
        }
    }

    fun pressParen() {
        if (waitingForSto.value) {
            waitingForSto.value = false
        }
        if (justCalculated.value) {
            expr.value = "Ans×("
            cursorPosition.value = if (cursorPosition.value != null) expr.value.length else null
            result.value = "0"
            justCalculated.value = false
            resetModifiers()
            return
        }
        val currentExpr = expr.value
        val pos = cursorPosition.value ?: currentExpr.length
        if (currentExpr.isNotEmpty() && pos > 0 && pos <= currentExpr.length && isImplicitMulRequired(currentExpr[pos - 1], currentExpr)) {
            insertTextAtCursor("×")
        }
        insertTextAtCursor("(")
        resetModifiers()
        runLiveCalculation()
    }

    fun pressKey(value: String) {
        if (isWelcome.value) return

        if (waitingForSto.value && alpha.value && value in listOf("A", "B", "C", "D", "E", "F")) {
            try {
                val evaluatedVal = engine.evaluate(if (expr.value.isEmpty()) result.value else expr.value)
                val updatedVars = variables.value.toMutableMap()
                updatedVars[value] = evaluatedVal
                variables.value = updatedVars
                engine.variables[value] = evaluatedVal
                result.value = "$value→${engine.formatResult(evaluatedVal)}"
                justCalculated.value = true
            } catch (e: Exception) {
                result.value = "Math ERROR"
            }
            waitingForSto.value = false
            resetModifiers()
            return
        }

        if (waitingForSto.value) {
            waitingForSto.value = false
            result.value = "0"
        }

        if (shift.value && value == "%") {
            insertTextAtCursor(",")
            resetModifiers()
            runLiveCalculation()
            return
        }

        if (justCalculated.value) {
            if (value.any { it.isDigit() } || value == "(" || value == ".") {
                expr.value = ""
                cursorPosition.value = if (cursorPosition.value != null) 0 else null
                result.value = "0"
            }
            justCalculated.value = false
        }

        val currentExpr = expr.value
        val pos = cursorPosition.value ?: currentExpr.length
        if (currentExpr.isNotEmpty() && pos > 0 && pos <= currentExpr.length) {
            val charBefore = currentExpr[pos - 1]
            val requiresMul = if (value == "(") {
                isImplicitMulRequired(charBefore, currentExpr)
            } else if (value.any { it.isDigit() }) {
                isImplicitMulRequired(charBefore, currentExpr) && !charBefore.isDigit() && charBefore != '.'
            } else {
                false
            }
            if (requiresMul) {
                insertTextAtCursor("×")
            }
        }

        insertTextAtCursor(value)
        resetModifiers()
        runLiveCalculation()
    }

    fun pressNeg() {
        if (justCalculated.value) {
            expr.value = ""
            cursorPosition.value = if (cursorPosition.value != null) 0 else null
            result.value = "0"
            justCalculated.value = false
        }

        val currentExpr = expr.value
        val pos = cursorPosition.value ?: currentExpr.length
        if (pos >= 3 && currentExpr.substring(pos - 3, pos) == "(-)") {
            val updated = currentExpr.substring(0, pos - 3) + currentExpr.substring(pos)
            expr.value = updated
            cursorPosition.value = if (cursorPosition.value != null) pos - 3 else null
        } else {
            insertTextAtCursor("(-)")
        }
        resetModifiers()
        runLiveCalculation()
    }

    fun pressOp(op: String) {
        if (waitingForSto.value) {
            waitingForSto.value = false
            result.value = "0"
        }

        if (justCalculated.value) {
            expr.value = engine.formatResult(engine.ans)
            cursorPosition.value = if (cursorPosition.value != null) expr.value.length else null
            result.value = engine.formatResult(engine.ans)
            justCalculated.value = false
        }

        val currentExpr = expr.value
        val pos = cursorPosition.value ?: currentExpr.length
        val operators = listOf('+', '-', '×', '÷')

        if (currentExpr.isNotEmpty() && pos > 0 && pos <= currentExpr.length) {
            val charBefore = currentExpr[pos - 1]

            if ((charBefore == '×' || charBefore == '÷') && op == "-") {
                insertTextAtCursor(op)
                resetModifiers()
                runLiveCalculation()
                return
            }

            if (charBefore in operators) {
                val updated = currentExpr.substring(0, pos - 1) + currentExpr.substring(pos)
                expr.value = updated
                cursorPosition.value = if (cursorPosition.value != null) pos - 1 else null
            }

            if (charBefore == '(' && op != "-") {
                resetModifiers()
                return
            }
        } else {
            if (op != "-") {
                resetModifiers()
                return
            }
        }

        insertTextAtCursor(op)
        resetModifiers()
        runLiveCalculation()
    }

    fun pressConst(c: String) {
        if (calcBase.value == 16 && !alpha.value && !shift.value && c == "π") {
            pressBaseDigit("D")
            return
        }

        if (waitingForSto.value && alpha.value && c == "π") {
            try {
                val evaluatedVal = engine.evaluate(if (expr.value.isEmpty()) result.value else expr.value)
                val updatedVars = variables.value.toMutableMap()
                updatedVars["D"] = evaluatedVal
                variables.value = updatedVars
                engine.variables["D"] = evaluatedVal
                result.value = "D→${engine.formatResult(evaluatedVal)}"
                justCalculated.value = true
            } catch (e: Exception) {
                result.value = "Math ERROR"
            }
            waitingForSto.value = false
            resetModifiers()
            return
        }

        if (justCalculated.value) {
            expr.value = "Ans"
            cursorPosition.value = if (cursorPosition.value != null) expr.value.length else null
            result.value = "Ans"
            justCalculated.value = false
        }

        val currentExpr = expr.value
        val pos = cursorPosition.value ?: currentExpr.length
        if (currentExpr.isNotEmpty() && pos > 0 && pos <= currentExpr.length && isImplicitMulRequired(currentExpr[pos - 1], currentExpr)) {
            insertTextAtCursor("×")
        }

        if (shift.value && c == "π") {
            insertTextAtCursor("e")
        } else if (alpha.value && c == "π") {
            insertTextAtCursor(variables.value["D"].toString())
        } else {
            insertTextAtCursor(if (c == "π") "π" else c)
        }
        resetModifiers()
        runLiveCalculation()
    }

    fun pressScientificConstant(symbol: String) {
        if (isWelcome.value) {
            isWelcome.value = false
            expr.value = ""
            cursorPosition.value = if (cursorPosition.value != null) 0 else null
            result.value = "0"
        }
        
        if (justCalculated.value) {
            expr.value = ""
            cursorPosition.value = if (cursorPosition.value != null) 0 else null
            result.value = "0"
            justCalculated.value = false
        }
        
        val currentExpr = expr.value
        val pos = cursorPosition.value ?: currentExpr.length
        if (currentExpr.isNotEmpty() && pos > 0 && pos <= currentExpr.length && isImplicitMulRequired(currentExpr[pos - 1], currentExpr)) {
            insertTextAtCursor("×")
        }
        
        val symbolToInsert = if (symbol == "e") "𝑒" else symbol
        insertTextAtCursor(symbolToInsert)
        runLiveCalculation()
    }

    fun pressFunc(fn: String) {
        if (shift.value && alpha.value && fn == "log") {
            if (justCalculated.value) {
                expr.value = ""
                cursorPosition.value = if (cursorPosition.value != null) 0 else null
                justCalculated.value = false
            }
            val currentExpr = expr.value
            val pos = cursorPosition.value ?: currentExpr.length
            if (currentExpr.isNotEmpty() && pos > 0 && pos <= currentExpr.length && isImplicitMulRequired(currentExpr[pos - 1], currentExpr)) {
                insertTextAtCursor("×")
            }
            insertTextAtCursor("logbase(")
            resetModifiers()
            runLiveCalculation()
            return
        }

        if (calcBase.value == 16 && !alpha.value && !shift.value) {
            val hexDigits = mapOf(
                "pow2" to "A", "pow" to "B", "sqrt" to "C", "log" to "E", "ln" to "F"
            )
            val digit = hexDigits[fn]
            if (digit != null) {
                pressBaseDigit(digit)
                return
            }
        }

        val alphaVars = mapOf(
            "pow2" to "A", "pow" to "B", "sqrt" to "C", "log" to "E", "ln" to "F"
        )

        if (waitingForSto.value && alpha.value && alphaVars.containsKey(fn)) {
            val varName = alphaVars[fn]!!
            try {
                val evaluatedVal = engine.evaluate(if (expr.value.isEmpty()) result.value else expr.value)
                val updatedVars = variables.value.toMutableMap()
                updatedVars[varName] = evaluatedVal
                variables.value = updatedVars
                engine.variables[varName] = evaluatedVal
                result.value = "$varName→${engine.formatResult(evaluatedVal)}"
                justCalculated.value = true
            } catch (e: Exception) {
                result.value = "Math ERROR"
            }
            waitingForSto.value = false
            resetModifiers()
            return
        }

        if (alpha.value && !shift.value && alphaVars.containsKey(fn)) {
            val varName = alphaVars[fn]!!
            if (justCalculated.value) {
                expr.value = ""
                cursorPosition.value = if (cursorPosition.value != null) 0 else null
                justCalculated.value = false
            }
            val currentExpr = expr.value
            val pos = cursorPosition.value ?: currentExpr.length
            if (currentExpr.isNotEmpty() && pos > 0 && pos <= currentExpr.length && isImplicitMulRequired(currentExpr[pos - 1], currentExpr)) {
                insertTextAtCursor("×")
            }
            insertTextAtCursor(variables.value[varName].toString())
            resetModifiers()
            runLiveCalculation()
            return
        }

        val noImplicitMul = listOf("pow", "pow2", "nPr", "fact")
        if (justCalculated.value && !noImplicitMul.contains(fn)) {
            expr.value = engine.formatResult(engine.ans) + "×"
            cursorPosition.value = if (cursorPosition.value != null) expr.value.length else null
            result.value = "0"
            justCalculated.value = false
        }

        val currentExpr = expr.value
        val pos = cursorPosition.value ?: currentExpr.length
        if (!noImplicitMul.contains(fn) && currentExpr.isNotEmpty() && pos > 0 && pos <= currentExpr.length && isImplicitMulRequired(currentExpr[pos - 1], currentExpr)) {
            insertTextAtCursor("×")
        }

        val funcToInsert = when {
            shift.value && alpha.value -> {
                val shiftAlphaFuncs = mapOf(
                    "sin" to "acsc(", "cos" to "asec(", "tan" to "acot("
                )
                shiftAlphaFuncs[fn] ?: "$fn("
            }
            shift.value -> {
                val shiftFuncs = mapOf(
                    "sin" to "asin(", "cos" to "acos(", "tan" to "atan(",
                    "log" to "10^", "ln" to "e^", "pow2" to "^3", "pow" to "^",
                    "sqrt" to "cbrt(", "fact" to "^(-1)", "nPr" to "C", "pol" to "Rec(", "abs" to "Rnd("
                )
                shiftFuncs[fn] ?: "$fn("
            }
            alpha.value -> {
                val alphaFuncs = mapOf(
                    "sin" to "csc(", "cos" to "sec(", "tan" to "cot(", "fact" to "fact("
                )
                alphaFuncs[fn] ?: "$fn("
            }
            else -> {
                val normalFuncs = mapOf(
                    "sin" to "sin(", "cos" to "cos(", "tan" to "tan(",
                    "log" to "log(", "ln" to "ln(", "pow2" to "^2", "pow" to "^",
                    "sqrt" to "√(", "fact" to "!", "nPr" to "P", "pol" to "Pol(", "abs" to "abs("
                )
                normalFuncs[fn] ?: "$fn("
            }
        }
        insertTextAtCursor(funcToInsert)

        resetModifiers()
        runLiveCalculation()
    }

    fun pressSD() {
        try {
            val evaluated = engine.evaluate(if (expr.value.isEmpty()) result.value else expr.value)
            if (showExact.value) {
                result.value = engine.formatResult(evaluated)
                showExact.value = false
            } else {
                val exact = engine.toExactForm(expr.value, evaluated)
                result.value = exact ?: engine.toFraction(evaluated)
                showExact.value = true
            }
        } catch (e: Exception) {
            result.value = "Math ERROR"
        }
        resetModifiers()
    }

    fun pressExp() {
        if (justCalculated.value) {
            expr.value = "Ans"
            cursorPosition.value = if (cursorPosition.value != null) expr.value.length else null
            justCalculated.value = false
        }
        insertTextAtCursor("E")
        resetModifiers()
        runLiveCalculation()
    }

    fun pressEng() {
        try {
            val valDouble = engine.evaluate(if (expr.value.isEmpty()) result.value else expr.value)
            val rawScientific = String.format(java.util.Locale.US, "%.12e", valDouble)
            val parts = rawScientific.split('e', 'E')
            if (parts.size == 2) {
                val mDouble = parts[0].toDoubleOrNull() ?: 0.0
                val exponent = parts[1].toIntOrNull() ?: 0
                val mantissa = engine.formatResult(mDouble)
                if (exponent != 0) {
                    result.value = "$mantissa×10^$exponent"
                } else {
                    result.value = mantissa
                }
            } else {
                result.value = engine.formatResult(valDouble)
            }
        } catch (e: Exception) {
            result.value = "Math ERROR"
        }
        resetModifiers()
    }

    fun pressSto() {
        if (alpha.value) {
            val updatedVars = variables.value.toMutableMap()
            updatedVars.keys.forEach { updatedVars[it] = 0.0 }
            variables.value = updatedVars
            engine.variables.keys.forEach { engine.variables[it] = 0.0 }
            result.value = "Vars Cleared"
            justCalculated.value = true
            waitingForSto.value = false
            resetModifiers()
            return
        }

        waitingForSto.value = true
        resetModifiers()
        result.value = "STO"
    }

    fun pressDel() {
        if (justCalculated.value) {
            justCalculated.value = false
        }
        deleteTextAtCursor()
        resetModifiers()
        runLiveCalculation()
    }

    fun pressAns() {
        if (justCalculated.value) {
            expr.value = ""
            cursorPosition.value = if (cursorPosition.value != null) 0 else null
            justCalculated.value = false
        }
        insertTextAtCursor("Ans")
        resetModifiers()
        runLiveCalculation()
    }

    fun pressAC() {
        if (shift.value) {
            result.value = "Clear all"
            viewModelScope.launch {
                kotlinx.coroutines.delay(1000)
                expr.value = ""
                cursorPosition.value = null
                result.value = "0"
                engine.ans = 0.0
                val clearedVars = variables.value.toMutableMap()
                clearedVars.keys.forEach { clearedVars[it] = 0.0 }
                variables.value = clearedVars
                engine.variables.keys.forEach { engine.variables[it] = 0.0 }
                mode.value = "DEG"
                calcBase.value = 10
                engine.calcBase = 10
                justCalculated.value = false
                waitingForSto.value = false
                resetModifiers()
            }
            return
        }

        expr.value = ""
        cursorPosition.value = null
        result.value = "0"
        justCalculated.value = false
        waitingForSto.value = false
        resetModifiers()
    }

    fun pressClear() {
        expr.value = ""
        cursorPosition.value = null
        result.value = "0"
        justCalculated.value = false
        resetModifiers()
    }

    fun pressBack() {
        deleteTextAtCursor()
        runLiveCalculation()
    }

    fun pressEq() {
        if (shift.value && justCalculated.value) {
            expr.value = engine.formatBaseResult(engine.ans) + "="
            result.value = "0"
            justCalculated.value = false
            resetModifiers()
            return
        }

        if (shift.value) {
            if (justCalculated.value) {
                expr.value = ""
                justCalculated.value = false
            }
            expr.value += "="
            resetModifiers()
            return
        }

        var displayExpr = expr.value.trim()
        while (displayExpr.isNotEmpty() && displayExpr.last() in listOf('+', '-', '×', '÷', '*', '/')) {
            displayExpr = displayExpr.dropLast(1).trim()
        }
        expr.value = displayExpr

        try {
            if (displayExpr.contains("=")) {
                val parts = displayExpr.split("=")
                if (parts.size != 2) throw IllegalArgumentException()
                val leftVal = engine.evaluate(parts[0])
                val rightVal = engine.evaluate(parts[1])
                val isEqual = abs(leftVal - rightVal) < 1e-10
                result.value = if (isEqual) "True" else "False"
                engine.ans = if (isEqual) 1.0 else 0.0
            } else {
                val rawVal = engine.evaluate(displayExpr)
                val exact = engine.toExactForm(displayExpr, rawVal)
                result.value = if (showExact.value && exact != null) exact else engine.formatBaseResult(rawVal)
                engine.ans = rawVal
            }

            // Save History directly to SQLite database
            val formattedDate = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            viewModelScope.launch {
                repository.insert(HistoryItem(date = formattedDate, expr = displayExpr, result = result.value))
            }

            justCalculated.value = true
        } catch (e: Exception) {
            if (e.message?.contains("Math ERROR") == true || e is ArithmeticException) {
                result.value = "Math ERROR"
            } else {
                result.value = "Syntax ERROR"
            }
        }
        resetModifiers()
    }

    fun pressBaseKey(base: Int) {
        calcBase.value = base
        engine.calcBase = base
        resetModifiers()
        runLiveCalculation()
    }

    fun pressBaseDigit(d: String) {
        if (basicMode.value) {
            pressAC()
            basicMode.value = false
        }
        if (justCalculated.value) {
            expr.value = ""
            cursorPosition.value = if (cursorPosition.value != null) 0 else null
            justCalculated.value = false
        }

        // Base input validations
        if (calcBase.value == 2 && d !in listOf("0", "1")) return
        if (calcBase.value == 8 && d !in listOf("0", "1", "2", "3", "4", "5", "6", "7")) return
        if (calcBase.value == 16 && d.uppercase() !in listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F")) return
        if (calcBase.value == 10 && d !in listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")) return

        insertTextAtCursor(d)
        runLiveCalculation()
    }

    fun pressBasePrefix(type: Int) {
        if (basicMode.value) {
            pressAC()
            basicMode.value = false
        }
        if (justCalculated.value) {
            expr.value = ""
            cursorPosition.value = if (cursorPosition.value != null) 0 else null
            justCalculated.value = false
        }
        val prefix = when (type) {
            2 -> "0b"
            8 -> "0o"
            16 -> "0x"
            else -> ""
        }
        val current = expr.value
        val pos = cursorPosition.value ?: current.length
        if (prefix.isNotEmpty()) {
            val endsWithPrefix = if (pos >= 2) {
                val sub = current.substring(pos - 2, pos)
                sub == "0b" || sub == "0o" || sub == "0x"
            } else false
            if (!endsWithPrefix) {
                insertTextAtCursor(prefix)
            }
        }
    }

    private fun isImplicitMulRequired(lastChar: Char, currentExpr: String): Boolean {
        if (calcBase.value != 10) {
            return false
        }
        if (lastChar == 'E') {
            return false
        }
        if (lastChar.isDigit() || lastChar == ')' || lastChar == 'π' || lastChar == 'e' || currentExpr.endsWith("𝑒")) {
            return true
        }
        if (lastChar.isLetter()) {
            return true
        }
        val specials = listOf('ₑ', 'ₚ', 'ₙ', 'μ', '₀', '∞', 'ₐ', 'ₑ', 'ₙ', 'ₕ', 'ₘ', '☉', '⊕', 'ħ', 'α', 'σ')
        if (lastChar in specials) {
            return true
        }
        return false
    }

    private fun runLiveCalculation() {
        if (expr.value.isEmpty() || justCalculated.value || waitingForSto.value) return
        var calcTerm = expr.value
        try {
            if (calcTerm.contains("=")) {
                val parts = calcTerm.split("=")
                if (parts.size == 2) {
                    val lVal = engine.evaluate(parts[0])
                    val rVal = engine.evaluate(parts[1])
                    val isEqual = abs(lVal - rVal) < 1e-10
                    result.value = if (isEqual) "True" else "False"
                }
            } else {
                while (calcTerm.isNotEmpty() && calcTerm.last() in listOf('+', '-', '×', '÷', '*', '/')) {
                    calcTerm = calcTerm.dropLast(1).trim()
                }
                if (calcTerm.isNotEmpty() && !calcTerm.endsWith("(") && !calcTerm.endsWith("E")) {
                    val liveResult = engine.evaluate(calcTerm)
                    result.value = engine.formatBaseResult(liveResult)
                }
            }
        } catch (e: Exception) {
            // Ignore for live preview updates
        }
    }

    fun loadFromHistory(item: HistoryItem) {
        expr.value = item.expr
        result.value = item.result
        justCalculated.value = true
        showHistory.value = false
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clear()
        }
    }

    // Generator logic
    fun generateTable() {
        val fx = tableFx.value
        val startVal = tableStart.value.toDoubleOrNull() ?: 1.0
        val endVal = tableEnd.value.toDoubleOrNull() ?: 5.0
        val stepVal = tableStep.value.toDoubleOrNull() ?: 1.0

        if (stepVal == 0.0) {
            tableError.value = "Step cannot be 0"
            return
        }
        tableError.value = null

        val list = mutableListOf<Pair<Double, String>>()
        var x = startVal
        var limit = 0
        while (x <= endVal + 1e-10) {
            if (limit++ > 50) break
            try {
                // simple evaluate with 'x' replacement
                val replaceFx = fx.replace("x", "($x)")
                val y = engine.evaluate(replaceFx)
                val exact = engine.toExactForm(replaceFx, y)
                val display = exact ?: engine.formatResult(y)
                list.add(x to display)
            } catch (e: Exception) {
                list.add(x to "Error")
            }
            x += stepVal
            // Floating point fix matching JS: x = parseFloat(x.toFixed(10))
            x = (x * 1e10).roundToLong() / 1e10
        }
        tableRows.value = list
    }

    // Converting logic
    fun convertBase() {
        val input = baseInput.value.trim().uppercase()
        val from = baseFrom.value
        if (input.isEmpty()) {
            baseError.value = "Enter a number"
            return
        }

        try {
            val validChars = mapOf(
                2 to "^[01]+$",
                8 to "^[0-7]+$",
                10 to "^[0-9]+$",
                16 to "^[0-9A-F]+$"
            )

            if (!input.matches(validChars[from]!!.toRegex())) {
                throw IllegalArgumentException("Invalid digits")
            }

            // Parse base
            val parsedValDouble = engine.parseBaseNum(input, from)
            val parsedVal = parsedValDouble.toLong()

            baseResultBin.value = java.lang.Long.toBinaryString(parsedVal)
            baseResultOct.value = java.lang.Long.toOctalString(parsedVal)
            baseResultDec.value = parsedVal.toString()
            baseResultHex.value = java.lang.Long.toHexString(parsedVal).uppercase()
            baseError.value = null
        } catch (e: Exception) {
            baseError.value = "Error: Invalid digits"
        }
    }

    // Solving System
    fun solveEquation() {
        try {
            val inputs = eqnInputs.value
            val type = eqnType.value
            when (type) {
                1 -> {
                    val a = inputs["a"]?.toDoubleOrNull() ?: 0.0
                    val b = inputs["b"]?.toDoubleOrNull() ?: 0.0
                    if (a == 0.0) throw IllegalArgumentException("a ≠ 0")
                    val x = -b / a
                    eqnResult.value = "x = ${engine.formatResult(x)}"
                }
                2 -> {
                    val a = inputs["a"]?.toDoubleOrNull() ?: 0.0
                    val b = inputs["b"]?.toDoubleOrNull() ?: 0.0
                    val c = inputs["c"]?.toDoubleOrNull() ?: 0.0
                    if (a == 0.0) throw IllegalArgumentException("a ≠ 0")
                    val disc = b * b - 4 * a * c
                    if (disc > 0) {
                        val x1 = (-b + sqrt(disc)) / (2 * a)
                        val x2 = (-b - sqrt(disc)) / (2 * a)
                        eqnResult.value = "x₁ = ${engine.formatResult(x1)}\nx₂ = ${engine.formatResult(x2)}"
                    } else if (disc == 0.0) {
                        val x = -b / (2 * a)
                        eqnResult.value = "x = ${engine.formatResult(x)}\n(Double root)"
                    } else {
                        val real = -b / (2 * a)
                        val imag = sqrt(-disc) / (2 * a)
                        eqnResult.value = "x₁ = ${engine.formatResult(real)} + ${engine.formatResult(imag)}i\nx₂ = ${engine.formatResult(real)} - ${engine.formatResult(imag)}i"
                    }
                }
                3 -> {
                    val a = inputs["a"]?.toDoubleOrNull() ?: 0.0
                    val b = inputs["b"]?.toDoubleOrNull() ?: 0.0
                    val c = inputs["c"]?.toDoubleOrNull() ?: 0.0
                    val d = inputs["d"]?.toDoubleOrNull() ?: 0.0
                    if (a == 0.0) throw IllegalArgumentException("a ≠ 0")

                    // Cardano formula for real roots approximation & solver
                    val f = ((3 * c / a) - (b * b / (a * a))) / 3.0
                    val g = ((2 * b * b * b / (a * a * a)) - (9 * b * c / (a * a)) + (27 * d / a)) / 27.0
                    val h = (g * g / 4.0) + (f * f * f / 27.0)

                    if (h > 0) {
                        val r = -(g / 2.0) + sqrt(h)
                        val s = Math.cbrt(r)
                        val t = -(g / 2.0) - sqrt(h)
                        val u = Math.cbrt(t)
                        val x1 = (s + u) - (b / (3 * a))
                        val real = -(s + u) / 2.0 - (b / (3 * a))
                        val imag = (s - u) * sqrt(3.0) / 2.0
                        eqnResult.value = "x₁ = ${engine.formatResult(x1)}\nx₂ = ${engine.formatResult(real)} + ${engine.formatResult(imag)}i\nx₃ = ${engine.formatResult(real)} - ${engine.formatResult(imag)}i"
                    } else if (f == 0.0 && g == 0.0 && h == 0.0) {
                        val x = -Math.cbrt(d / a)
                        eqnResult.value = "x = ${engine.formatResult(x)}"
                    } else {
                        val i = sqrt((g * g / 4.0) - h)
                        val j = Math.cbrt(i)
                        val k = acos(-(g / (2.0 * i)))
                        val l = -j
                        val m = cos(k / 3.0)
                        val n = sqrt(3.0) * sin(k / 3.0)
                        val p = -(b / (3.0 * a))
                        val x1 = 2.0 * j * cos(k / 3.0) + p
                        val x2 = l * (m + n) + p
                        val x3 = l * (m - n) + p
                        eqnResult.value = "x₁ = ${engine.formatResult(x1)}\nx₂ = ${engine.formatResult(x2)}\nx₃ = ${engine.formatResult(x3)}"
                    }
                }
                4 -> {
                    val a1 = inputs["a1"]?.toDoubleOrNull() ?: 0.0
                    val b1 = inputs["b1"]?.toDoubleOrNull() ?: 0.0
                    val c1 = inputs["c1"]?.toDoubleOrNull() ?: 0.0
                    val a2 = inputs["a2"]?.toDoubleOrNull() ?: 0.0
                    val b2 = inputs["b2"]?.toDoubleOrNull() ?: 0.0
                    val c2 = inputs["c2"]?.toDoubleOrNull() ?: 0.0

                    val det = a1 * b2 - a2 * b1
                    if (abs(det) < 1e-10) throw IllegalArgumentException("No unique solution")
                    val x = (c1 * b2 - c2 * b1) / det
                    val y = (a1 * c2 - a2 * c1) / det
                    eqnResult.value = "x = ${engine.formatResult(x)}\ny = ${engine.formatResult(y)}"
                }
                5 -> {
                    val a1 = inputs["a1"]?.toDoubleOrNull() ?: 0.0
                    val b1 = inputs["b1"]?.toDoubleOrNull() ?: 0.0
                    val c1 = inputs["c1"]?.toDoubleOrNull() ?: 0.0
                    val d1 = inputs["d1"]?.toDoubleOrNull() ?: 0.0

                    val a2 = inputs["a2"]?.toDoubleOrNull() ?: 0.0
                    val b2 = inputs["b2"]?.toDoubleOrNull() ?: 0.0
                    val c2 = inputs["c2"]?.toDoubleOrNull() ?: 0.0
                    val d2 = inputs["d2"]?.toDoubleOrNull() ?: 0.0

                    val a3 = inputs["a3"]?.toDoubleOrNull() ?: 0.0
                    val b3 = inputs["b3"]?.toDoubleOrNull() ?: 0.0
                    val c3 = inputs["c3"]?.toDoubleOrNull() ?: 0.0
                    val d3 = inputs["d3"]?.toDoubleOrNull() ?: 0.0

                    val det = a1 * (b2 * c3 - b3 * c2) - b1 * (a2 * c3 - a3 * c2) + c1 * (a2 * b3 - a3 * b2)
                    if (abs(det) < 1e-10) throw IllegalArgumentException("No unique solution")

                    val detX = d1 * (b2 * c3 - b3 * c2) - b1 * (d2 * c3 - d3 * c2) + c1 * (d2 * b3 - d3 * b2)
                    val detY = a1 * (d2 * c3 - d3 * c2) - d1 * (a2 * c3 - a3 * c2) + c1 * (a2 * d3 - a3 * d2)
                    val detZ = a1 * (b2 * d3 - b3 * d2) - b1 * (a2 * d3 - a3 * d2) + d1 * (a2 * b3 - a3 * b2)

                    val x = detX / det
                    val y = detY / det
                    val z = detZ / det

                    eqnResult.value = "x = ${engine.formatResult(x)}\ny = ${engine.formatResult(y)}\nz = ${engine.formatResult(z)}"
                }
            }
        } catch (e: Exception) {
            eqnResult.value = "Error: " + (e.message ?: "Invalid parameters")
        }
    }

    fun getAnsValue(): Double {
        return engine.ans
    }

    fun insertValueToExpression(value: String) {
        if (justCalculated.value) {
            expr.value = value
            cursorPosition.value = if (cursorPosition.value != null) value.length else null
            justCalculated.value = false
        } else {
            insertTextAtCursor(value)
        }
        runLiveCalculation()
    }
}
