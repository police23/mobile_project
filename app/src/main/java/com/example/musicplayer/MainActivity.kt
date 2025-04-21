package com.example.musicplayer

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    companion object {
        var MusicListMA = ArrayList<Music>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Set the main layout
        requestRuntimePermission()
    }

    private fun requestRuntimePermission() {
        val permissions = arrayOf(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
        if (permissions.any { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions, 13)
        } else {
            loadMusic() // Call a separate function to load music
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 13) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show()
                loadMusic() // Call a separate function to load music
            } else {
                Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadMusic() {
        MusicListMA = getAllAudio() ?: ArrayList() // Ensure MusicListMA is not null
        if (MusicListMA.isEmpty()) {
            Toast.makeText(this, "No music found", Toast.LENGTH_SHORT).show()
        }
        // Initialize RecyclerView with MusicAdapter regardless of list content
        val recyclerView = findViewById<RecyclerView>(R.id.musicRV)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = MusicAdapter(this, MusicListMA)
    }
    
    private fun getAllAudio(): ArrayList<Music>? {
        val musicList = ArrayList<Music>()
        val contentResolver = contentResolver
        val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            android.provider.MediaStore.Audio.Media._ID,
            android.provider.MediaStore.Audio.Media.TITLE,
            android.provider.MediaStore.Audio.Media.ALBUM,
            android.provider.MediaStore.Audio.Media.ARTIST,
            android.provider.MediaStore.Audio.Media.DATA,
            android.provider.MediaStore.Audio.Media.DURATION,
            android.provider.MediaStore.Audio.Media.ALBUM_ID
        )
        // Removed folder filtering to load all audio files
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getString(it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID))
                    val title = it.getString(it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE))
                    val album = it.getString(it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM))
                    val artist = it.getString(it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST))
                    val path = it.getString(it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA))
                    val duration = it.getLong(it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DURATION))
                    val artUri = Uri.withAppendedPath(
                        Uri.parse("content://media/external/audio/albumart"),
                        it.getString(it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM_ID))
                    ).toString()
    
                    val music = Music(id, title, album, artist, path, duration, artUri)
                    musicList.add(music)
                } while (it.moveToNext())
            }
        }
        return musicList
    }
}
