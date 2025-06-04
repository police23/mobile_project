package com.example.musicplayer

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.databinding.ActivitySelectionBinding
import com.google.android.material.snackbar.Snackbar

class SelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySelectionBinding
    private lateinit var adapter: MusicAdapter

    companion object {
        var selectedSongs = ArrayList<Music>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Reset selected songs when activity starts
        selectedSongs.clear()
        
        // Setup RecyclerView
        binding.selectionRV.setItemViewCacheSize(30)
        binding.selectionRV.setHasFixedSize(true)
        binding.selectionRV.layoutManager = LinearLayoutManager(this)
        adapter = MusicAdapter(this, MainActivity.MusicListMA, selectionActivity = true)
        binding.selectionRV.adapter = adapter
        
        // Set up buttons
        binding.backBtnSA.setOnClickListener { finish() }
        binding.selectAllBtn.setOnClickListener { 
            adapter.selectAll()
            updateSelectionCounter()
        }
        
        binding.clearSelectionsBtn.setOnClickListener {
            adapter.clearSelections()
            updateSelectionCounter()
        }
        
        binding.doneBtn.setOnClickListener {
            addSongsToPlaylist()
        }
        
        // Initialize counter
        updateSelectionCounter()
    }
    
    // Method to update the selection counter in UI
    fun updateSelectionCounter() {
        val count = selectedSongs.size
        binding.selectionCounter.text = "$count songs selected"
        
        // Enable/disable the done button based on selection status
        binding.doneBtn.isEnabled = count > 0
    }
    
    // Method to add all selected songs to playlist
    private fun addSongsToPlaylist() {
        if(selectedSongs.isEmpty()) {
            Toast.makeText(this, "Please select at least one song", Toast.LENGTH_SHORT).show()
            return
        }
        
        val currentPlaylistPos = PlaylistDetails.currentPlaylistPos
        
        // Check for duplicates and add only unique songs
        val currentPlaylist = PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist
        val addedSongs = ArrayList<Music>()
        val duplicates = ArrayList<Music>()
        
        for(song in selectedSongs) {
            if(!currentPlaylist.any { it.id == song.id }) {
                PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist.add(song)
                addedSongs.add(song)
            } else {
                duplicates.add(song)
            }
        }
        
        // Show result message
        if(addedSongs.size > 0) {
            val message = "${addedSongs.size} songs added to playlist"
            if(duplicates.size > 0) {
                message.plus(" (${duplicates.size} duplicates skipped)")
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "All selected songs already exist in playlist", Toast.LENGTH_SHORT).show()
        }
        
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Update UI if needed
    }
}
