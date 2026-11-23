package com.example.tdip

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Locale

class AdminScreen : AppCompatActivity() {

    private lateinit var adminBookingsContainer: LinearLayout
    private var adminName: String = "Admin"
    private var resortName: String = "Canyon Cove"

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            SupabaseCloudManager.fetchBookingsFromSupabase { _, _ ->
                renderAdminBookings()
            }
            refreshHandler.postDelayed(this, 3000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContentView(R.layout.activity_adminscreen)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        adminName = intent.getStringExtra("ADMIN_NAME") ?: "Admin"
        resortName = intent.getStringExtra("RESORT_NAME") ?: "Canyon Cove"

        adminBookingsContainer = findViewById(R.id.admin_bookings_container)

        val adminSubtitle = findViewById<TextView>(R.id.admin_subtitle)
        val adminResortName = findViewById<TextView>(R.id.admin_resort_name)

        adminSubtitle.text = "Welcome, $adminName (Resort Administrator)"
        adminResortName.text = "Managing Resort: $resortName\n(Live Auto-Refresh Every 3s)"

        val btnLogout = findViewById<Button>(R.id.btn_admin_logout)

        btnLogout.setOnClickListener {
            finish()
        }

        renderAdminBookings()
    }

    override fun onResume() {
        super.onResume()
        refreshHandler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    private fun calculateNights(checkInStr: String, checkOutStr: String): Int {
        return try {
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val inDate = df.parse(checkInStr)
            val outDate = df.parse(checkOutStr)
            if (inDate != null && outDate != null) {
                val diffMs = outDate.time - inDate.time
                val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
                if (diffDays > 0) diffDays else 1
            } else {
                1
            }
        } catch (e: Exception) {
            1
        }
    }

    private fun renderAdminBookings() {
        adminBookingsContainer.removeAllViews()
        val resortBookings = BookingManager.getBookingsForResort(resortName)

        if (resortBookings.isEmpty()) {
            val emptyTextView = TextView(this).apply {
                text = "No active customer bookings yet for $resortName.\n(Listening for new customer bookings...)"
                setTextColor(Color.parseColor("#888888"))
                textSize = 14f
                setPadding(0, 16, 0, 16)
            }
            adminBookingsContainer.addView(emptyTextView)
            return
        }

        for (booking in resortBookings) {
            val cardView = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#1E1E1E"))
                setPadding(20, 20, 20, 20)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 16)
                layoutParams = params
            }

            val titleText = TextView(this).apply {
                text = "Booking #${booking.bookingId} - ${booking.resort.name}"
                setTextColor(Color.WHITE)
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
            }

            val baseNum = booking.resort.price.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 2500.0
            val nights = calculateNights(booking.checkInDate, booking.checkOutDate)
            val displayTotal = if (booking.totalPrice > 0.0) booking.totalPrice else (baseNum * booking.pax * nights)
            val emailDisplay = if (booking.touristEmail.isNotEmpty()) booking.touristEmail else "Customer"
            val formattedPrice = String.format(Locale.US, "Php %,.2f", displayTotal)

            val detailsHtml = "<b>Customer:</b> <font color='#B388FF'>$emailDisplay</font><br>" +
                    "<b>Dates:</b> ${booking.checkInDate} to ${booking.checkOutDate} (<font color='#45ED4D'><b>$nights Night${if (nights > 1) "s" else ""}</b></font>)<br>" +
                    "<b>Guests:</b> <font color='#45ED4D'><b>${booking.pax} Pax</b></font><br>" +
                    "<b>Total Price:</b> <font color='#45ED4D'><b>$formattedPrice</b></font><br>" +
                    "<b>Status:</b> ${booking.status}"

            val detailsText = TextView(this).apply {
                text = Html.fromHtml(detailsHtml, Html.FROM_HTML_MODE_COMPACT)
                setTextColor(Color.parseColor("#CCCCCC"))
                textSize = 14f
                setPadding(0, 4, 0, 8)
            }

            val actionLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val btnConfirm = Button(this).apply {
                text = "Approve"
                setTextColor(Color.parseColor("#45ED4D"))
                textSize = 12f
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener {
                    booking.status = "Confirmed"
                    SupabaseCloudManager.updateBookingStatusInSupabase(booking.bookingId, "Confirmed") { _, _ -> }
                    Toast.makeText(context, "Booking #${booking.bookingId} Approved!", Toast.LENGTH_SHORT).show()
                    renderAdminBookings()
                }
            }

            val btnNotApproved = Button(this).apply {
                text = "Not Approved"
                setTextColor(Color.parseColor("#FF5252"))
                textSize = 12f
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener {
                    booking.status = "Not Approved"
                    SupabaseCloudManager.updateBookingStatusInSupabase(booking.bookingId, "Not Approved") { _, _ -> }
                    Toast.makeText(context, "Booking #${booking.bookingId} marked as Not Approved.", Toast.LENGTH_SHORT).show()
                    renderAdminBookings()
                }
            }

            val btnCancel = Button(this).apply {
                text = "Cancel"
                setTextColor(Color.parseColor("#888888"))
                textSize = 12f
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener {
                    BookingManager.cancelBooking(booking.bookingId)
                    SupabaseCloudManager.deleteBookingFromSupabase(booking.bookingId) { _, _ -> }
                    Toast.makeText(context, "Booking #${booking.bookingId} Cancelled.", Toast.LENGTH_SHORT).show()
                    renderAdminBookings()
                }
            }

            actionLayout.addView(btnConfirm)
            actionLayout.addView(btnNotApproved)
            actionLayout.addView(btnCancel)

            cardView.addView(titleText)
            cardView.addView(detailsText)
            cardView.addView(actionLayout)

            adminBookingsContainer.addView(cardView)
        }
    }
}
