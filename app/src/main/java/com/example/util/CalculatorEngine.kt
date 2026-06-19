package com.example.util

import kotlin.math.*

class CalculatorEngine(
    var mode: String = "DEG", // DEG or RAD
    var calcBase: Int = 10,  // 2, 8, 10, 16
    var ans: Double = 0.0,
    var variables: MutableMap<String, Double> = mutableMapOf(
        "A" to 0.0, "B" to 0.0, "C" to 0.0, "D" to 0.0, "E" to 0.0, "F" to 0.0
    ),
    var polToggle: Boolean = false,
    var polLastX: Double = 0.0,
    var polLastY: Double = 0.0,
    var recToggle: Boolean = false,
    var recLastR: Double = 0.0,
    var recLastT: Double = 0.0
) {
    // Exact value tables, representing standard trig angles matching JS
    private val exactValuesDEG = mapOf(
        "sin(0)" to "0", "sin(30)" to "1/2", "sin(45)" to "1/√2", "sin(60)" to "√3/2", "sin(90)" to "1",
        "sin(120)" to "√3/2", "sin(135)" to "1/√2", "sin(150)" to "1/2", "sin(180)" to "0",
        "cos(0)" to "1", "cos(30)" to "√3/2", "cos(45)" to "1/√2", "cos(60)" to "1/2", "cos(90)" to "0",
        "cos(120)" to "-1/2", "cos(135)" to "-1/√2", "cos(150)" to "-√3/2", "cos(180)" to "-1",
        "tan(0)" to "0", "tan(30)" to "1/√3", "tan(45)" to "1", "tan(60)" to "√3",
        "tan(120)" to "-√3", "tan(135)" to "-1", "tan(150)" to "-1/√3", "tan(180)" to "0",
        "csc(30)" to "2", "csc(45)" to "√2", "csc(60)" to "2/√3", "csc(90)" to "1",
        "csc(120)" to "2/√3", "csc(135)" to "√2", "csc(150)" to "2",
        "sec(0)" to "1", "sec(30)" to "2/√3", "sec(45)" to "√2", "sec(60)" to "2",
        "sec(120)" to "-2", "sec(135)" to "-√2", "sec(150)" to "-2/√3", "sec(180)" to "-1",
        "cot(30)" to "√3", "cot(45)" to "1", "cot(60)" to "1/√3",
        "cot(120)" to "-1/√3", "cot(135)" to "-1", "cot(150)" to "-√3"
    )

    private val exactValuesRAD = mapOf(
        "sin(0)" to "0", "sin(π/6)" to "1/2", "sin(π/4)" to "1/2", "sin(π/3)" to "3/2", "sin(π/2)" to "1",
        "sin(2π/3)" to "3/2", "sin(3π/4)" to "1/2", "sin(5π/6)" to "1/2", "sin(π)" to "0",
        "cos(0)" to "1", "cos(π/6)" to "3/2", "cos(π/4)" to "1/2", "cos(π/3)" to "1/2", "cos(π/2)" to "0",
        "cos(2π/3)" to "-1/2", "cos(3π/4)" to "-1/2", "cos(5π/6)" to "-3/2", "cos(π)" to "-1",
        "tan(0)" to "0", "tan(π/6)" to "1/3", "tan(π/4)" to "1", "tan(π/3)" to "3",
        "tan(2π/3)" to "-3", "tan(3π/4)" to "-1", "tan(5π/6)" to "-1/3", "tan(π)" to "0",
        "csc(π/6)" to "2", "csc(π/4)" to "2", "csc(π/3)" to "2/3", "csc(π/2)" to "1",
        "csc(2π/3)" to "2/3", "csc(3π/4)" to "2", "csc(5π/6)" to "2",
        "sec(0)" to "1", "sec(π/6)" to "2/3", "sec(π/4)" to "2", "sec(π/3)" to "2",
        "sec(2π/3)" to "-2", "sec(3π/4)" to "-2", "sec(5π/6)" to "-2/3", "sec(π)" to "-1",
        "cot(π/6)" to "3", "cot(π/4)" to "1", "cot(π/3)" to "1/3",
        "cot(2π/3)" to "-1/3", "cot(3π/4)" to "-1", "cot(5π/6)" to "-3"
    )

    // Parses clean values string based on key mapping representation
    fun parseBaseNum(numStr: String, base: Int): Double {
        if (!numStr.contains(".")) {
            return numStr.toLong(base).toDouble()
        }
        val parts = numStr.split(".")
        val intPart = parts[0]
        val fracPart = parts.getOrNull(1) ?: ""
        val intVal = if (intPart.isEmpty()) 0.0 else intPart.toLong(base).toDouble()
        var fracVal = 0.0
        for (i in fracPart.indices) {
            val digitChar = fracPart[i]
            val digit = if (digitChar in '0'..'9') {
                digitChar - '0'
            } else {
                digitChar.uppercaseChar() - 'A' + 10
            }
            if (digit < 0 || digit >= base) throw IllegalArgumentException("Invalid digit")
            fracVal += digit.toDouble() / base.toDouble().pow(i + 1)
        }
        return intVal + fracVal
    }

    fun formatBaseResult(value: Double): String {
        if (!value.isFinite()) return "Math ERROR"
        return when (calcBase) {
            2 -> {
                val isNegative = value < 0
                val absVal = abs(value)
                val intPart = absVal.toLong()
                val binInt = java.lang.Long.toBinaryString(intPart)
                val fracPart = absVal - intPart
                if (fracPart < 1e-10) {
                    (if (isNegative) "-" else "") + binInt
                } else {
                    var fracStr = ""
                    var rem = fracPart
                    for (i in 0..10) {
                        rem *= 2
                        val bit = rem.toInt()
                        fracStr += bit
                        rem -= bit
                        if (rem < 1e-10) break
                    }
                    (if (isNegative) "-" else "") + "$binInt.$fracStr"
                }
            }
            8 -> {
                val isNegative = value < 0
                val absVal = abs(value)
                val intPart = absVal.toLong()
                val octInt = java.lang.Long.toOctalString(intPart)
                val fracPart = absVal - intPart
                if (fracPart < 1e-10) {
                    (if (isNegative) "-" else "") + octInt
                } else {
                    var fracStr = ""
                    var rem = fracPart
                    for (i in 0..10) {
                        rem *= 8
                        val d = rem.toInt()
                        fracStr += d
                        rem -= d
                        if (rem < 1e-10) break
                    }
                    (if (isNegative) "-" else "") + "$octInt.$fracStr"
                }
            }
            16 -> {
                val isNegative = value < 0
                val absVal = abs(value)
                val intPart = absVal.toLong()
                val hexInt = java.lang.Long.toHexString(intPart).uppercase()
                val fracPart = absVal - intPart
                if (fracPart < 1e-10) {
                    (if (isNegative) "-" else "") + hexInt
                } else {
                    var fracStr = ""
                    var rem = fracPart
                    for (i in 0..10) {
                        rem *= 16
                        val bit = rem.toInt()
                        val bitChar = if (bit < 10) ('0' + bit) else ('A' + (bit - 10))
                        fracStr += bitChar
                        rem -= bit
                        if (rem < 1e-10) break
                    }
                    (if (isNegative) "-" else "") + "$hexInt.$fracStr"
                }
            }
            else -> formatResult(value)
        }
    }

    fun formatResult(num: Double): String {
        if (!num.isFinite()) return "Math ERROR"
        if (num % 1 == 0.0) return num.toLong().toString()
        if (abs(num - num.roundToLong()) < 1e-12) return num.roundToLong().toString()
        val fixed = String.format("%.10f", num).trimEnd('0').trimEnd('.')
        if (abs(num) < 10000 && abs(num) > 0.0001) {
            return fixed
        } else {
            // Normal scientific notation format without extra signs
            return String.format("%.6e", num).replace("e+0", "e").replace("e-0", "e-")
        }
    }

    fun toExactForm(exprStr: String, value: Double): String? {
        val clean = exprStr.replace(" ", "").lowercase()
        if (mode == "DEG" && exactValuesDEG.containsKey(clean)) {
            return exactValuesDEG[clean]
        }
        if (mode == "RAD" && exactValuesRAD.containsKey(clean)) {
            val v = exactValuesRAD[clean]
            if (v != null) return v
        }
        if (mode == "RAD") {
            if (abs(value - Math.PI / 6) < 1e-10) return "π/6"
            if (abs(value - Math.PI / 4) < 1e-10) return "π/4"
            if (abs(value - Math.PI / 3) < 1e-10) return "π/3"
            if (abs(value - Math.PI / 2) < 1e-10) return "π/2"
            if (abs(value) < 1e-10) return "0"
        }
        if (abs(value - 0.5) < 1e-10) return "1/2"
        if (abs(value - 0.70710678118) < 1e-9) return "1/√2"
        if (abs(value - 0.86602540378) < 1e-9) return "√3/2"
        return null
    }

    fun toFraction(dec: Double): String {
        if (dec % 1 == 0.0) return dec.toLong().toString()
        val sign = if (dec < 0) "-" else ""
        val absDec = abs(dec)
        val tolerance = 1.0e-9
        var h1 = 1L
        var h2 = 0L
        var k1 = 0L
        var k2 = 1L
        var b = absDec
        do {
            val a = b.toLong()
            var aux = h1
            h1 = a * h1 + h2
            h2 = aux
            aux = k1
            k1 = a * k1 + k2
            k2 = aux
            val diff = b - a
            if (abs(diff) < tolerance) break
            b = 1.0 / diff
        } while (abs(absDec - h1.toDouble() / k1) > absDec * tolerance && k1 < 10000)
        return "$sign$h1/$k1"
    }

    fun factorial(n: Double): Double {
        if (n < 0 || n % 1 != 0.0) throw IllegalArgumentException("Math ERROR")
        if (n > 170) return Double.POSITIVE_INFINITY
        val nInt = n.toInt()
        var r = 1.0
        for (i in 2..nInt) {
            r *= i
        }
        return r
    }

    fun evaluate(expr: String): Double {
        var preprocessed = expr
            .replace("Ans", ans.toString())
            .replace("π", Math.PI.toString())
            .replace("e", Math.E.toString())

        // Substitute variables A, B, C, D, E, F
        variables.forEach { (name, value) ->
            preprocessed = preprocessed.replace("\\b$name\\b".toRegex(), value.toString())
        }

        // Unary minus representation in clean JS replace: replace(/\(\-\)(?![0-9(])/g, '(-)1') and replace(/\(\-\)/g, '(0-1)*')
        preprocessed = preprocessed.replace("(-)", "(0-1)*")

        // Map basic characters log_y(x) or nPr(n, r) or nCr(n, r) if using custom button logic
        // Format of nPr/nCr representation in styled UI expression: e.g., 5P3 becomes nPr(5,3)
        // Regex replacement for \d+P\d+ and \d+C\d+
        val pRegex = "(\\d+)P(\\d+)".toRegex()
        while (preprocessed.contains(pRegex)) {
            preprocessed = preprocessed.replace(pRegex, "nPr($1,$2)")
        }
        val cRegex = "(\\d+)C(\\d+)".toRegex()
        while (preprocessed.contains(cRegex)) {
            preprocessed = preprocessed.replace(cRegex, "nCr($1,$2)")
        }

        // Implicit multiplications: e.g. "2(3)" -> "2*(3)", "2tan(30)" -> "2*tan(30)", "πsin(x)" -> "π*sin(x)", ")(" -> ")*("
        preprocessed = preprocessed.replace("(\\d|\\)|π|e)(?=[a-zA-Z√∛\\(])".toRegex(), "$1*")
        preprocessed = preprocessed.replace("\\)(?=\\d|π|e)".toRegex(), ")*")
        
        return Parser(preprocessed, this).parse()
    }

    private class Parser(val input: String, val engine: CalculatorEngine) {
        var pos = -1
        var ch = ' '

        fun nextChar() {
            pos++
            ch = if (pos < input.length) input[pos] else '\u0000'
        }

        fun eat(charToEat: Char): Boolean {
            while (ch == ' ') nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < input.length) throw IllegalArgumentException("Syntax ERROR: Unexpected '$ch'")
            return x
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+')) x += parseTerm()
                else if (eat('-')) x -= parseTerm()
                else break
            }
            return x
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('×') || eat('*')) x *= parseFactor()
                else if (eat('÷') || eat('/')) x /= parseFactor()
                else break
            }
            return x
        }

        fun parseFactor(): Double {
            if (eat('+')) return parseFactor()
            if (eat('-')) return -parseFactor()

            var x: Double
            val startPos = this.pos
            if (eat('(')) {
                x = parseExpression()
                eat(')')
            } else if (ch in '0'..'9' || ch == '.') {
                while (ch in '0'..'9' || ch == '.' || (engine.calcBase == 16 && ch.uppercaseChar() in 'A'..'F')) {
                    nextChar()
                }
                val numStr = input.substring(startPos, this.pos)
                x = if (engine.calcBase != 10) {
                    engine.parseBaseNum(numStr, engine.calcBase)
                } else {
                    numStr.toDouble()
                }
            } else if (ch.isLetter() || ch == '√' || ch == '∛') {
                if (ch == '√') {
                    nextChar()
                    eat('(')
                    x = parseExpression()
                    eat(')')
                    x = sqrt(x)
                } else if (ch == '∛') {
                    nextChar()
                    eat('(')
                    x = parseExpression()
                    eat(')')
                    x = Math.cbrt(x)
                } else {
                    while (ch.isLetter() || ch.isDigit()) nextChar()
                    val func = input.substring(startPos, this.pos)
                    if (eat('(')) {
                        val args = mutableListOf<Double>()
                        args.add(parseExpression())
                        while (eat(',')) {
                            args.add(parseExpression())
                        }
                        eat(')')

                        x = when (func) {
                            "sin" -> sin(toRad(args[0]))
                            "cos" -> cos(toRad(args[0]))
                            "tan" -> {
                                val rad = if (engine.mode == "DEG") toRad(args[0]) else args[0]
                                if (abs(cos(rad)) < 1e-12) {
                                    throw ArithmeticException("Math ERROR")
                                }
                                tan(rad)
                            }
                            "csc" -> {
                                val rad = if (engine.mode == "DEG") toRad(args[0]) else args[0]
                                if (abs(sin(rad)) < 1e-12) {
                                    throw ArithmeticException("Math ERROR")
                                }
                                1.0 / sin(rad)
                            }
                            "sec" -> {
                                val rad = if (engine.mode == "DEG") toRad(args[0]) else args[0]
                                if (abs(cos(rad)) < 1e-12) {
                                    throw ArithmeticException("Math ERROR")
                                }
                                1.0 / cos(rad)
                            }
                            "cot" -> {
                                val rad = if (engine.mode == "DEG") toRad(args[0]) else args[0]
                                if (abs(sin(rad)) < 1e-12) {
                                    throw ArithmeticException("Math ERROR")
                                }
                                1.0 / tan(rad)
                            }
                            "asin" -> {
                                val v = args[0]
                                if (v < -1.0 || v > 1.0) throw IllegalArgumentException("Math ERROR")
                                fromRad(asin(v))
                            }
                            "acos" -> {
                                val v = args[0]
                                if (v < -1.0 || v > 1.0) throw IllegalArgumentException("Math ERROR")
                                fromRad(acos(v))
                            }
                            "atan" -> fromRad(atan(args[0]))
                            "acsc" -> {
                                val v = args[0]
                                if (v == 0.0 || (1.0 / v) < -1.0 || (1.0 / v) > 1.0) throw IllegalArgumentException("Math ERROR")
                                fromRad(asin(1.0 / v))
                            }
                            "asec" -> {
                                val v = args[0]
                                if (v == 0.0 || (1.0 / v) < -1.0 || (1.0 / v) > 1.0) throw IllegalArgumentException("Math ERROR")
                                fromRad(acos(1.0 / v))
                            }
                            "acot" -> {
                                val v = args[0]
                                if (v == 0.0) throw IllegalArgumentException("Math ERROR")
                                fromRad(atan(1.0 / v))
                            }
                            "log" -> {
                                val v = args[0]
                                if (v <= 0.0) throw IllegalArgumentException("Math ERROR")
                                log10(v)
                            }
                            "ln" -> {
                                val v = args[0]
                                if (v <= 0.0) throw IllegalArgumentException("Math ERROR")
                                ln(v)
                            }
                            "logbase" -> {
                                val y = args[0]
                                val v = args[1]
                                if (v <= 0 || y <= 0 || y == 1.0) throw IllegalArgumentException("Math ERROR")
                                ln(v) / ln(y)
                            }
                            "abs" -> abs(args[0])
                            "cbrt" -> Math.cbrt(args[0])
                            "Rnd" -> {
                                val v = args[0]
                                (v * 1e9).roundToLong() / 1e9
                            }
                            "factorial", "fact" -> engine.factorial(args[0])
                            "nPr" -> {
                                val n = args[0]
                                val r = args[1]
                                engine.factorial(n) / engine.factorial(n - r)
                            }
                            "nCr" -> {
                                val n = args[0]
                                val r = args[1]
                                engine.factorial(n) / (engine.factorial(r) * engine.factorial(n - r))
                            }
                            "Pol" -> {
                                val xVal = args[0]
                                val yVal = args[1]
                                if (!engine.polToggle) {
                                    engine.polToggle = true
                                    engine.polLastX = xVal
                                    engine.polLastY = yVal
                                    sqrt(xVal * xVal + yVal * yVal)
                                } else {
                                    engine.polToggle = false
                                    fromRad(atan2(engine.polLastY, engine.polLastX))
                                }
                            }
                            "Rec" -> {
                                val rVal = args[0]
                                val tVal = args[1]
                                if (!engine.recToggle) {
                                    engine.recToggle = true
                                    engine.recLastR = rVal
                                    engine.recLastT = tVal
                                    rVal * cos(toRad(tVal))
                                } else {
                                    engine.recToggle = false
                                    engine.recLastR * sin(toRad(engine.recLastT))
                                }
                            }
                            else -> throw IllegalArgumentException("Syntax ERROR")
                        }
                    } else {
                        x = when (func) {
                            "Ans" -> engine.ans
                            else -> throw IllegalArgumentException("Syntax ERROR")
                        }
                    }
                }
            } else {
                throw IllegalArgumentException("Syntax ERROR")
            }

            // check exponent or factorial postfixes
            if (eat('^')) {
                val exponent = parseFactor()
                x = x.pow(exponent)
            }
            if (eat('!')) {
                x = engine.factorial(x)
            }

            return x
        }

        private fun toRad(angle: Double): Double {
            return if (engine.mode == "DEG") angle * Math.PI / 180.0 else angle
        }

        private fun fromRad(rad: Double): Double {
            return if (engine.mode == "DEG") rad * 180.0 / Math.PI else rad
        }
    }
}
