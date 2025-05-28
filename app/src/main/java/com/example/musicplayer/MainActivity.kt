package com.example.musicplayer

import android.R.attr.type
import android.R.id.toggle
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.musicplayer.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlin.system.exitProcess
import java.lang.reflect.Type
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    companion object {
        lateinit var MusicListMA : ArrayList<Music>
        lateinit var musicListSearch : ArrayList<Music>
        var search: Boolean = false
        var downloaded: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MusicPlayer) // Set the theme before setting the content view
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root) // Set the main layout


        toggle = ActionBarDrawerToggle(this, binding.root, R.string.open, R.string.close)
        binding.root.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //for storing favourites data using shared preferences
        FavouriteActivity.favouriteSongs = ArrayList() // Initialize the favourite songs list
        val editor = getSharedPreferences("FAVOURITES", MODE_PRIVATE)
        val jsonString = editor.getString("FavouriteSongs", null)
        val typeToken = object : TypeToken<ArrayList<Music>>() {}.type

        if (jsonString != null) {
            val data: ArrayList<Music> = GsonBuilder().create().fromJson(jsonString,typeToken)
            FavouriteActivity.favouriteSongs.addAll(data)
        }

        PlaylistActivity.musicPlaylist = MusicPlaylist()
        val jsonStringPlaylist = editor.getString("MusicPlaylist", null)
        if(jsonStringPlaylist != null){
            val dataPlaylist: MusicPlaylist = GsonBuilder().create().fromJson(jsonStringPlaylist, MusicPlaylist::class.java)
            PlaylistActivity.musicPlaylist = dataPlaylist
        }


        binding.shuffleBtn.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("index", 0)
            intent.putExtra("class", "MainActivity")
            startActivity(intent)
            Toast.makeText(this, "Shuffle Button Clicked", Toast.LENGTH_SHORT).show()
        }
        binding.favoriteBtn.setOnClickListener {
            val intent = Intent(this, FavouriteActivity::class.java)
            startActivity(intent)
            Toast.makeText(this, "Favourite Button Clicked", Toast.LENGTH_SHORT).show()
        }
        binding.downloadBtn.setOnClickListener {
            val intent = Intent(this, DownloadActivity::class.java)
            startActivity(intent)
            Toast.makeText(this, "Download Button Clicked", Toast.LENGTH_SHORT).show()
        }
        binding.playlistBtn.setOnClickListener {
            val intent = Intent(this, PlaylistActivity::class.java)
            startActivity(intent)
            Toast.makeText(this, "Playlist Button Clicked", Toast.LENGTH_SHORT).show()
        }
        binding.navView.setOnClickListener {
            when (it.id)
            {
                R.id.navSettings-> {
                    Toast.makeText(this, "Settings Button Clicked", Toast.LENGTH_SHORT).show()
                }
                R.id.navAbout -> {
                    Toast.makeText(this, "About Button Clicked", Toast.LENGTH_SHORT).show()
                }
                R.id.navExit -> exitProcess(1)
            }
            true
        }
        requestRuntimePermission()
    }

    private fun requestRuntimePermission() {
        val permissions = arrayOf(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
        if (permissions.any {
                ActivityCompat.checkSelfPermission(
                    this,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }) {
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

    override fun onDestroy() {

        super.onDestroy()
        //for storing favourites data using shared preferences

    }

    fun loadMusic() {
        MusicListMA = getAllAudio() ?: ArrayList() // Ensure MusicListMA is not null
        if (MusicListMA.isEmpty()) {
            Toast.makeText(this, "No music found", Toast.LENGTH_SHORT).show()
        }


        // Initialize RecyclerView with MusicAdapter regardless of list content
        val recyclerView = findViewById<RecyclerView>(R.id.musicRV)
        recyclerView.setHasFixedSize(true)
        recyclerView.setItemViewCacheSize(13)
        recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
        recyclerView.adapter = MusicAdapter(this@MainActivity, MusicListMA)
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
            Log.d("MusicLoader", "Number of songs found: ${it.count}")
            if (it.moveToFirst()) {
                do {
                    val id =
                        it.getString(it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID))
                    val title =
                        it.getString(it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE))
                    val album =
                        it.getString(it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM))
                    val artist =
                        it.getString(it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST))
                    val path =
                        it.getString(it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA))
                    val duration =
                        it.getLong(it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DURATION))
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

    override fun onResume() {
        super.onResume()
        if (downloaded) {
            loadMusic()
            Toast.makeText(this, "Downloaded songs are available in Downloads section", Toast.LENGTH_SHORT).show()
            downloaded = false
        }
        val editor = getSharedPreferences("FAVOURITES", MODE_PRIVATE).edit()
        val jsonString = GsonBuilder().create().toJson(FavouriteActivity.favouriteSongs)
        editor.putString("FavouriteSongs", jsonString)
        editor.apply()
        val jsonStringPlaylist = GsonBuilder().create().toJson(PlaylistActivity.musicPlaylist)
        editor.putString("MusicPlaylist", jsonStringPlaylist)
        editor.apply()


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item))
            return true
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search_view_menu,menu)
        val searchView = menu?.findItem(R.id.searchView)?.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true

            override fun onQueryTextChange(newText: String?): Boolean {
                Toast.makeText(this@MainActivity, "Searching for: $newText", Toast.LENGTH_SHORT).show()
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }
}
