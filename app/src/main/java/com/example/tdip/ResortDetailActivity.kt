package com.example.tdip

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.InputType
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.util.Calendar

class ResortDetailActivity : AppCompatActivity() {

    private lateinit var mapLibreView: MapView
    private lateinit var tvName: TextView
    private lateinit var tvInfo: TextView
    private lateinit var tvAmenities: TextView
    private lateinit var tvAvailability: TextView
    private lateinit var btnBook: Button
    private lateinit var btnBack: Button

    private var resortId: Int = 1
    private var resortName: String = "Resort"
    private var resortLocation: String = "Nasugbu"
    private var resortRating: Double = 4.5
    private var resortPrice: String = "Php 2,500/night"
    private var resortLat: Double = 14.0722
    private var resortLon: Double = 120.6288
    private var isAvailable: Boolean = true
    private var currentUserEmail: String = ""
    private var amenitiesList: ArrayList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContentView(R.layout.activity_resort_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        resortId = intent.getIntExtra("RESORT_ID", 1)
        resortName = intent.getStringExtra("RESORT_NAME") ?: "Resort"
        resortLocation = intent.getStringExtra("RESORT_LOCATION") ?: "Nasugbu"
        resortRating = intent.getDoubleExtra("RESORT_RATING", 4.5)
        resortPrice = intent.getStringExtra("RESORT_PRICE") ?: "Php 2,500/night"
        resortLat = intent.getDoubleExtra("RESORT_LAT", 14.0722)
        resortLon = intent.getDoubleExtra("RESORT_LON", 120.6288)
        isAvailable = intent.getBooleanExtra("RESORT_AVAILABLE", true)
        currentUserEmail = intent.getStringExtra("USER_EMAIL") ?: "tourist@example.com"
        amenitiesList = intent.getStringArrayListExtra("RESORT_AMENITIES") ?: arrayListOf()

        tvName = findViewById(R.id.detail_resort_name)
        tvInfo = findViewById(R.id.detail_resort_info)
        tvAmenities = findViewById(R.id.detail_amenities)
        tvAvailability = findViewById(R.id.detail_availability)
        mapLibreView = findViewById(R.id.detail_mapView)
        btnBook = findViewById(R.id.detail_book_btn)
        btnBack = findViewById(R.id.detail_back_btn)

        mapLibreView.onCreate(savedInstanceState)
        mapLibreView.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri("https://styles.maptoolkit.org/summer.json")) { _ ->
                val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(resortLat, resortLon))
                    .zoom(14.0)
                    .build()
                map.cameraPosition = cameraPosition
                map.addMarker(MarkerOptions().position(LatLng(resortLat, resortLon)).title(resortName))
            }
        }

        var distanceText = ""
        try {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    val userLatLng = Location("user").apply {
                        latitude = loc.latitude
                        longitude = loc.longitude
                    }
                    val resortLatLng = Location("resort").apply {
                        latitude = resortLat
                        longitude = resortLon
                    }
                    val meters = userLatLng.distanceTo(resortLatLng)
                    val km = meters / 1000.0
                    distanceText = "\nDistance: %.2f km from your GPS".format(km)
                }
            }
        } catch (e: Exception) {
        }

        tvName.text = "Resort ID #$resortId: $resortName"
        tvInfo.text = "Location: $resortLocation\nRating: $resortRating / 5\nBase Price: $resortPrice\nGPS: %.4f° N, %.4f° E$distanceText".format(resortLat, resortLon)

        val amenitiesFormatted = if (amenitiesList.isNotEmpty()) {
            amenitiesList.joinToString(prefix = "• ", separator = "\n• ")
        } else {
            "• Standard Resort Amenities"
        }
        tvAmenities.text = "Amenities & Features:\n$amenitiesFormatted"

        updateAvailabilityDisplay()

        btnBook.setOnClickListener { showBookingDialog() }
        btnBack.setOnClickListener { finish() }
    }

    override fun onStart() { super.onStart(); mapLibreView.onStart() }
    override fun onResume() { super.onResume(); mapLibreView.onResume() }
    override fun onPause() { super.onPause(); mapLibreView.onPause() }
    override fun onStop() { super.onStop(); mapLibreView.onStop() }
    override fun onDestroy() { super.onDestroy(); mapLibreView.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapLibreView.onSaveInstanceState(outState) }
    override fun onLowMemory() { super.onLowMemory(); mapLibreView.onLowMemory() }

    private fun updateAvailabilityDisplay() {
        if (isAvailable) {
            tvAvailability.text = "Status: Available for Booking"
            tvAvailability.setTextColor(Color.parseColor("#45ED4D"))
            btnBook.isEnabled = true
            btnBook.alpha = 1.0f
        } else {
            tvAvailability.text = "Status: Fully Booked / Unavailable"
            tvAvailability.setTextColor(Color.parseColor("#FF5252"))
            btnBook.isEnabled = false
            btnBook.alpha = 0.5f
        }
    }

    private fun extractBasePrice(priceStr: String): Double {
        val digits = priceStr.replace(Regex("[^0-9.]"), "")
        return digits.toDoubleOrNull() ?: 2500.0
    }

    private fun showBookingDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 30)
        }

        val titleText = TextView(this).apply {
            text = "Book $resortName"
            setTextColor(Color.WHITE)
            textSize = 18f
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

        val baseNum = extractBasePrice(resortPrice)
        val priceEstimateText = TextView(this).apply {
            text = "Estimated Total: Php %,.2f (For 2 Pax)".format(baseNum * 2)
            setTextColor(Color.parseColor("#45ED4D"))
            textSize = 14f
            setPadding(0, 8, 0, 8)
        }

        paxInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pax = s.toString().toIntOrNull() ?: 1
                val total = baseNum * if (pax > 0) pax else 1
                priceEstimateText.text = "Estimated Total: Php %,.2f (For $pax Pax)".format(total)
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

            val dummyResort = Resort(resortId, resortName, resortLocation, listOf("All"), resortRating, resortPrice, resortLat, resortLon, isAvailable)

            val result = BookingManager.createBooking(
                touristEmail = currentUserEmail,
                resort = dummyResort,
                checkInStr = checkInDateStr,
                checkOutStr = checkOutDateStr,
                paymentMethod = "Cash ($pax Pax)"
            )

            when (result) {
                is BookingManager.BookingResult.Success -> {
                    Toast.makeText(this, "Booking submitted for $pax Pax at $resortName!", Toast.LENGTH_LONG).show()
                    isAvailable = false
                    updateAvailabilityDisplay()
                    SupabaseCloudManager.uploadBookingToSupabase(result.booking) { _, _ -> }
                    dialog.dismiss()
                }
                is BookingManager.BookingResult.Failure -> {
                    DialogUtils.showFullErrorDialog(this, "Booking Error", result.errorMessage)
                }
            }
        }

        dialog.show()
    }
}
