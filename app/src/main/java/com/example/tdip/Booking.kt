package com.example.tdip

// Resibo at record ng check-in at check-out dates sa napiling resort.
class Booking(
    val bookingId: Int,
    val resort: Resort,
    val checkInDate: String,
    val checkOutDate: String,
    var status: String = "Pending"
) {
    // Kinukumpirma ang reservation kapag okay na ang dates at payment.
    fun confirm() {
        status = "Confirmed"
        resort.markBooked()
    }
}
