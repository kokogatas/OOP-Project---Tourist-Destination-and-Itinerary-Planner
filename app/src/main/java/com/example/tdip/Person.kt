package com.example.tdip

// Base model para sa lahat ng users. Dito nakatago ang name, email, at password ehe.
open class Person(
    val userId: Int,
    val name: String,
    val email: String,
    val password: String
) {
    // check kung tama ang tinype na password bago papasukin
    fun verifyPassword(inputPassword: String): Boolean {
        return this.password == inputPassword
    }
}
