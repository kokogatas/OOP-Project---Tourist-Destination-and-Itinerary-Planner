package com.example.tdip

// Model para sa resort admin na taga-approve at taga-manage ng guest bookings.
class ResortAdmin(
    userId: Int,
    name: String,
    email: String,
    password: String,
    val resortName: String
) : Person(userId, name, email, password) {

    fun login(): Boolean {
        return true
    }
}
