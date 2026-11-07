package com.example.tdip

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

// AdminScreen: Dashboard ng resort admin. Naka-3s auto refresh para nakikita ang live customer bookings mula sa Cloud!
class AdminScreen : AppCompatActivity() {

    private lateinit var adminBookingsContainer: LinearLayout
    private var adminName: String = "Admin"
    private var resortName: String = "Canyon Cove"

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            // Live fetch sa Cloud bawat 3 segundo
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
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
            }

            val detailsText = TextView(this).apply {
                text = "Check-In: ${booking.checkInDate} | Check-Out: ${booking.checkOutDate}\nStatus: ${booking.status}"
                setTextColor(Color.parseColor("#CCCCCC"))
                textSize = 13f
                setPadding(0, 4, 0, 8)
            }

            val actionLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            // Admin Approve Button
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

            // Admin Not Approved / Reject Button
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

            // Admin Cancel / Delete Button
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
