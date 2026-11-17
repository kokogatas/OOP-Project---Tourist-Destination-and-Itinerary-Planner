package com.example.tdip

object UserManager {
    private val users = mutableListOf<Person>()

    init {
        seedDefaultUsers()
    }

    private fun seedDefaultUsers() {
        if (users.none { it.email.equals("tourist@example.com", true) }) {
            users.add(Tourist(1, "Mico Tourist", "tourist@example.com", "password123"))
        }
        if (users.none { it.email.equals("admin@example.com", true) }) {
            users.add(ResortAdmin(2, "Admin User", "admin@example.com", "admin123", "Canyon Cove Hotel & Spa"))
        }
    }

    fun clearUsers() {
        users.clear()
        seedDefaultUsers()
    }

    fun addUser(person: Person) {
        users.removeIf { it.email.equals(person.email, ignoreCase = true) }
        users.add(person)
    }

    fun login(emailOrName: String, pass: String, selectedRole: String): Person? {
        val cleanInput = emailOrName.trim()
        val cleanPass = pass.trim()

        val foundUser = users.find {
            (it.email.equals(cleanInput, ignoreCase = true) || it.name.equals(cleanInput, ignoreCase = true))
                    && it.verifyPassword(cleanPass)
        }
        if (foundUser == null) return null

        return when (selectedRole) {
            "Admin" -> if (foundUser is ResortAdmin) foundUser else null
            else -> if (foundUser is Tourist) foundUser else null
        }
    }
}
