package com.example.tdip

import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Calendar

// Mainscreen: Ang dashboard ng customer. Dito nakikita ang interactive Google Maps, resorts, at itinerary.
class Mainscreen : AppCompatActivity() {

    private lateinit var resortsListContainer: LinearLayout
    private lateinit var itineraryListContainer: LinearLayout
    private lateinit var searchEditText: EditText
    private lateinit var tvMapOverlay: TextView
    private lateinit var mapWebView: WebView
    private lateinit var btnOpenGoogleMaps: Button

    private var selectedCategory: String = "All"
    private var searchQuery: String = ""
    private var currentUserEmail: String = ""
    private var currentlySelectedResort: Resort? = null

    // Complete list ng resorts sa Nasugbu, Batangas
    private val allResorts = listOf(
        Resort(1, "Bernabeach", "Beachfront, Nasugbu", listOf("All", "Budget", "WiFi"), 4.0, "Huts from Php 1,000", 14.0682, 120.6271),
        Resort(2, "Calayo Beach Resort", "Calayo Beach Area, Nasugbu", listOf("All", "Budget", "Family", "WiFi"), 4.5, "Php 1,800/night", 14.0722, 120.6288),
        Resort(3, "Sand Bar Beach Resort", "Near Calayo Beach, Nasugbu", listOf("All", "Budget", "WiFi"), 4.1, "Php 2,200/night", 14.0750, 120.6295),
        Resort(4, "Pamana Resorts", "Beachfront, Nasugbu", listOf("All", "Family", "Pool", "WiFi"), 4.1, "Rooms from Php 5,900", 14.0810, 120.6310),
        Resort(5, "Canyon Cove", "White-sand Beachfront, Nasugbu", listOf("All", "Pool", "Family", "Spa", "WiFi"), 3.8, "Midrange (Php 3,800)", 14.0880, 120.6350),
        Resort(6, "Pico Sands Hotel", "Cove and Lagoon, Nasugbu", listOf("All", "Pool", "Spa", "Family", "WiFi"), 4.4, "Premium (Php 6,500)", 14.1980, 120.6110),
        Resort(7, "Club Punta Fuego", "Private Resort Beach, Nasugbu", listOf("All", "Pool", "Spa", "WiFi"), 4.8, "Luxury (Php 7,200)", 14.1283, 120.5847),
        Resort(8, "Shoreland", "Beachfront, Nasugbu", listOf("All", "Budget", "WiFi"), 3.7, "From Php 2,539/night", 14.0620, 120.6250),
        Resort(9, "RSAM Beach Resort", "Beachfront, Nasugbu", listOf("All", "Family", "WiFi"), 3.8, "About Php 4,750/night", 14.0650, 120.6260),
        Resort(10, "Pico de Loro", "Cove and Lagoon, Nasugbu", listOf("All", "Pool", "Spa", "Family"), 4.4, "Access fees vary", 14.2003, 120.6121)
    )

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            // Live fetch sa Cloud bawat 1 segundo para updated status agad
            SupabaseCloudManager.fetchBookingsFromSupabase { _, _ ->
                renderItinerary()
            }
            refreshHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContentView(R.layout.activity_mainscreen)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        resortsListContainer = findViewById(R.id.resorts_list_container)
        itineraryListContainer = findViewById(R.id.itinerary_list_container)
        searchEditText = findViewById(R.id.search_edit_text)
        tvMapOverlay = findViewById(R.id.tv_map_overlay)
        mapWebView = findViewById(R.id.map_web_view)
        btnOpenGoogleMaps = findViewById(R.id.btn_open_google_maps)

        // Setup WebView for Google Maps
        mapWebView.settings.javaScriptEnabled = true
        mapWebView.webViewClient = WebViewClient()

        currentUserEmail = intent.getStringExtra("USER_EMAIL") ?: "tourist@example.com"
        val greetingTitle = findViewById<TextView>(R.id.greeting_title)
        if (currentUserEmail.isNotEmpty()) {
            val name = currentUserEmail.substringBefore("@")
            greetingTitle.text = "Welcome, $name"
        }

        // Default map view centered on Canyon Cove (resortId #5)
        updateGpsMap(allResorts[4])

