package com.example.tdip

// Model para sa mga customer na gustong mag-vacation at mag-swimming sa Nasugbu.
class Tourist(
    userId: Int,
    name: String,
    email: String,
    password: String
) : Person(userId, name, email, password) {

    val itineraryList = mutableListOf<Itinerary>()

    fun login(): Boolean {
        return true
    }

    // Pang-filter at pang-search ng resorts sa Nasugbu.
    fun searchResort(query: String, allResorts: List<Resort>): List<Resort> {
        if (query.isEmpty()) return allResorts
        return allResorts.filter {
            it.name.contains(query, ignoreCase = true) || it.location.contains(query, ignoreCase = true)
        }
    }
}
