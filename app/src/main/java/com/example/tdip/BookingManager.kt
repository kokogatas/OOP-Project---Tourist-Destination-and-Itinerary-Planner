package com.example.tdip

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Taga-check ng dates, nag-iwas sa double booking, at nag-co-convert ng JSON para sa cloud sync.
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
                // Try next format
            }
        }
        if (dateStr.length >= 10) {
            try {
                return SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr.substring(0, 10))
            } catch (e: Exception) {
                // Ignore
            }
        }
        return null
    }

    fun getAllBookings(): List<Booking> {
        return allBookings
    }

    // Set and deduplicate bookings by bookingId
    fun setAllBookings(newBookings: List<Booking>) {
        val uniqueBookings = newBookings.distinctBy { it.bookingId }
        allBookings.clear()
        allBookings.addAll(uniqueBookings)
    }

    // Pinipigilan ang pag-book sa nakalipas na petsa o kapag mas nauna ang check-out kaysa check-in.
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

    // Chine-check kung may kaparehong petsa na na-book na sa parehong resort.
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

    // Gumagawa ng bagong booking na may unique booking ID at initial status na 'Pending'
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

        if (hasDateConflict(resort.resortId, checkInStr, checkOutStr)) {
            return BookingResult.Failure("Date Conflict! This resort is already booked for the selected dates.")
        }

        val uniqueBookingId = ((System.currentTimeMillis() % 899999) + 100000).toInt()

        val payment: Payment = if (paymentMethod.contains("Card", ignoreCase = true)) {
            val gateway: PaymentProcessing = CardGateway()
            val success = gateway.process(3500.0)
            CardPayment(uniqueBookingId, if (success) 3500.0 else 0.0)
        } else {
            CashPayment(uniqueBookingId, 3500.0)
        }
        payment.pay()

        val newBooking = Booking(
            bookingId = uniqueBookingId,
            resort = resort,
            checkInDate = checkInStr,
            checkOutDate = checkOutStr,
            status = "Pending",
            touristEmail = touristEmail
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

    fun getBookingsForResort(resortName: String): List<Booking> {
        return allBookings.filter { 
            (it.resort.name.contains(resortName, ignoreCase = true) || resortName.contains(it.resort.name, ignoreCase = true)) &&
            !it.status.equals("Cancelled", true)
        }
    }

    // Nagka-cancel ng booking at nagtatanggal nito sa active list.
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

    // Gagawing JSON format para mai-push sa Cloud database.
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
