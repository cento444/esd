package com.example.ui.util

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

object AppKeyboards {
    val Text = KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences,
        keyboardType = KeyboardType.Text
    )

    val Words = KeyboardOptions(
        capitalization = KeyboardCapitalization.Words,
        keyboardType = KeyboardType.Text
    )

    val Number = KeyboardOptions(
        keyboardType = KeyboardType.Number
    )

    val Decimal = KeyboardOptions(
        keyboardType = KeyboardType.Decimal
    )

    val Phone = KeyboardOptions(
        keyboardType = KeyboardType.Phone
    )

    val Time = KeyboardOptions(
        keyboardType = KeyboardType.Number
    )

    val Date = KeyboardOptions(
        keyboardType = KeyboardType.Number
    )
}

fun formatTimeInput(input: String): String {
    if (input.isEmpty()) return ""
    if (input.contains(":")) {
        val parts = input.split(":")
        val hours = parts.getOrNull(0)?.filter { it.isDigit() }?.take(2) ?: ""
        val mins = parts.getOrNull(1)?.filter { it.isDigit() }?.take(2) ?: ""
        val hasTrailingColon = input.endsWith(":") && parts.size <= 2
        return if (mins.isNotEmpty()) {
            "$hours:$mins"
        } else if (hasTrailingColon || parts.size > 1) {
            "$hours:"
        } else {
            hours
        }
    }
    val digits = input.filter { it.isDigit() }.take(4)
    return when {
        digits.length == 4 -> "${digits.substring(0, 2)}:${digits.substring(2, 4)}"
        else -> digits
    }
}

fun formatDateInput(input: String): String {
    if (input.isEmpty()) return ""
    if (input.contains("/")) {
        val parts = input.split("/")
        val day = parts.getOrNull(0)?.filter { it.isDigit() }?.take(2) ?: ""
        val month = parts.getOrNull(1)?.filter { it.isDigit() }?.take(2) ?: ""
        val year = parts.getOrNull(2)?.filter { it.isDigit() }?.take(4) ?: ""
        val sb = StringBuilder(day)
        if (parts.size > 1 || input.endsWith("/")) {
            sb.append("/")
            sb.append(month)
            if (parts.size > 2 || (parts.size == 2 && input.endsWith("/") && month.isNotEmpty())) {
                sb.append("/")
                sb.append(year)
            }
        }
        return sb.toString()
    }
    val digits = input.filter { it.isDigit() }.take(8)
    return when {
        digits.length > 4 -> "${digits.substring(0, 2)}/${digits.substring(2, 4)}/${digits.substring(4)}"
        digits.length > 2 -> "${digits.substring(0, 2)}/${digits.substring(2)}"
        else -> digits
    }
}

fun showAndroidTimePicker(
    context: android.content.Context,
    initialTime: String,
    onTimeSelected: (String) -> Unit
) {
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    android.app.TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val formatted = "%02d:%02d".format(hourOfDay, minute)
            onTimeSelected(formatted)
        },
        initialHour,
        initialMinute,
        true
    ).show()
}

fun formatDateToDisplay(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return ""
    val trimmed = dateStr.trim()
    // If format is yyyy-MM-dd
    val hyphenParts = trimmed.split("-")
    if (hyphenParts.size == 3 && hyphenParts[0].length == 4) {
        val y = hyphenParts[0]
        val m = hyphenParts[1].padStart(2, '0')
        val d = hyphenParts[2].padStart(2, '0')
        return "$d/$m/$y"
    }
    // If format is yyyy/MM/dd
    val slashParts = trimmed.split("/")
    if (slashParts.size == 3 && slashParts[0].length == 4) {
        val y = slashParts[0]
        val m = slashParts[1].padStart(2, '0')
        val d = slashParts[2].padStart(2, '0')
        return "$d/$m/$y"
    }
    return trimmed
}

fun showAndroidDatePicker(
    context: android.content.Context,
    initialDateKey: String, // format yyyy-MM-dd or dd/MM/yyyy
    onDateSelected: (String) -> Unit
) {
    val cal = java.util.Calendar.getInstance()
    if (initialDateKey.isNotBlank()) {
        try {
            if (initialDateKey.contains("-")) {
                val parts = initialDateKey.split("-")
                if (parts.size == 3 && parts[0].length == 4) {
                    val y = parts[0].toInt()
                    val m = parts[1].toInt() - 1
                    val d = parts[2].toInt()
                    cal.set(y, m, d)
                }
            } else if (initialDateKey.contains("/")) {
                val parts = initialDateKey.split("/")
                if (parts.size == 3) {
                    if (parts[0].length == 4) {
                        // yyyy/MM/dd
                        cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                    } else if (parts[2].length == 4) {
                        // dd/MM/yyyy
                        cal.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
                    }
                }
            }
        } catch (_: Exception) {}
    }

    android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formatted = String.format(java.util.Locale.ROOT, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            onDateSelected(formatted)
        },
        cal.get(java.util.Calendar.YEAR),
        cal.get(java.util.Calendar.MONTH),
        cal.get(java.util.Calendar.DAY_OF_MONTH)
    ).show()
}

