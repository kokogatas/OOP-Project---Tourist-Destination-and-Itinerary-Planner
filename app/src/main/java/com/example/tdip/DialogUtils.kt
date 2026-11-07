package com.example.tdip

import android.content.Context
import android.graphics.Color
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

object DialogUtils {

    // Shows a multi-line, scrollable dialog so long error messages never get cropped or truncated!
    fun showFullErrorDialog(context: Context, title: String, message: String) {
        val textView = TextView(context).apply {
            text = message
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(40, 30, 40, 30)
        }

        val scrollView = ScrollView(context).apply {
            addView(textView)
        }

        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(scrollView)
            .setPositiveButton("OK", null)
            .show()
    }
}
