package com.example.musicplayer

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import okhttp3.Request

class SpotifyAPI {
    private val client = OkHttpClient()

    private suspend fun fetchTokenFromSpotify(): String? {
        val clientId = "97ad3e77c26a4930a0df2df09d1664f2"
        val clientSecret = "9a132f6508d642d7816c15847b5f08b1"
        val encodedCredentials = Base64.encodeToString(
            "$clientId:$clientSecret".toByteArray(),
            Base64.NO_WRAP
        )

        val url = "https://accounts.spotify.com/api/token"
        val contentType = "application/x-www-form-urlencoded"
        val body = "grant_type=client_credentials".toRequestBody(contentType.toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Basic $encodedCredentials")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                json.getString("access_token") // return the access token
            } else {
                null // return null if error
            }
        }
    }

    suspend fun searchTrack(query : String) : List<Map<String, String>> {
        val token = fetchTokenFromSpotify() ?: ""

        val encoded_query = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.spotify.com/v1/search?q=$encoded_query&type=track&limit=5"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                val tracks = json.getJSONObject("tracks").getJSONArray("items")
                val trackList = mutableListOf<Map<String, String>>()
                for (i in 0 until tracks.length()) {
                    val track = tracks.getJSONObject(i)
                    val id = track.getString("id")
                    val name = track.getString("name")
                    val album = track.getJSONObject("album")
                    val image = album.getJSONArray("images").getJSONObject(0).getString("url")
                    val artistName = track.getJSONArray("artists").getJSONObject(0).getString("name")
                    val trackInfo = mapOf(
                        "id" to id,
                        "name" to name,
                        "image" to image,
                        "artistName" to artistName,
                    )
                    trackList.add(trackInfo)
                }
                trackList
            } else {
                emptyList()
            }
        }
    }
}