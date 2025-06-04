package com.example.musicplayer

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.databinding.MusicViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MusicAdapter(
    private val context: Context,
    private var musicList: ArrayList<Music>,
    private val playlistDetails: Boolean = false,
    private val selectionActivity: Boolean = false
) : RecyclerView.Adapter<MusicAdapter.MyHolder>() {

    class MyHolder(binding: MusicViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.songNameMV
        val album = binding.songAlbumMV
        val image = binding.imageMV
        val duration = binding.songDuration
        val root = binding.root
        val checkbox = binding.checkboxMV  // Add reference to the checkbox
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(MusicViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.title.text = musicList[position].title
        holder.album.text = musicList[position].album
        holder.duration.text = formatSongDuration(musicList[position].duration)
        Glide.with(context)
            .load(musicList[position].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_player_icon_splash_screen).centerCrop())
            .into(holder.image)
        
        // Show checkbox only in selection mode
        if(selectionActivity) {
            holder.checkbox.visibility = View.VISIBLE
            // Set checkbox state based on selection list
            holder.checkbox.isChecked = SelectionActivity.selectedSongs.contains(musicList[position])
        } else {
            holder.checkbox.visibility = View.GONE
        }
        
        // Click event for playing the song
        holder.root.setOnClickListener {
            when {
                selectionActivity -> {
                    // Toggle selection instead of immediate add
                    toggleSelection(position)
                    notifyItemChanged(position)
                    // Update the selection counter in SelectionActivity
                    (context as SelectionActivity).updateSelectionCounter()
                }
                playlistDetails -> {
                    val intent = Intent(context, PlayerActivity::class.java)
                    intent.putExtra("index", position)
                    intent.putExtra("class", "PlaylistDetails")
                    ContextCompat.startActivity(context, intent, null)
                }
                else -> {
                    val intent = Intent(context, PlayerActivity::class.java)
                    intent.putExtra("index", position)
                    intent.putExtra("class", "MusicAdapter")
                    ContextCompat.startActivity(context, intent, null)
                }
            }
        }

        // Long press event for deleting song from playlist
        holder.root.setOnLongClickListener {
            if (playlistDetails) {
                val builder = MaterialAlertDialogBuilder(context)
                builder.setTitle(musicList[position].title)
                    .setMessage("Do you want to remove this song from playlist?")
                    .setPositiveButton("Yes") { dialog, _ ->
                        val currentPlaylistPos = PlaylistDetails.currentPlaylistPos
                        PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist.removeAt(position)
                        refreshPlaylist()
                        dialog.dismiss()
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                val customDialog = builder.create()
                customDialog.show()
                return@setOnLongClickListener true
            }
            false
        }

        // Add checkbox click handler
        holder.checkbox.setOnClickListener {
            toggleSelection(position)
            (context as SelectionActivity).updateSelectionCounter()
        }
    }

    private fun toggleSelection(position: Int) {
        val song = musicList[position]
        if(SelectionActivity.selectedSongs.contains(song)) {
            SelectionActivity.selectedSongs.remove(song)
        } else {
            SelectionActivity.selectedSongs.add(song)
        }
    }

    override fun getItemCount(): Int {
        return musicList.size
    }

    fun refreshPlaylist() {
        musicList = ArrayList()
        musicList = PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist
        notifyDataSetChanged()
    }
    
    fun updateMusicList(searchList: ArrayList<Music>) {
        musicList = ArrayList()
        musicList.addAll(searchList)
        notifyDataSetChanged()
    }

    fun selectAll() {
        SelectionActivity.selectedSongs.clear()
        SelectionActivity.selectedSongs.addAll(musicList)
        notifyDataSetChanged()
    }

    fun clearSelections() {
        SelectionActivity.selectedSongs.clear()
        notifyDataSetChanged()
    }
}
