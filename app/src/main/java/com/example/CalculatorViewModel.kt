package com.example

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.floor

/**
 * Data model representing an entry in the calculator's historical calculation log.
 */
data class HistoryItem(
    val expression: String,
    val result: String
)

/**
 * Highly deterministic ViewModel managing the state of the calculator.
 * Strictly isolates math evaluation and UI state changes from the rendering controller.
 */
class CalculatorViewModel : ViewModel() {

    // Current expression being entered by the user
    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression.asStateFlow()

    // Real-time evaluation preview of the active expression
    private val _realtimeResult = MutableStateFlow("")
    val realtimeResult: StateFlow<String> = _realtimeResult.asStateFlow()

    // Flag representing whether we just performed an '=' evaluation.
    // If true, the next input will overwrite the display instead of appending (unless it is an operator).
    private var isEvaluatedState = false

    // Historical list of completed expressions and results
    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    // Degree vs Radian toggle state
    private val _isDegreeMode = MutableStateFlow(true)
    val isDegreeMode: StateFlow<Boolean> = _isDegreeMode.asStateFlow()

    // Inverse functions toggle state (e.g. sin -> sin⁻¹)
    private val _isInverseMode = MutableStateFlow(false)
    val isInverseMode: StateFlow<Boolean> = _isInverseMode.asStateFlow()

    // Light vs Dark mode theme state
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    /**
     * Toggles between Degree and Radian modes.
     */
    fun toggleDegreeMode() {
        _isDegreeMode.value = !_isDegreeMode.value
        // Recalculate preview in the new trigonometric mode
        updateRealtimePreview(_expression.value)
    }

    /**
     * Toggles between standard and inverse scientific modes.
     */
    fun toggleInverseMode() {
        _isInverseMode.value = !_isInverseMode.value
    }

    /**
     * Toggles between Dark Mode and Light Mode themes.
     */
    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    /**
     * Appends a character or token to the active expression.
     */
    fun onKeyPress(key: String) {
        val current = _expression.value

        // Handle numeric or basic key input
        when (key) {
            "AC" -> {
                _expression.value = ""
                _realtimeResult.value = ""
                isEvaluatedState = false
            }
            "DEL" -> {
                _expression.value = deleteLastToken(current)
                isEvaluatedState = false
                updateRealtimePreview(_expression.value)
            }
            "=" -> {
                evaluateAndCommit()
            }
            else -> {
                // Determine whether to overwrite or append
                val isOperator = key in listOf("+", "-", "×", "÷", "^", "!", "nPr", "nCr")
                if (isEvaluatedState) {
                    isEvaluatedState = false
                    if (isOperator) {
                        // If it's an operator, chain it to the previous result
                        _expression.value = _realtimeResult.value + formatKeyForExpression(key)
                    } else {
                        // Otherwise, start a fresh expression
                        _expression.value = formatKeyForExpression(key)
                    }
                } else {
                    _expression.value = current + formatKeyForExpression(key)
                }
                updateRealtimePreview(_expression.value)
            }
        }
    }

    /**
     * Pulls a previous historical calculation's result into the current active expression.
     */
    fun selectHistoryItem(item: HistoryItem) {
        if (isEvaluatedState || _expression.value.isEmpty()) {
            _expression.value = item.result
            isEvaluatedState = false
        } else {
            _expression.value += item.result
        }
        updateRealtimePreview(_expression.value)
    }

    /**
     * Clears all calculation history.
     */
    fun clearHistory() {
        _history.value = emptyList()
    }

    /**
     * Helper to map clicked UI keys into corresponding math syntax strings.
     */
    private fun formatKeyForExpression(key: String): String {
        return when (key) {
            "sin" -> if (_isInverseMode.value) "sin⁻¹(" else "sin("
            "cos" -> if (_isInverseMode.value) "cos⁻¹(" else "cos("
            "tan" -> if (_isInverseMode.value) "tan⁻¹(" else "tan("
            "sinh" -> if (_isInverseMode.value) "sinh⁻¹(" else "sinh("
            "cosh" -> if (_isInverseMode.value) "cosh⁻¹(" else "cosh("
            "tanh" -> if (_isInverseMode.value) "tanh⁻¹(" else "tanh("
            "log" -> if (_isInverseMode.value) "10^(" else "log("
            "ln" -> if (_isInverseMode.value) "e^(" else "ln("
            "√" -> "√("
            "x²" -> "²"
            "xʸ" -> if (_isInverseMode.value) "^(1/" else "^"
            "eˣ" -> "e^("
            "x!" -> "!"
            "nPr" -> "nPr"
            "nCr" -> "nCr"
            else -> key
        }
    }

