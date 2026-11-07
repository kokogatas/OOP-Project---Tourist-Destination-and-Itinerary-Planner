package com.example.tdip

// ResortAdmin inherits from Person
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
