package com.example.musicplayer

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var musicAdapter: MusicAdapter

    companion object {
        lateinit var MusicListMA : ArrayList<Music>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeLayout()


        binding.shuffleBtn.setOnClickListener {
            val intent = Intent(this@MainActivity, PlayerActivity::class.java)
            startActivity(intent)
        }
        binding.favoriteBtn.setOnClickListener {
            val intent = Intent(this@MainActivity, FavouriteActivity::class.java)
            startActivity(intent)
        }
        binding.playlistBtn.setOnClickListener {
            val intent = Intent(this@MainActivity, PlaylistActivity::class.java)
            startActivity(intent)
        }
        binding.navView.setNavigationItemSelectedListener {
            when(it.itemId) {
                R.id.navSettings -> Toast.makeText(baseContext, "Cài đặt", Toast.LENGTH_SHORT).show()

                R.id.navAbout -> Toast.makeText(baseContext, "Về chúng tôi", Toast.LENGTH_SHORT).show()
                R.id.navExit -> finish()
            }
            true

        }


    }
    private fun requestRuntimePermission(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 13)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        if (requestCode == 13) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Granted", Toast.LENGTH_SHORT).show()
            else ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 13)
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item))
            return true
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("SetTextI18n")
    private fun initializeLayout() {
        requestRuntimePermission()
        setTheme(R.style.Theme_MusicPlayer)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        toggle = ActionBarDrawerToggle(this, binding.root, R.string.open, R.string.close)
        binding.root.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val musicList = ArrayList<String>()
        MusicListMA = getAllAudio()
        binding.musicRV.setHasFixedSize(true)
        binding.musicRV.setItemViewCacheSize(13)
        binding.musicRV.layoutManager = LinearLayoutManager(this@MainActivity)
        musicAdapter = MusicAdapter(this@MainActivity, MusicListMA)
        binding.musicRV.adapter = musicAdapter
        binding.totalSongs.text = "Số bài hát : " + musicAdapter.itemCount

    }
    @SuppressLint("Range")
    private fun getAllAudio() : ArrayList<Music> {
        val tempList = ArrayList<Music>()
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATE_ADDED, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM_ID)
        val cursor = this.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, MediaStore.Audio.Media.DATE_ADDED + " DESC", null )
        if (cursor != null) {
            if (cursor.moveToFirst())
                do {
                    var titleC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
                    var idC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID))
                    var albumC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))
                    var artistC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                    var pathC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                    var durationC = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                    var albumIdC = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)).toString()
                    val uri = Uri.parse("content://media/external/audio/albumart")
                    val artUriC = Uri.withAppendedPath(uri, albumIdC).toString()
                    val music = Music(id = idC, title = titleC, album = albumC, artist = artistC, path = pathC, duration = durationC, artUri = artUriC)
                    val file = File(music.path)
                    if (file.exists())
                        tempList.add(music)

                } while (cursor.moveToNext())
                cursor.close()
        }
        return tempList
    }
}