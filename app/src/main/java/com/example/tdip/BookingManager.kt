package com.example.tdip

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object BookingManager {

    private val allBookings = mutableListOf<Booking>()
    private val touristItineraries = mutableMapOf<String, Itinerary>()

    private val possibleDateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    )

    private fun parseDate(dateStr: String): Date? {
        for (format in possibleDateFormats) {
            try {
                return format.parse(dateStr)
            } catch (e: Exception) {
            }
        }
        if (dateStr.length >= 10) {
            try {
                return SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr.substring(0, 10))
            } catch (e: Exception) {
            }
        }
        return null
    }

    fun getAllBookings(): List<Booking> {
        return allBookings
    }

    fun setAllBookings(newBookings: List<Booking>) {
        val uniqueBookings = newBookings.distinctBy { it.bookingId }
        allBookings.clear()
        allBookings.addAll(uniqueBookings)
    }

    fun isInvalidDateRange(checkInStr: String, checkOutStr: String): String? {
        val checkInDate = parseDate(checkInStr) ?: return "Invalid check-in date format: '$checkInStr'."
        val checkOutDate = parseDate(checkOutStr) ?: return "Invalid check-out date format: '$checkOutStr'."

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today = cal.time

        val inCal = Calendar.getInstance().apply {
            time = checkInDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val cleanCheckIn = inCal.time

        val outCal = Calendar.getInstance().apply {
            time = checkOutDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val cleanCheckOut = outCal.time

        if (cleanCheckIn.before(today)) {
            return "Invalid Date! Check-in date cannot be in the past."
        }

        if (cleanCheckOut.before(cleanCheckIn) || cleanCheckOut == cleanCheckIn) {
            return "Invalid Date Range! Check-out date must be after check-in date."
        }

        return null
    }

    fun hasDateConflict(resortId: Int, checkInStr: String, checkOutStr: String): Boolean {
        val newIn = parseDate(checkInStr) ?: return false
        val newOut = parseDate(checkOutStr) ?: return false

        return allBookings.any { existing ->
            if (existing.resort.resortId == resortId && !existing.status.equals("Cancelled", true)) {
                val existingIn = parseDate(existing.checkInDate)
                val existingOut = parseDate(existing.checkOutDate)

                if (existingIn != null && existingOut != null) {
                    newIn.before(existingOut) && newOut.after(existingIn)
                } else false
            } else false
        }
    }

    fun extractPax(paymentMethod: String): Int {
        val regex = Regex("([0-9]+)\\s*Pax", RegexOption.IGNORE_CASE)
        val match = regex.find(paymentMethod)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 1
    }

    fun calculateNights(checkInStr: String, checkOutStr: String): Int {
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

    private fun generateBookingId(checkInStr: String, checkOutStr: String): Int {
        return try {
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val checkInDate = df.parse(checkInStr)
            val checkOutDate = df.parse(checkOutStr)
            val calIn = Calendar.getInstance()
            val calOut = Calendar.getInstance()
            if (checkInDate != null) calIn.time = checkInDate
            if (checkOutDate != null) calOut.time = checkOutDate

            val inDay = calIn.get(Calendar.DAY_OF_MONTH)
            val outDay = calOut.get(Calendar.DAY_OF_MONTH)
            val customerQueue = (allBookings.size + 1) % 99

            val rawId = (inDay * 10000) + (outDay * 100) + customerQueue
            100000 + (rawId % 899999)
        } catch (e: Exception) {
            ((System.currentTimeMillis() % 899999) + 100000).toInt()
        }
    }

    fun createBooking(
        touristEmail: String,
        resort: Resort,
        checkInStr: String,
        checkOutStr: String,
        paymentMethod: String
    ): BookingResult {

        val dateError = isInvalidDateRange(checkInStr, checkOutStr)
        if (dateError != null) {
            return BookingResult.Failure(dateError)
        }

        val basePrice = resort.price.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 2500.0
        val nights = calculateNights(checkInStr, checkOutStr)
        val paxNum = extractPax(paymentMethod)
        val computedTotal = basePrice * paxNum * nights

        val uniqueBookingId = generateBookingId(checkInStr, checkOutStr)

        val payment: Payment = if (paymentMethod.contains("Card", ignoreCase = true)) {
            val gateway: PaymentProcessing = CardGateway()
            val success = gateway.process(computedTotal)
            CardPayment(uniqueBookingId, if (success) computedTotal else 0.0)
        } else {
            CashPayment(uniqueBookingId, computedTotal)
        }
        payment.pay()

        val newBooking = Booking(
            bookingId = uniqueBookingId,
            resort = resort,
            checkInDate = checkInStr,
            checkOutDate = checkOutStr,
            status = "Pending",
            touristEmail = touristEmail,
            totalPrice = computedTotal,
            pax = paxNum
        )

        allBookings.add(newBooking)

        val userItinerary = touristItineraries.getOrPut(touristEmail) {
            Itinerary(1, checkInStr, checkOutStr)
        }
        userItinerary.addBooking(newBooking)

        return BookingResult.Success(newBooking)
    }

    fun getTouristBookings(touristEmail: String): List<Booking> {
        val matches = allBookings.filter { 
            (it.touristEmail.equals(touristEmail, ignoreCase = true) || it.touristEmail.isEmpty()) &&
            !it.status.equals("Cancelled", true)
        }
        return matches
    }

    fun getOfficialResort(resortId: Int, resortName: String): Resort {
        val officialResorts = listOf(
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
        return officialResorts.find { it.resortId == resortId || it.name.equals(resortName, true) }
            ?: officialResorts.find { it.name.contains(resortName, true) || resortName.contains(it.name, true) }
            ?: officialResorts[4]
    }

    fun getBookingsForResort(resortName: String): List<Booking> {
        val cleanAdminResort = resortName.lowercase().trim()
        val targetResortId = getResortIdByName(resortName)
        return allBookings.filter { booking ->
            if (booking.status.equals("Cancelled", true)) {
                false
            } else {
                val bookingResortName = booking.resort.name.lowercase().trim()
                val adminKeyword = cleanAdminResort.split(" ").firstOrNull() ?: cleanAdminResort
                val bookingKeyword = bookingResortName.split(" ").firstOrNull() ?: bookingResortName

                bookingResortName.contains(cleanAdminResort) ||
                cleanAdminResort.contains(bookingResortName) ||
                adminKeyword == bookingKeyword ||
                booking.resort.resortId == targetResortId
            }
        }
    }

    private fun getResortIdByName(name: String): Int {
        val n = name.lowercase()
        return when {
            n.contains("berna") -> 1
            n.contains("calayo") -> 2
            n.contains("sand") -> 3
            n.contains("pamana") -> 4
            n.contains("canyon") -> 5
            n.contains("pico sands") -> 6
            n.contains("fuego") -> 7
            n.contains("shoreland") -> 8
            n.contains("rsam") -> 9
            n.contains("loro") -> 10
            else -> 5
        }
    }

    fun cancelBooking(bookingId: Int): Boolean {
        val booking = allBookings.find { it.bookingId == bookingId }
        if (booking != null) {
            booking.status = "Cancelled"
            booking.resort.isAvailable = true
            allBookings.removeAll { it.bookingId == bookingId }
            return true
        }
        allBookings.removeAll { it.bookingId == bookingId }
        return false
    }

    fun getBookingsAsJson(): String {
        val jsonArray = JSONArray()
        for (booking in allBookings) {
            if (!booking.status.equals("Cancelled", true)) {
                val obj = JSONObject()
                obj.put("bookingid", booking.bookingId)
                obj.put("resortid", booking.resort.resortId)
                obj.put("resortname", booking.resort.name)
                obj.put("checkindate", booking.checkInDate)
                obj.put("checkoutdate", booking.checkOutDate)
                obj.put("status", booking.status)
                obj.put("touristemail", booking.touristEmail)
                obj.put("totalprice", booking.totalPrice)
                obj.put("pax", booking.pax)
                jsonArray.put(obj)
            }
        }
        return jsonArray.toString(2)
    }

    sealed class BookingResult {
        data class Success(val booking: Booking) : BookingResult()
        data class Failure(val errorMessage: String) : BookingResult()
    }
}
