package com.example.tdip

// Listahan ng mga naka-planong bakasyon at bookings ng customer.
class Itinerary(
    val itineraryId: Int,
    val startDate: String,
    val endDate: String
) {
    private val bookings = mutableListOf<Booking>()

    fun getBookings(): List<Booking> {
        return bookings
    }

    // Nagdadagdag ng bagong booking sa itinerary list.
    fun addBooking(booking: Booking) {
        bookings.add(booking)
    }

    // Nagtatanggal ng booking kapag na-cancel ang lakad.
    fun removeBooking(bookingId: Int) {
        bookings.removeAll { it.bookingId == bookingId }
    }
}
