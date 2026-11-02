package com.example.tdip

// Model ng resort sa Nasugbu kasama ang presyo, rating, at GPS coordinates.
class Resort(
    val resortId: Int,
    val name: String,
    val location: String,
    val amenities: List<String>,
    val rating: Double,
    val price: String,
    val latitude: Double = 14.0722,
    val longitude: Double = 120.6288,
    var isAvailable: Boolean = true
) {
    // Tinitignan kung available pa ang resort para ma-book.
    fun checkAvailability(): Boolean {
        return isAvailable
    }

    // Nilalagay sa fully booked status kapag reserved na.
    fun markBooked() {
        isAvailable = false
    }

    // Kinukuha ang exact GPS coordinates para hindi maligaw sa mapa.
    fun getGpsCoordinates(): String {
        return String.format("%.4f° N, %.4f° E", latitude, longitude)
    }
}
