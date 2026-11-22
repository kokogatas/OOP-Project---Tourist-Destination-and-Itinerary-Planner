package com.example.tdip

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Html
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Mainscreen : AppCompatActivity() {

    private lateinit var resortsListContainer: LinearLayout
    private lateinit var itineraryListContainer: LinearLayout
    private lateinit var searchEditText: EditText
    private lateinit var tvMapOverlay: TextView
    private lateinit var tvWarningBanner: TextView
    private lateinit var mapLibreView: MapView
    private lateinit var btnFullScreenMap: Button
    private lateinit var btnOpenGoogleMaps: Button

    private var selectedCategory: String = "All"
    private var searchQuery: String = ""
    private var currentUserEmail: String = ""
    private var currentlySelectedResort: Resort? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: Location? = null

    private val allResorts = listOf(
        Resort(1, "Berna Beach Resort", "Beachfront, Nasugbu", listOf("All", "Budget", "WiFi", "Pool"), 4.0, "Php 1,500/night", 14.0687293198155, 120.62560921173868, true, "2:00 PM", "12:00 NN", "6:00 AM – 5:00 PM", "10:00 AM – 8:30 PM"),
        Resort(2, "Calayo Beach Resort", "Calayo Beach Area, Nasugbu", listOf("All", "Budget", "Family", "WiFi"), 4.5, "Php 1,800/night", 14.146119552634666, 120.61212663872902, true, "2:00 PM", "12:00 PM", "N/A", "7:30 AM – 5:00 PM (Pool: 7:30 AM – 11:00 PM)"),
        Resort(3, "Sand Bar Beach Resort", "Near Calayo Beach, Nasugbu", listOf("All", "Budget", "WiFi"), 4.1, "Php 2,200/night", 14.151478147970018, 120.60977798475078, true, "2:00 PM", "12:00 PM", "N/A", "Latest Check-in: 8:00 PM"),
        Resort(4, "Pamana Resorts and Hotels", "Beachfront, Nasugbu", listOf("All", "Family", "WiFi"), 4.1, "Php 5,900/night", 14.152455482419331, 120.60930996556769, true, "2:00 PM", "12:00 PM", "N/A", "Latest Check-in: 8:00 PM"),
        Resort(5, "Canyon Cove Hotel & Spa", "White-sand Beachfront, Nasugbu", listOf("All", "Pool", "Family", "Spa", "WiFi"), 3.8, "Php 3,800/night", 14.092536522979145, 120.62173702154001, true, "3:00 PM", "11:00 AM", "7:00 AM – 8:00 PM", "24/7 Front Desk"),
        Resort(6, "Pico Sands Hotel", "Cove and Lagoon, Nasugbu", listOf("All", "Pool", "Spa", "Family", "WiFi"), 4.4, "Php 6,500/night", 14.192116211144523, 120.60208803872919, true, "2:00 PM", "11:00 AM", "N/A", "24/7 Front Desk"),
        Resort(7, "Club Punta Fuego", "Private Resort Beach, Nasugbu", listOf("All", "Pool", "Spa", "WiFi"), 4.8, "Php 7,200/night", 14.133503675842242, 120.57975121173968, true, "2:00 PM", "12:00 PM", "N/A", "Standard Front Desk"),
        Resort(8, "Shoreland Beach Resort powered by Cocotel", "Beachfront, Nasugbu", listOf("All", "Budget", "WiFi", "Pool"), 3.7, "Php 2,539/night", 14.06999023964827, 120.62567936756317, true, "2:00 PM", "12:00 PM", "N/A", "Latest Check-in: 9:00 PM"),
        Resort(9, "RSAM Beach Resort by Cocotel", "Beachfront, Nasugbu", listOf("All", "Family", "WiFi"), 3.8, "Php 4,750/night", 14.15077504858209, 120.60990193688187, true, "2:00 PM", "12:00 PM", "N/A", "Standard Front Desk"),
        Resort(10, "Pico De Loro Beach", "Cove and Lagoon, Nasugbu", listOf("All", "Pool", "Spa", "Family"), 4.4, "Php 5,500/night", 14.189976914403214, 120.59706429639938, true, "2:00 PM", "12:00 PM", "6:00 AM – 6:00 PM", "Private Members Club")
    )

    private val resortAmenitiesMap = mapOf(
        1 to listOf("Swimming Pool", "Beachfront", "Non-Smoking Rooms", "Air Conditioning", "Free Internet", "Free Self-Parking", "Room Service"),
        2 to listOf("Wheelchair Accessible", "Parking", "Paid Private Parking On-Site", "Street Parking", "Free Self-Parking", "Pets Welcome", "Free Wi-Fi", "Air Conditioning"),
        3 to listOf("Pets Welcome", "Beachfront", "Air Conditioning", "Free Internet", "Free Self-Parking", "Parking Garage", "Room Service", "Housekeeping"),
        4 to listOf("Parking", "Accessible Parking", "Paid Private Parking On-Site", "Free Self-Parking", "Free Wi-Fi", "Air Conditioning", "Smoking-Free"),
        5 to listOf("Wheelchair Accessible", "Swimming Pool", "Spa", "Free Wi-Fi", "Beachfront", "Mountain View", "Non-Smoking Rooms", "Air Conditioning", "Meeting Rooms", "Parking", "Paid Private Parking On-Site", "Free Self-Parking", "24-Hour Front Desk", "Concierge", "Free Breakfast", "Room Service", "Housekeeping", "Laundry Service", "Suites", "Balcony", "Coffee and Tea Makers", "Clothes Iron", "Hair Dryers", "Accepts Credit Cards"),
        6 to listOf("Swimming Pool", "Fitness Center", "Spa", "Free Wi-Fi", "Mountain View", "Non-Smoking Rooms", "Air Conditioning", "Meeting Rooms", "Parking", "Paid Private Parking On-Site", "Valet Parking", "Free Self-Parking", "24-Hour Front Desk", "Concierge", "Shuttle Bus", "Free Breakfast", "Room Service", "Dry-Cleaning", "Laundry Service", "Balcony", "Minibars", "Tennis Court"),
        7 to listOf("Swimming Pool", "Fitness Center", "Spa", "Free Wi-Fi", "Beachfront", "Non-Smoking Rooms", "Air Conditioning", "Business Center", "Meeting Rooms", "Parking", "Accessible Parking", "Paid Private Parking On-Site", "Free Self-Parking", "24-Hour Front Desk", "Concierge", "Shuttle Bus", "Free Breakfast", "Room Service", "Laundry Service", "Room Safes", "Suites", "Balcony", "Refrigerator", "Minibars", "Televisions", "Golf Course", "Tennis Court", "Private Beach"),
        8 to listOf("Swimming Pool", "Pets Welcome", "Free Wi-Fi", "Non-Smoking Rooms", "Air Conditioning", "Parking", "Paid Private Parking On-Site", "Free Self-Parking"),
        9 to listOf("Parking", "Accessible Parking", "Paid Private Parking On-Site", "Free Self-Parking", "Parking Garage", "Pets Welcome", "Free Wi-Fi", "Smoking-Free"),
        10 to listOf("Swimming Pool", "Beachfront", "Free Wi-Fi", "Air Conditioning", "Parking", "Spa", "Fitness Center")
    )

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        resortsListContainer = findViewById(R.id.resorts_list_container)
        itineraryListContainer = findViewById(R.id.itinerary_list_container)
        searchEditText = findViewById(R.id.search_edit_text)
        tvMapOverlay = findViewById(R.id.tv_map_overlay)
        tvWarningBanner = findViewById(R.id.tv_schedule_warning_banner)
        mapLibreView = findViewById(R.id.mapView)
        btnFullScreenMap = findViewById(R.id.btn_fullscreen_map)
        btnOpenGoogleMaps = findViewById(R.id.btn_open_google_maps)

        mapLibreView.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 100)
        }

        fetchUserGpsLocationAndInitializeMap()

        currentUserEmail = intent.getStringExtra("USER_EMAIL") ?: "tourist@example.com"
        val greetingTitle = findViewById<TextView>(R.id.greeting_title)
        if (currentUserEmail.isNotEmpty()) {
            val name = currentUserEmail.substringBefore("@")
            greetingTitle.text = "Welcome, $name"
        }

        btnFullScreenMap.setOnClickListener {
            val targetResort = currentlySelectedResort ?: allResorts[0]
            val intent = Intent(this, FullScreenMapActivity::class.java).apply {
                putExtra("RESORT_ID", targetResort.resortId)
                putExtra("RESORT_NAME", targetResort.name)
                putExtra("RESORT_LAT", targetResort.latitude)
                putExtra("RESORT_LON", targetResort.longitude)
            }
            startActivity(intent)
        }

        btnOpenGoogleMaps.setOnClickListener {
            val targetResort = currentlySelectedResort ?: allResorts[0]
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

    private fun getYellowMarkerIcon(context: Context): Icon {
        val size = 64
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD700")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, paint)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, borderPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 30f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("🟡", size / 2f, size / 2f + 10f, textPaint)

        return IconFactory.getInstance(context).fromBitmap(bitmap)
    }

    private fun fetchUserGpsLocationAndInitializeMap() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val isGpsOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsOn) {
            Toast.makeText(this, "GPS is OFF. Distance cannot be calculated.", Toast.LENGTH_LONG).show()
            initializeMapWithResorts(null)
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { loc ->
                if (loc != null) {
                    userLocation = loc
                } else {
                    val fallbackLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (fallbackLoc != null) {
                        userLocation = fallbackLoc
                    }
                }
                initializeMapWithResorts(userLocation)
            }.addOnFailureListener {
                initializeMapWithResorts(null)
            }
        } else {
            initializeMapWithResorts(null)
        }
    }

    private fun initializeMapWithResorts(userLoc: Location?) {
        mapLibreView.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri("https://styles.maptoolkit.org/summer.json")) { _ ->
                val nasugbuCenter = LatLng(14.07, 120.63)
                val cameraPosition = CameraPosition.Builder()
                    .target(nasugbuCenter)
                    .zoom(12.0)
                    .build()
                map.cameraPosition = cameraPosition

                if (userLoc != null) {
                    val yellowIcon = getYellowMarkerIcon(this)
                    map.addMarker(MarkerOptions().position(LatLng(userLoc.latitude, userLoc.longitude)).title("🟡 Your GPS Location").icon(yellowIcon))
                }

                for (resort in allResorts) {
                    map.addMarker(MarkerOptions().position(LatLng(resort.latitude, resort.longitude)).title("📍 ${resort.name}"))
                }
            }
        }
        updateGpsMap(allResorts[0])
    }

    override fun onStart() { super.onStart(); mapLibreView.onStart() }
    override fun onResume() { super.onResume(); mapLibreView.onResume(); refreshHandler.post(refreshRunnable) }
    override fun onPause() { super.onPause(); mapLibreView.onPause(); refreshHandler.removeCallbacks(refreshRunnable) }
    override fun onStop() { super.onStop(); mapLibreView.onStop() }
    override fun onDestroy() { super.onDestroy(); mapLibreView.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapLibreView.onSaveInstanceState(outState) }
    override fun onLowMemory() { super.onLowMemory(); mapLibreView.onLowMemory() }

    private fun updateGpsMap(resort: Resort) {
        currentlySelectedResort = resort

        mapLibreView.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri("https://styles.maptoolkit.org/summer.json")) { _ ->
                map.removeAnnotations()

                val resortPoint = LatLng(resort.latitude, resort.longitude)
                map.addMarker(MarkerOptions().position(resortPoint).title("📍 ${resort.name}"))

                if (userLocation != null) {
                    val yellowIcon = getYellowMarkerIcon(this)
                    val userPoint = LatLng(userLocation!!.latitude, userLocation!!.longitude)
                    map.addMarker(MarkerOptions().position(userPoint).title("🟡 Your GPS Location").icon(yellowIcon))
                }

                val cameraPosition = CameraPosition.Builder()
                    .target(resortPoint)
                    .zoom(14.0)
                    .build()
                map.cameraPosition = cameraPosition
            }
        }

        var distanceText = ""
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val isGpsOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsOn) {
            distanceText = "<br><font color='#FF5252'><b>GPS is OFF. Distance cannot be calculated.</b></font>"
        } else if (userLocation != null) {
            val results = FloatArray(1)
            Location.distanceBetween(userLocation!!.latitude, userLocation!!.longitude, resort.latitude, resort.longitude, results)
            val meters = results[0]
            val km = meters / 1000.0
            val miles = km * 0.621371
            distanceText = "<br><b>User GPS:</b> %.4f° N, %.4f° E<br><b>Distance:</b> <font color='#45ED4D'><b>%.2f km (%.2f miles)</b></font>".format(userLocation!!.latitude, userLocation!!.longitude, km, miles)
        } else {
            distanceText = "<br><font color='#FFB300'><b>Fetching GPS location...</b></font>"
        }

        val overlayHtml = "<b>📍 <font color='#B388FF'>${resort.name}</font></b> (ID #${resort.resortId})<br>Resort GPS: ${resort.latitude}° N, ${resort.longitude}° E$distanceText"
        tvMapOverlay.text = Html.fromHtml(overlayHtml, Html.FROM_HTML_MODE_COMPACT)
        tvMapOverlay.textSize = 12.0f
        tvMapOverlay.setTextColor(Color.WHITE)
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

    private fun renderItinerary() {
        itineraryListContainer.removeAllViews()
        val userBookings = BookingManager.getTouristBookings(currentUserEmail)

        var hasConflict = false
        for (i in 0 until userBookings.size) {
            for (j in i + 1 until userBookings.size) {
                val b1 = userBookings[i]
                val b2 = userBookings[j]
                try {
                    val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val in1 = df.parse(b1.checkInDate)
                    val out1 = df.parse(b1.checkOutDate)
                    val in2 = df.parse(b2.checkInDate)
                    val out2 = df.parse(b2.checkOutDate)
                    if (in1 != null && out1 != null && in2 != null && out2 != null) {
                        if (in1.before(out2) && out1.after(in2)) {
                            hasConflict = true
                        }
                    }
                } catch (e: Exception) {
                }
            }
        }

        if (hasConflict) {
            tvWarningBanner.visibility = View.VISIBLE
            tvWarningBanner.text = "Schedule Conflict Warning: You have overlapping booking dates between multiple resorts! You sure about that?"
        } else {
            tvWarningBanner.visibility = View.GONE
        }

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
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
            }

            val statusLower = booking.status.lowercase()
            val (statusLabel, statusColor) = when {
                statusLower.contains("confirm") -> "Approved (Confirmed)" to "#45ED4D"
                statusLower.contains("not") || statusLower.contains("reject") || statusLower.contains("cancel") -> "Status: Not Approved" to "#FF5252"
                else -> "Pending Approval" to "#FFB300"
            }

            val baseNum = booking.resort.price.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 2500.0
            val nights = calculateNights(booking.checkInDate, booking.checkOutDate)
            val displayTotal = if (booking.totalPrice > 0.0) booking.totalPrice else (baseNum * booking.pax * nights)

            val datesText = TextView(this).apply {
                text = "Dates: ${booking.checkInDate} to ${booking.checkOutDate} ($nights Night${if (nights > 1) "s" else ""}, ${booking.pax} Pax)\n$statusLabel\nTotal Price: Php %,.2f".format(displayTotal)
                setTextColor(Color.parseColor(statusColor))
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
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
                filterMapMarkers(searchQuery)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterMapMarkers(query: String) {
        mapLibreView.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri("https://styles.maptoolkit.org/summer.json")) { _ ->
                map.removeAnnotations()
                if (userLocation != null) {
                    val yellowIcon = getYellowMarkerIcon(this)
                    map.addMarker(MarkerOptions().position(LatLng(userLocation!!.latitude, userLocation!!.longitude)).title("🟡 Your GPS Location").icon(yellowIcon))
                }
                val filtered = allResorts.filter { query.isEmpty() || it.name.contains(query, true) || it.location.contains(query, true) }
                for (resort in filtered) {
                    map.addMarker(MarkerOptions().position(LatLng(resort.latitude, resort.longitude)).title("📍 ${resort.name}"))
                }
                if (filtered.isNotEmpty()) {
                    updateGpsMap(filtered[0])
                }
            }
        }
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
            setTypeface(null, Typeface.BOLD)
        }

        val checkInBtn = Button(this).apply {
            text = "Select Check-In Date"
            setTextColor(Color.parseColor("#B388FF"))
        }

        val checkOutBtn = Button(this).apply {
            text = "Select Check-Out Date"
            setTextColor(Color.parseColor("#B388FF"))
        }

        val paxInput = EditText(this).apply {
            hint = "Enter number of persons (Pax), e.g. 2"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#888888"))
            inputType = InputType.TYPE_CLASS_NUMBER
            setText("2")
        }

        val baseNum = resort.price.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 2500.0
        
        val cal = Calendar.getInstance()
        val defaultYear = cal.get(Calendar.YEAR)
        val defaultMonth = cal.get(Calendar.MONTH)
        val defaultDay = cal.get(Calendar.DAY_OF_MONTH)

        val checkInDefault = String.format("%04d-%02d-%02d", defaultYear, defaultMonth + 1, defaultDay)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val checkOutDefault = String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))

        var checkInDateStr = checkInDefault
        var checkOutDateStr = checkOutDefault

        val initialNights = calculateNights(checkInDateStr, checkOutDateStr)
        val initialTotal = baseNum * 2 * initialNights

        val priceEstimateText = TextView(this).apply {
            text = "Estimated Total: Php %,.2f (For 2 Pax, $initialNights Nights)".format(initialTotal)
            setTextColor(Color.parseColor("#45ED4D"))
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 8, 0, 8)
        }

        fun updateEstimate() {
            val pax = paxInput.text.toString().toIntOrNull() ?: 1
            val nights = calculateNights(checkInDateStr, checkOutDateStr)
            val total = baseNum * (if (pax > 0) pax else 1) * nights
            priceEstimateText.text = "Estimated Total: Php %,.2f (For $pax Pax, $nights Nights)".format(total)
        }

        paxInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateEstimate()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val cashPayBtn = Button(this).apply {
            text = "Pay with Cash at Resort"
            setTextColor(Color.parseColor("#45ED4D"))
        }

        val cardPayBtn = Button(this).apply {
            text = "Credit/Debit Card (Unavailable / Disabled)"
            setTextColor(Color.GRAY)
            isEnabled = false
            alpha = 0.5f
        }

        checkInBtn.text = "Check-In: $checkInDateStr"
        checkOutBtn.text = "Check-Out: $checkOutDateStr"

        checkInBtn.setOnClickListener {
            val pickerCal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                checkInDateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
                checkInBtn.text = "Check-In: $checkInDateStr"
                updateEstimate()
            }, pickerCal.get(Calendar.YEAR), pickerCal.get(Calendar.MONTH), pickerCal.get(Calendar.DAY_OF_MONTH)).show()
        }

        checkOutBtn.setOnClickListener {
            val pickerCal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                checkOutDateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
                checkOutBtn.text = "Check-Out: $checkOutDateStr"
                updateEstimate()
            }, pickerCal.get(Calendar.YEAR), pickerCal.get(Calendar.MONTH), pickerCal.get(Calendar.DAY_OF_MONTH)).show()
        }

        dialogView.addView(titleText)
        dialogView.addView(checkInBtn)
        dialogView.addView(checkOutBtn)
        dialogView.addView(paxInput)
        dialogView.addView(priceEstimateText)
        dialogView.addView(cashPayBtn)
        dialogView.addView(cardPayBtn)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        cashPayBtn.setOnClickListener {
            val paxStr = paxInput.text.toString().trim()
            val pax = paxStr.toIntOrNull() ?: 1

            val result = BookingManager.createBooking(
                touristEmail = currentUserEmail,
                resort = resort,
                checkInStr = checkInDateStr,
                checkOutStr = checkOutDateStr,
                paymentMethod = "Cash ($pax Pax)"
            )

            when (result) {
                is BookingManager.BookingResult.Success -> {
                    Toast.makeText(this, "Booking submitted for $pax Pax at ${resort.name}!", Toast.LENGTH_LONG).show()
                    
                    SupabaseCloudManager.uploadBookingToSupabase(result.booking) { success, msg ->
                        if (success) {
                            Toast.makeText(this, "Booking saved to Cloud!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Booking saved locally ($msg)", Toast.LENGTH_SHORT).show()
                        }
                    }

                    renderItinerary()
                    dialog.dismiss()
                }
                is BookingManager.BookingResult.Failure -> {
                    DialogUtils.showFullErrorDialog(this, "Booking Error", result.errorMessage)
                }
            }
        }

        dialog.show()
    }

    private fun createResortView(resort: Resort): View {
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 24)
            setOnClickListener {
                updateGpsMap(resort)
            }
        }

        val nameTextView = TextView(this).apply {
            text = resort.name
            setTextColor(Color.WHITE)
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
        }

        val detailsTextView = TextView(this).apply {
            val htmlText = "Rating: <font color='#45ED4D'><b>${resort.rating}/5</b></font> | Price: <font color='#45ED4D'><b>${resort.price}</b></font><br>GPS: ${resort.getGpsCoordinates()}"
            text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
            setTextColor(Color.parseColor("#CCCCCC"))
            textSize = 14f
            setPadding(0, 4, 0, 8)
        }

        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 4, 0, 0)
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

        val viewButton = Button(this).apply {
            text = "View"
            setTextColor(Color.parseColor("#B388FF"))
            textSize = 12f
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                updateGpsMap(resort)
                val amenitiesList = resortAmenitiesMap[resort.resortId] ?: resort.amenities
                val intent = Intent(context, ResortDetailActivity::class.java).apply {
                    putExtra("RESORT_ID", resort.resortId)
                    putExtra("RESORT_NAME", resort.name)
                    putExtra("RESORT_LOCATION", resort.location)
                    putExtra("RESORT_RATING", resort.rating)
                    putExtra("RESORT_PRICE", resort.price)
                    putExtra("RESORT_LAT", resort.latitude)
                    putExtra("RESORT_LON", resort.longitude)
                    putExtra("RESORT_AVAILABLE", resort.isAvailable)
                    putExtra("USER_EMAIL", currentUserEmail)
                    putExtra("RESORT_CHECKIN", resort.checkInTime)
                    putExtra("RESORT_CHECKOUT", resort.checkOutTime)
                    putExtra("RESORT_DAYTOUR", resort.dayTourSchedule)
                    putExtra("RESORT_FRONTDESK", resort.frontDeskHours)
                    putStringArrayListExtra("RESORT_AMENITIES", ArrayList(amenitiesList))
                }
                startActivity(intent)
            }
        }

        buttonLayout.addView(addButton)
        buttonLayout.addView(viewButton)

        cardLayout.addView(nameTextView)
        cardLayout.addView(detailsTextView)
        cardLayout.addView(buttonLayout)

        return cardLayout
    }
}
