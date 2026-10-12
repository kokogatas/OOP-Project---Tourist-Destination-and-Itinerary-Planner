package com.example.tdip

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Mainscreen : AppCompatActivity() {

    private lateinit var resortsListContainer: LinearLayout
    private lateinit var searchEditText: EditText


    private var selectedCategory: String = "All"
    private var searchQuery: String = ""




    private val allResorts = listOf(
        Resort("Bernabeach", "4.0/5", "Huts from Php 1,000", "Beachfront", listOf("All", "Budget", "WiFi")),
        Resort("Calayo Beach Resort", "4.5/5", "Php 1,800/night", "Calayo Beach Area", listOf("All", "Budget", "Family", "WiFi")),
        Resort("Sand Bar Beach Resort", "4.1/5", "Php 2,200/night", "Near Calayo Beach", listOf("All", "Budget", "WiFi")),
        Resort("Pamana Resorts", "4.1/5", "Rooms from Php 5,900", "Beachfront", listOf("All", "Family", "Pool", "WiFi")),
        Resort("Canyon Cove", "3.8/5", "Midrange (Php 3,800)", "White-sand Beachfront", listOf("All", "Pool", "Family", "Spa", "WiFi")),
        Resort("Pico Sands Hotel", "4.4/5", "Premium (Php 6,500)", "Cove and Lagoon", listOf("All", "Pool", "Spa", "Family", "WiFi")),
        Resort("Club Punta Fuego", "4.8/5", "Luxury (Php 7,200)", "Private Resort Beach", listOf("All", "Pool", "Spa", "WiFi")),
        Resort("Shoreland", "3.7/5", "From Php 2,539/night", "Beachfront", listOf("All", "Budget", "WiFi")),
        Resort("RSAM Beach Resort", "3.8/5", "About Php 4,750/night", "Beachfront", listOf("All", "Family", "WiFi")),
        Resort("Pico de Loro", "4.4/5", "Access fees vary", "Cove and Lagoon", listOf("All", "Pool", "Spa", "Family"))
    )

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
        searchEditText = findViewById(R.id.search_edit_text)

        val userEmail = intent.getStringExtra("USER_EMAIL")
        val greetingTitle = findViewById<TextView>(R.id.greeting_title)
        if (!userEmail.isNullOrEmpty()) {
            val name = userEmail.substringBefore("@")
            greetingTitle.text = "Welcome, $name"
        }


        setupCategoryButtons()
        setupSearchListener()
        renderResorts()


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
            val matchesCategory = (selectedCategory == "All") || resort.categories.contains(selectedCategory)
            val matchesSearch = searchQuery.isEmpty() || resort.name.contains(searchQuery, ignoreCase = true) || resort.beach.contains(searchQuery, ignoreCase = true)
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
    }

    private fun createResortView(resort: Resort): View {
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 24)
        }

        val nameTextView = TextView(this).apply {
            text = resort.name
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }

        val detailsTextView = TextView(this).apply {
            text = "Rating: ${resort.rating} | ${resort.price} | ${resort.beach}"
            setTextColor(Color.parseColor("#CCCCCC"))
            textSize = 13f
            setPadding(0, 4, 0, 8)
        }

        val addButton = Button(this).apply {
            text = "Add to Itinerary"
            setTextColor(Color.WHITE)
            textSize = 12f
            setOnClickListener {
                Toast.makeText(context, "${resort.name} added to itinerary", Toast.LENGTH_SHORT).show()
            }
        }

        cardLayout.addView(nameTextView)
        cardLayout.addView(detailsTextView)
        cardLayout.addView(addButton)

        return cardLayout
    }
}
