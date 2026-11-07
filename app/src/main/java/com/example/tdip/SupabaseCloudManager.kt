package com.example.tdip

import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object SupabaseCloudManager {

    private const val SUPABASE_URL = "https://wrzfejimxuodjsoctjwd.supabase.co/rest/v1"


    // wag pakealaman to pless <3
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndyemZlamlteHVvZGpzb2N0andkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTQwNjQyODIsImV4cCI6MjEwOTY0MDI4Mn0.IB-8AKP6VMh8pGy0zFRSiKV5GdFveukLfuUnwT9U0ZI"


    fun fetchUsersFromSupabase(onResult: (Boolean, String) -> Unit) {
        Thread {
            try {
                val cleanKey = SUPABASE_ANON_KEY.trim()
                val endpoint = "$SUPABASE_URL/users?select=*"
                val url = URL(endpoint)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("apikey", cleanKey)
                connection.setRequestProperty("Authorization", "Bearer $cleanKey")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode in 200..201) {
                    val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                    parseAndLoadUsers(jsonText)
                    
                    Handler(Looper.getMainLooper()).post {
                        onResult(true, "Users synced from Cloud!")
                    }
                } else {
                    val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    Log.e("SupabaseCloud", "GET Users error ${connection.responseCode}: $err")
                    Handler(Looper.getMainLooper()).post {
                        onResult(false, "HTTP ${connection.responseCode}")
                    }
                }
            } catch (e: Exception) {
                Log.e("SupabaseCloud", "GET Users exception: ${e.message}")
                Handler(Looper.getMainLooper()).post {
                    onResult(false, "Offline mode")
                }
            }
        }.start()
    }

    fun fetchBookingsFromSupabase(onResult: (Boolean, String) -> Unit) {
        Thread {
            try {
                val cleanKey = SUPABASE_ANON_KEY.trim()
                val endpoint = "$SUPABASE_URL/bookings?select=*"
                val url = URL(endpoint)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("apikey", cleanKey)
                connection.setRequestProperty("Authorization", "Bearer $cleanKey")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode in 200..201) {
                    val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                    parseAndLoadBookings(jsonText)
                    
                    Handler(Looper.getMainLooper()).post {
                        onResult(true, "Bookings synced from Cloud!")
                    }
                } else {
                    val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    Log.e("SupabaseCloud", "GET Bookings error ${connection.responseCode}: $err")
                    Handler(Looper.getMainLooper()).post {
                        onResult(false, "HTTP ${connection.responseCode}")
                    }
                }
            } catch (e: Exception) {
                Log.e("SupabaseCloud", "GET Bookings exception: ${e.message}")
                Handler(Looper.getMainLooper()).post {
                    onResult(false, "Offline mode")
                }
            }
        }.start()
    }

    private fun parseAndLoadUsers(jsonText: String) {
        try {
            val jsonArray = JSONArray(jsonText)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val role = obj.optString("role", "Customer")
                val name = obj.optString("name", "")
                val email = obj.optString("email", "")
                val pass = obj.optString("password", "")

                if (email.isNotEmpty()) {
                    if (role == "Admin") {
                        val resort = if (obj.has("resortname")) obj.optString("resortname") else obj.optString("resortName", "Resort")
                        UserManager.registerAdmin(name, email, pass, resort)
                    } else {
                        UserManager.registerTourist(name, email, pass)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseAndLoadBookings(jsonText: String) {
        try {
            val jsonArray = JSONArray(jsonText)
            val newList = mutableListOf<Booking>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = if (obj.has("bookingid")) obj.optInt("bookingid") else obj.optInt("bookingId", 100)
                val resortId = if (obj.has("resortid")) obj.optInt("resortid") else obj.optInt("resortId", 1)
                val resortName = if (obj.has("resortname")) obj.optString("resortname") else obj.optString("resortName", "Resort")
                val checkIn = if (obj.has("checkindate")) obj.optString("checkindate") else obj.optString("checkInDate", "2026-03-01")
                val checkOut = if (obj.has("checkoutdate")) obj.optString("checkoutdate") else obj.optString("checkOutDate", "2026-03-03")
                val status = obj.optString("status", "Pending")
                val email = if (obj.has("touristemail")) obj.optString("touristemail") else obj.optString("email", "")

                val dummyResort = Resort(resortId, resortName, "Nasugbu, Batangas", listOf("All"), 4.5, "Standard")
                val booking = Booking(id, dummyResort, checkIn, checkOut, status, email)
                newList.add(booking)
            }
            BookingManager.setAllBookings(newList)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun uploadUserToSupabase(user: Person, onResult: (Boolean, String) -> Unit) {
        Thread {
            try {
                val cleanKey = SUPABASE_ANON_KEY.trim()
                val endpoint = "$SUPABASE_URL/users"
                val url = URL(endpoint)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("apikey", cleanKey)
                connection.setRequestProperty("Authorization", "Bearer $cleanKey")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Prefer", "return=representation")
                connection.doOutput = true

                val obj = JSONObject()
                val uniqueId = (10000..99999).random()
                obj.put("userid", uniqueId)
                obj.put("name", user.name)
                obj.put("email", user.email)
                obj.put("password", user.password)
                
                if (user is ResortAdmin) {
                    obj.put("role", "Admin")
                    obj.put("resortname", user.resortName)
                } else {
                    obj.put("role", "Customer")
                }

                OutputStreamWriter(connection.outputStream).use { it.write(obj.toString()) }

                if (connection.responseCode in 200..201) {
                    Handler(Looper.getMainLooper()).post {
                        onResult(true, "User uploaded to Cloud!")
                    }
                } else {
                    val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    Log.e("SupabaseCloud", "POST Users error ${connection.responseCode}: $err")
                    Handler(Looper.getMainLooper()).post {
                        onResult(false, "User registered locally.")
                    }
                }
            } catch (e: Exception) {
                Log.e("SupabaseCloud", "POST Users exception: ${e.message}")
                Handler(Looper.getMainLooper()).post {
                    onResult(false, "User registered locally.")
                }
            }
        }.start()
    }

    fun uploadBookingToSupabase(booking: Booking, onResult: (Boolean, String) -> Unit) {
        Thread {
            try {
                val cleanKey = SUPABASE_ANON_KEY.trim()
                val endpoint = "$SUPABASE_URL/bookings"
                val url = URL(endpoint)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("apikey", cleanKey)
                connection.setRequestProperty("Authorization", "Bearer $cleanKey")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Prefer", "return=representation")
                connection.doOutput = true

                val obj = JSONObject()
                obj.put("bookingid", booking.bookingId)
                obj.put("resortid", booking.resort.resortId)
                obj.put("resortname", booking.resort.name)
                obj.put("checkindate", booking.checkInDate)
                obj.put("checkoutdate", booking.checkOutDate)
                obj.put("status", booking.status)
                obj.put("touristemail", booking.touristEmail)

                OutputStreamWriter(connection.outputStream).use { it.write(obj.toString()) }

                if (connection.responseCode in 200..201) {
                    Handler(Looper.getMainLooper()).post {
                        onResult(true, "Booking saved to Cloud!")
                    }
                } else {
                    val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    Log.e("SupabaseCloud", "POST Bookings error ${connection.responseCode}: $err")
                    Handler(Looper.getMainLooper()).post {
                        onResult(false, "Booking saved locally.")
                    }
                }
            } catch (e: Exception) {
                Log.e("SupabaseCloud", "POST Bookings exception: ${e.message}")
                Handler(Looper.getMainLooper()).post {
                    onResult(false, "Booking saved locally.")
                }
            }
        }.start()
    }

    fun deleteBookingFromSupabase(bookingId: Int, onResult: (Boolean, String) -> Unit) {
        Thread {
            try {
                val cleanKey = SUPABASE_ANON_KEY.trim()
                val endpoint = "$SUPABASE_URL/bookings?bookingid=eq.$bookingId"
                val url = URL(endpoint)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "DELETE"
                connection.setRequestProperty("apikey", cleanKey)
                connection.setRequestProperty("Authorization", "Bearer $cleanKey")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode in 200..204) {
                    Handler(Looper.getMainLooper()).post {
                        onResult(true, "Booking deleted from Cloud!")
                    }
                } else {
                    val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    Log.e("SupabaseCloud", "DELETE Bookings error ${connection.responseCode}: $err")
                    Handler(Looper.getMainLooper()).post {
                        onResult(false, "Booking cancelled locally.")
                    }
                }
            } catch (e: Exception) {
                Log.e("SupabaseCloud", "DELETE Bookings exception: ${e.message}")
                Handler(Looper.getMainLooper()).post {
                    onResult(false, "Booking cancelled locally.")
                }
            }
        }.start()
    }

    fun updateBookingStatusInSupabase(bookingId: Int, newStatus: String, onResult: (Boolean, String) -> Unit) {
        Thread {
            try {
                val cleanKey = SUPABASE_ANON_KEY.trim()
                val endpoint = "$SUPABASE_URL/bookings?bookingid=eq.$bookingId"
                val url = URL(endpoint)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "PATCH"
                connection.setRequestProperty("apikey", cleanKey)
                connection.setRequestProperty("Authorization", "Bearer $cleanKey")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val obj = JSONObject()
                obj.put("status", newStatus)
                OutputStreamWriter(connection.outputStream).use { it.write(obj.toString()) }

                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode in 200..204) {
                    Handler(Looper.getMainLooper()).post {
                        onResult(true, "Booking status updated to $newStatus in Cloud!")
                    }
                } else {
                    val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    Log.e("SupabaseCloud", "PATCH Bookings error ${connection.responseCode}: $err")
                    Handler(Looper.getMainLooper()).post {
                        onResult(false, "Booking updated locally.")
                    }
                }
            } catch (e: Exception) {
                Log.e("SupabaseCloud", "PATCH Bookings exception: ${e.message}")
                Handler(Looper.getMainLooper()).post {
                    onResult(false, "Booking updated localy.")
                }
            }
        }.start()
    }
}
