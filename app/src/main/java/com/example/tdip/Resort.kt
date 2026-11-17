package com.example.tdip

class Resort(
    val resortId: Int,
    val name: String,
    val location: String,
    val amenities: List<String>,
    val rating: Double,
    val price: String,
    val latitude: Double,
    val longitude: Double,
    var isAvailable: Boolean = true,
    val checkInTime: String = "2:00 PM",
    val checkOutTime: String = "12:00 NN",
    val dayTourSchedule: String = "N/A",
    val frontDeskHours: String = "Standard Hours"
) {
    val bookings = mutableListOf<Booking>()

    fun checkAvailability(): Boolean {
        return isAvailable
    }

    fun markBooked() {
        isAvailable = false
    }

    fun getGpsCoordinates(): String {
        return String.format("%.4f° N, %.4f° E", latitude, longitude)
    }
}
