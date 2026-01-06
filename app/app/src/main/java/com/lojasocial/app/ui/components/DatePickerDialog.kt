package com.lojasocial.app.ui.components

import android.app.DatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.lojasocial.app.R
import java.util.Calendar

/**
 * Reusable DatePickerDialog component
 * 
 * This component wraps Android's native DatePickerDialog and applies the custom theme
 * defined in themes.xml. It can be used anywhere in the app for date selection.
 * 
 * @param showDialog Whether to show the date picker dialog
 * @param onDateSelected Callback invoked when a date is selected. Parameters: (day: Int, month: Int, year: Int)
 *                       Note: month is 1-based (1 = January, 12 = December)
 * @param onDismiss Callback invoked when the dialog is dismissed
 * @param initialYear Optional initial year to display (defaults to current year)
 * @param initialMonth Optional initial month to display (1-based, defaults to current month)
 * @param initialDay Optional initial day to display (defaults to current day)
 * @param minDate Optional minimum date that can be selected (milliseconds since epoch)
 * @param maxDate Optional maximum date that can be selected (milliseconds since epoch)
 */
@Composable
fun CustomDatePickerDialog(
    showDialog: Boolean,
    onDateSelected: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit,
    initialYear: Int? = null,
    initialMonth: Int? = null,
    initialDay: Int? = null,
    minDate: Long? = null,
    maxDate: Long? = null
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    
    val year = initialYear ?: calendar.get(Calendar.YEAR)
    val month = (initialMonth ?: (calendar.get(Calendar.MONTH) + 1)) - 1 // Convert to 0-based
    val day = initialDay ?: calendar.get(Calendar.DAY_OF_MONTH)
    
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            R.style.CustomDatePickerDialog, // Apply the custom theme
            { _, selectedYear, selectedMonth, selectedDay ->
                // selectedMonth is 0-based, convert to 1-based for callback
                onDateSelected(selectedDay, selectedMonth + 1, selectedYear)
            },
            year,
            month,
            day
        ).apply {
            // Set min/max dates if provided
            minDate?.let { this.datePicker.minDate = it }
            maxDate?.let { this.datePicker.maxDate = it }
            
            // Handle dismiss callback
            setOnCancelListener {
                onDismiss()
            }
        }
    }
    
    // Show/hide dialog based on showDialog state
    LaunchedEffect(showDialog) {
        if (showDialog && !datePickerDialog.isShowing) {
            datePickerDialog.show()
        } else if (!showDialog && datePickerDialog.isShowing) {
            datePickerDialog.dismiss()
        }
    }
    
    // Clean up when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            if (datePickerDialog.isShowing) {
                datePickerDialog.dismiss()
            }
        }
    }
}

