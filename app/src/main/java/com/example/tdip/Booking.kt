package com.example.tdip

class Booking(
    val bookingId: Int,
    val resort: Resort,
    val checkInDate: String,
    val checkOutDate: String,
    var status: String = "Pending",
    val touristEmail: String = "",
    val totalPrice: Double = 0.0,
    val pax: Int = 1
) {
    fun confirm() {
        status = "Confirmed"
        resort.markBooked()
    }
}