        // Open in Google Maps app button listener
        btnOpenGoogleMaps.setOnClickListener {
            val targetResort = currentlySelectedResort ?: allResorts[4]
            val geoUri = Uri.parse("geo:${targetResort.latitude},${targetResort.longitude}?q=${Uri.encode(targetResort.name)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
            try {
                startActivity(mapIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Opening GPS coordinates: ${targetResort.getGpsCoordinates()}", Toast.LENGTH_SHORT).show()
            }
        }

        setupCategoryButtons()
        setupSearchListener()

        renderItinerary()
        renderResorts()
    }

    override fun onResume() {
        super.onResume()
        refreshHandler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    // Updates the embedded Google Map WebView with exact resort GPS coordinates
    private fun updateGpsMap(resort: Resort) {
        currentlySelectedResort = resort
        val embedMapUrl = "https://maps.google.com/maps?q=${resort.latitude},${resort.longitude}&z=15&output=embed"
        mapWebView.loadUrl(embedMapUrl)
        tvMapOverlay.text = "📍 ${resort.name}\nLat: ${resort.latitude}° N, Lon: ${resort.longitude}° E"
    }

    private fun renderItinerary() {
        itineraryListContainer.removeAllViews()
        val userBookings = BookingManager.getTouristBookings(currentUserEmail)

        if (userBookings.isEmpty()) {
            val emptyTextView = TextView(this).apply {
                text = "No active resort bookings yet. Explore resorts below to add to your itinerary."
                setTextColor(Color.parseColor("#888888"))
                textSize = 13f
                setPadding(0, 10, 0, 10)
            }
            itineraryListContainer.addView(emptyTextView)
            return
        }

        for (booking in userBookings) {
            val bookingCard = LinearLayout(this).apply {
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
                text = booking.resort.name
                setTextColor(Color.WHITE)
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
            }

            val statusLower = booking.status.lowercase()
            val (statusLabel, statusColor) = when {
                statusLower.contains("confirm") -> "Approved (Confirmed)" to "#45ED4D" // Green
                statusLower.contains("not") || statusLower.contains("reject") || statusLower.contains("cancel") -> "Status: Not Approved" to "#FF5252" // Red
                else -> "Pending Approval" to "#FFB300" // Gold
            }

            val datesText = TextView(this).apply {
                text = "Dates: ${booking.checkInDate} to ${booking.checkOutDate}\n$statusLabel"
                setTextColor(Color.parseColor(statusColor))
                textSize = 13f
                setPadding(0, 4, 0, 8)
            }

            val btnCancel = Button(this).apply {
                text = "Cancel / Remove"
                setTextColor(Color.RED)
                textSize = 12f
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener {
                    BookingManager.cancelBooking(booking.bookingId)
                    SupabaseCloudManager.deleteBookingFromSupabase(booking.bookingId) { _, _ -> }
                    Toast.makeText(context, "Booking for ${booking.resort.name} removed.", Toast.LENGTH_SHORT).show()
                    renderItinerary()
                }
            }

            bookingCard.addView(titleText)
            bookingCard.addView(datesText)
            bookingCard.addView(btnCancel)
            itineraryListContainer.addView(bookingCard)
        }
    }

    private fun setupCategoryButtons() {
        val chipAll = findViewById<Button>(R.id.chip_all)
        val chipPool = findViewById<Button>(R.id.chip_pool)
        val chipFamily = findViewById<Button>(R.id.chip_family)
        val chipSpa = findViewById<Button>(R.id.chip_spa)
        val chipWifi = findViewById<Button>(R.id.chip_wifi)
        val chipBudget = findViewById<Button>(R.id.chip_budget)

        val buttons = listOf(chipAll, chipPool, chipFamily, chipSpa, chipWifi, chipBudget)

        val clickListener = View.OnClickListener { view ->
            val button = view as Button
            selectedCategory = when (button.id) {
                R.id.chip_pool -> "Pool"
                R.id.chip_family -> "Family"
                R.id.chip_spa -> "Spa"
                R.id.chip_wifi -> "WiFi"
                R.id.chip_budget -> "Budget"
                else -> "All"
            }

            buttons.forEach { b ->
                if (b.id == button.id) {
                    b.setTextColor(Color.parseColor("#B388FF"))
                } else {
                    b.setTextColor(Color.parseColor("#CCCCCC"))
                }
            }

            renderResorts()
        }

        buttons.forEach { it.setOnClickListener(clickListener) }
    }

    private fun setupSearchListener() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString().trim()
                renderResorts()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun renderResorts() {
        resortsListContainer.removeAllViews()

        val filteredList = allResorts.filter { resort ->
            val matchesCategory = (selectedCategory == "All") || resort.amenities.contains(selectedCategory)
            val matchesSearch = searchQuery.isEmpty() || resort.name.contains(searchQuery, ignoreCase = true) || resort.location.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }

        if (filteredList.isEmpty()) {
            val emptyTextView = TextView(this).apply {
                text = "No resorts found for this category."
                setTextColor(Color.parseColor("#888888"))
                textSize = 14f
                setPadding(0, 20, 0, 20)
            }
            resortsListContainer.addView(emptyTextView)
            return
        }

        for (resort in filteredList) {
            val resortCard = createResortView(resort)
            resortsListContainer.addView(resortCard)
        }

        val sessionLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 40, 0, 40)
        }

        val btnLogout = Button(this).apply {
            text = "Log Out"
            setTextColor(Color.RED)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                Toast.makeText(context, "Logged out successfully!", Toast.LENGTH_SHORT).show()
                val intent = Intent(context, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        sessionLayout.addView(btnLogout)
        resortsListContainer.addView(sessionLayout)
    }

    private fun showBookingDateDialog(resort: Resort) {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 30)
        }

        val titleText = TextView(this).apply {
            text = "Book ${resort.name}"
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }

        val checkInBtn = Button(this).apply {
            text = "Select Check-In Date"
            setTextColor(Color.parseColor("#B388FF"))
        }

        val checkOutBtn = Button(this).apply {
            text = "Select Check-Out Date"
            setTextColor(Color.parseColor("#B388FF"))
        }

        val cal = Calendar.getInstance()
        val defaultYear = cal.get(Calendar.YEAR)
        val defaultMonth = cal.get(Calendar.MONTH)
        val defaultDay = cal.get(Calendar.DAY_OF_MONTH)

        val checkInDefault = String.format("%04d-%02d-%02d", defaultYear, defaultMonth + 1, defaultDay)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val checkOutDefault = String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))

        var checkInDateStr = checkInDefault
        var checkOutDateStr = checkOutDefault

        checkInBtn.text = "Check-In: $checkInDateStr"
        checkOutBtn.text = "Check-Out: $checkOutDateStr"

        checkInBtn.setOnClickListener {
            val pickerCal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                checkInDateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
                checkInBtn.text = "Check-In: $checkInDateStr"
            }, pickerCal.get(Calendar.YEAR), pickerCal.get(Calendar.MONTH), pickerCal.get(Calendar.DAY_OF_MONTH)).show()
        }

        checkOutBtn.setOnClickListener {
            val pickerCal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                checkOutDateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
                checkOutBtn.text = "Check-Out: $checkOutDateStr"
            }, pickerCal.get(Calendar.YEAR), pickerCal.get(Calendar.MONTH), pickerCal.get(Calendar.DAY_OF_MONTH)).show()
        }

        dialogView.addView(titleText)
        dialogView.addView(checkInBtn)
        dialogView.addView(checkOutBtn)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Confirm Booking") { _, _ ->
                val result = BookingManager.createBooking(
                    touristEmail = currentUserEmail,
                    resort = resort,
                    checkInStr = checkInDateStr,
                    checkOutStr = checkOutDateStr,
                    paymentMethod = "Card"
                )

                when (result) {
                    is BookingManager.BookingResult.Success -> {
                        Toast.makeText(this, "Booking submitted for ${resort.name}!", Toast.LENGTH_LONG).show()
                        
                        SupabaseCloudManager.uploadBookingToSupabase(result.booking) { _, _ -> }

                        renderItinerary()
                    }
                    is BookingManager.BookingResult.Failure -> {
                        DialogUtils.showFullErrorDialog(this, "Booking Error", result.errorMessage)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createResortView(resort: Resort): View {
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 24)
            setOnClickListener {
                updateGpsMap(resort)
                Toast.makeText(context, "GPS Map centered on ${resort.name}", Toast.LENGTH_SHORT).show()
            }
        }

        val nameTextView = TextView(this).apply {
            text = resort.name
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }

        val detailsTextView = TextView(this).apply {
            text = "Rating: ${resort.rating}/5 | ${resort.price}\nGPS: ${resort.getGpsCoordinates()}"
            setTextColor(Color.parseColor("#CCCCCC"))
            textSize = 13f
            setPadding(0, 4, 0, 8)
        }

        val addButton = Button(this).apply {
            text = "+ Add to Itinerary"
            setTextColor(Color.WHITE)
            textSize = 12f
            setOnClickListener {
                updateGpsMap(resort)
                showBookingDateDialog(resort)
            }
        }

        cardLayout.addView(nameTextView)
        cardLayout.addView(detailsTextView)
        cardLayout.addView(addButton)

        return cardLayout
    }
}
