package com.example.tdip

import android.os.Handler
import android.os.Looper
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// GitHubCloudManager: Ginawang totoong cloud database ang GitHub repository. Galawan natin 'to bossing!
object GitHubCloudManager {

    private const val GITHUB_OWNER = "kokogatas"
    private const val GITHUB_REPO = "OOP-Project---Tourist-Destination-and-Itinerary-Planner"
    private const val USERS_FILE_PATH = "data/users.json"
    private const val BOOKINGS_FILE_PATH = "data/bookings.json"
    
    private const val USERS_RAW_URL = "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/main/$USERS_FILE_PATH"
    private const val BOOKINGS_RAW_URL = "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/main/$BOOKINGS_FILE_PATH"

    // Kinukuha ang listahan ng mga user sa GitHub Cloud para updated palagi.
    fun fetchUsersFromGitHub(onResult: (Boolean, String) -> Unit) {
        Thread {
            try {
                val cacheBustUrl = "$USERS_RAW_URL?t=${System.currentTimeMillis()}"
                val url = URL(cacheBustUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "TDIP-Android-App")
                connection.useCaches = false
                connection.connectTimeout = 4000
                connection.readTimeout = 4000

                if (connection.responseCode == 200) {
                    val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                    parseAndLoadUsers(jsonText)
                    
                    Handler(Looper.getMainLooper()).post {
                        onResult(true, "Successfully synced users from GitHub Cloud!")
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        onResult(false, "Users cloud file not found on GitHub.")
                    }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    onResult(false, "Could not fetch users from cloud: ${e.message}")
                }
            }
        }.start()
    }

    // Kinukuha ang live bookings sa GitHub API nang direkta para walang CDN cache delay (real-time 3s update yarn).
    fun fetchBookingsFromGitHub(onResult: (Boolean, String) -> Unit) {
        Thread {
            try {
                val token = CloudKeyManager.getDecryptedToken()
                val apiUrl = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/contents/$BOOKINGS_FILE_PATH?t=${System.currentTimeMillis()}"
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "TDIP-Android-App")
                connection.setRequestProperty("Accept", "application/vnd.github.raw+json")
                if (token.isNotEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer $token")
                }
                connection.useCaches = false
                connection.connectTimeout = 4000
                connection.readTimeout = 4000

                if (connection.responseCode == 200) {
                    val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                    parseAndLoadBookings(jsonText)
                    
                    Handler(Looper.getMainLooper()).post {
                        onResult(true, "Successfully synced live bookings from GitHub Cloud!")
                    }
                } else {
                    fetchBookingsFromRawUrl(onResult)
                }
            } catch (e: Exception) {
                fetchBookingsFromRawUrl(onResult)
            }
        }.start()
    }

    private fun fetchBookingsFromRawUrl(onResult: (Boolean, String) -> Unit) {
        try {
            val cacheBustUrl = "$BOOKINGS_RAW_URL?t=${System.currentTimeMillis()}"
            val url = URL(cacheBustUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "TDIP-Android-App")
            connection.useCaches = false
            connection.connectTimeout = 4000
            connection.readTimeout = 4000

            if (connection.responseCode == 200) {
                val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                parseAndLoadBookings(jsonText)
                
                Handler(Looper.getMainLooper()).post {
                    onResult(true, "Successfully synced bookings from RAW GitHub Cloud!")
                }
            } else {
                Handler(Looper.getMainLooper()).post {
                    onResult(false, "Bookings file not found on GitHub.")
                }
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                onResult(false, "Could not fetch bookings: ${e.message}")
            }
        }
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
                        val resort = obj.optString("resortName", "Resort")
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
                val id = obj.optInt("bookingId", 100)
                val resortId = obj.optInt("resortId", 1)
                val resortName = obj.optString("resortName", "Resort")
                val checkIn = obj.optString("checkInDate", "2026-03-01")
                val checkOut = obj.optString("checkOutDate", "2026-03-03")
                val status = obj.optString("status", "Confirmed")

                val dummyResort = Resort(resortId, resortName, "Nasugbu, Batangas", listOf("All"), 4.5, "Standard")
                val booking = Booking(id, dummyResort, checkIn, checkOut, status)
                newList.add(booking)
            }
            BookingManager.setAllBookings(newList)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getUsersAsJson(): String {
        val jsonArray = JSONArray()
        for (user in UserManager.getAllUsers()) {
            val obj = JSONObject()
            obj.put("userId", user.userId)
            obj.put("name", user.name)
            obj.put("email", user.email)
            obj.put("password", user.password)
            
            if (user is ResortAdmin) {
                obj.put("role", "Admin")
                obj.put("resortName", user.resortName)
            } else {
                obj.put("role", "Customer")
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString(2)
    }

    fun uploadUsersToGitHub(githubToken: String, onResult: (Boolean, String) -> Unit) {
        uploadFileToGitHub(USERS_FILE_PATH, getUsersAsJson(), githubToken, onResult)
    }

    fun uploadBookingsToGitHub(githubToken: String, onResult: (Boolean, String) -> Unit) {
        uploadFileToGitHub(BOOKINGS_FILE_PATH, BookingManager.getBookingsAsJson(), githubToken, onResult)
    }

    // Ini-upload ang updated JSON payload sa GitHub REST API via PUT request.
    private fun uploadFileToGitHub(filePath: String, contentJson: String, githubToken: String, onResult: (Boolean, String) -> Unit) {
        Thread {
            try {
                val apiUrl = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/contents/$filePath"
                val sha = getFileSha(apiUrl, githubToken)
                val base64Content = Base64.encodeToString(contentJson.toByteArray(), Base64.NO_WRAP)

                val requestBody = JSONObject()
                requestBody.put("message", "Cloud Sync: Update $filePath")
                requestBody.put("content", base64Content)
                if (sha.isNotEmpty()) {
                    requestBody.put("sha", sha)
                }

                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "PUT"
                connection.setRequestProperty("User-Agent", "TDIP-Android-App")
                connection.setRequestProperty("Authorization", "Bearer $githubToken")
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                connection.doOutput = true

                OutputStreamWriter(connection.outputStream).use { it.write(requestBody.toString()) }

                if (connection.responseCode in 200..201) {
                    Handler(Looper.getMainLooper()).post {
                        onResult(true, "Successfully uploaded $filePath to GitHub Cloud!")
                    }
                } else {
                    val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    Handler(Looper.getMainLooper()).post {
                        onResult(false, "Upload failed (HTTP ${connection.responseCode}): $err")
                    }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    onResult(false, "Upload error: ${e.message}")
                }
            }
        }.start()
    }

    private fun getFileSha(apiUrl: String, token: String): String {
        return try {
            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "TDIP-Android-App")
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(json).optString("sha", "")
            } else ""
        } catch (e: Exception) {
            ""
        }
    }
}
