package com.example.tdip

// Taga-lista at taga-check ng accounts sa memory.
object UserManager {
    private val users = mutableListOf<Person>()
    private var idCounter = 1

    init {
        // Default test accounts para sa login testing.
        users.add(Tourist(idCounter++, "Mico Tourist", "tourist@example.com", "password123"))
        users.add(ResortAdmin(idCounter++, "Admin User", "admin@example.com", "admin123", "Canyon Cove Resort"))
    }

    fun getAllUsers(): List<Person> {
        return users
    }

    // Gumagawa at nagtatabi ng bagong Customer account.
    fun registerTourist(name: String, email: String, pass: String): Tourist {
        val cleanEmail = email.trim()
        val cleanName = name.trim()

        val existingIndex = users.indexOfFirst {
            it.email.equals(cleanEmail, ignoreCase = true) || it.name.equals(cleanName, ignoreCase = true)
        }
        val tourist = Tourist(if (existingIndex >= 0) users[existingIndex].userId else idCounter++, cleanName, cleanEmail, pass)

        if (existingIndex >= 0) {
            users[existingIndex] = tourist
        } else {
            users.add(tourist)
        }
        return tourist
    }

    // Gumagawa at nagtatabi ng bagong Resort Admin account na naka-link sa isang resort.
    fun registerAdmin(name: String, email: String, pass: String, resortName: String): ResortAdmin {
        val cleanEmail = email.trim()
        val cleanName = name.trim()

        val existingIndex = users.indexOfFirst {
            it.email.equals(cleanEmail, ignoreCase = true) || it.name.equals(cleanName, ignoreCase = true)
        }
        val admin = ResortAdmin(if (existingIndex >= 0) users[existingIndex].userId else idCounter++, cleanName, cleanEmail, pass, resortName)

        if (existingIndex >= 0) {
            users[existingIndex] = admin
        } else {
            users.add(admin)
        }
        return admin
    }

    // Chine-check kung tama ang email/username at password sa napiling role.
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