    /**
     * Custom smart deletion rule: deletes full scientific function tokens rather than just individual letters.
     */
    private fun deleteLastToken(expr: String): String {
        if (expr.isEmpty()) return ""
        val tokens = listOf(
            "sin⁻¹(", "cos⁻¹(", "tan⁻¹(", "sinh⁻¹(", "cosh⁻¹(", "tanh⁻¹(",
            "sinh(", "cosh(", "tanh(", "sin(", "cos(", "tan(", "log(", "ln(", "√(", "e^(", "nPr", "nCr", "10^(", "^(1/"
        )
        for (token in tokens) {
            if (expr.endsWith(token)) {
                return expr.substring(0, expr.length - token.length)
            }
        }
        return expr.substring(0, expr.length - 1)
    }

    /**
     * Real-time calculation helper. Performs auto-closing of brackets to evaluate as the user types.
     */
    private fun updateRealtimePreview(expr: String) {
        if (expr.isEmpty()) {
            _realtimeResult.value = ""
            return
        }
        try {
            val closedExpr = autoCloseParentheses(expr)
            val parser = CalculatorParser(_isDegreeMode.value)
            val rawValue = parser.parse(closedExpr)
            _realtimeResult.value = formatResult(rawValue)
        } catch (e: Exception) {
            // Silence evaluation errors on intermediate inputs
            _realtimeResult.value = ""
        }
    }

    /**
     * Performs formal expression parsing and evaluation, then pushes the result to the history log.
     */
    private fun evaluateAndCommit() {
        val expr = _expression.value
        if (expr.isEmpty()) return

        try {
            val closedExpr = autoCloseParentheses(expr)
            val parser = CalculatorParser(_isDegreeMode.value)
            val rawValue = parser.parse(closedExpr)
            val finalResult = formatResult(rawValue)

            // Commit to history list
            val updatedHistory = _history.value.toMutableList()
            updatedHistory.add(HistoryItem(expr, finalResult))
            _history.value = updatedHistory

            // Commit to screen display state
            _expression.value = expr // keeps the full input displayed as the expression line
            _realtimeResult.value = finalResult
            isEvaluatedState = true

        } catch (e: Exception) {
            _realtimeResult.value = "Error"
            isEvaluatedState = true
        }
    }

    /**
     * Appends matching closing parenthesis to ensure parsing works on unclosed inputs.
     */
    private fun autoCloseParentheses(expression: String): String {
        var count = 0
        for (char in expression) {
            if (char == '(') count++
            else if (char == ')') count--
        }
        var normalized = expression
        if (count > 0) {
            normalized += ")".repeat(count)
        }
        return normalized
    }

    /**
     * Formats numerical output with high visual polish, utilizing scientific notation for extreme numbers
     * and trimming unnecessary trailing zeroes.
     */
    private fun formatResult(value: Double): String {
        if (value.isInfinite()) return "Infinity"
        if (value.isNaN()) return "Error"

        // For extremely large or incredibly tiny floating values, use formatted scientific notation
        if (abs(value) >= 1e12 || (abs(value) > 0.0 && abs(value) < 1e-6)) {
            return String.format(java.util.Locale.US, "%.6e", value)
                .replace("e+", "e")
                .replace("e0", "e")
        }

        // Return integer string if there is no decimal component
        if (value == floor(value) && value in -9e15..9e15) {
            return value.toLong().toString()
        }

        // Return formatted double value rounded up to 10 decimal digits
        val str = String.format(java.util.Locale.US, "%.10f", value)
        var trimmed = str
        if (trimmed.contains(".")) {
            while (trimmed.endsWith("0")) {
                trimmed = trimmed.substring(0, trimmed.length - 1)
            }
            if (trimmed.endsWith(".")) {
                trimmed = trimmed.substring(0, trimmed.length - 1)
            }
        }
        return trimmed
    }
}
