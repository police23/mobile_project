package com.example.musicplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.PlayerActivity.Companion.binding
import com.example.musicplayer.PlayerActivity.Companion.musicListPA
import com.example.musicplayer.PlayerActivity.Companion.musicService
import com.example.musicplayer.PlayerActivity.Companion.songPosition
import kotlin.system.exitProcess
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class NotificationReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action) {
            ApplicationClass.PREVIOUS -> prevNextSong(increment = false, context = context!!)
            ApplicationClass.PLAY -> if (PlayerActivity.isPlaying) pauseMusic() else playMusic()
            ApplicationClass.NEXT -> prevNextSong(increment = true, context = context!!)
            ApplicationClass.EXIT -> {
                PlayerActivity.musicService!!.stopForeground(true)
                PlayerActivity.musicService = null
                exitProcess(1)
            }
        }
    }
    private fun playMusic() {
        PlayerActivity.isPlaying = true
        PlayerActivity.musicService!!.mediaPlayer!!.start()
        PlayerActivity.musicService!!.showNotification(R.drawable.pause_icon)
        
        // Set play/pause button icon in PlayerActivity
        updatePlayPauseButton(PlayerActivity.binding.playPauseBtnPA, R.drawable.pause_icon)
        
        // Set play/pause button icon in NowPlaying if it's initialized
        try {
            updatePlayPauseButton(NowPlaying.binding.playPauseBtnNP, R.drawable.pause_icon)
        } catch(e: Exception) {
            // Handle case when NowPlaying isn't initialized
        }
    }

    private fun pauseMusic() {
        PlayerActivity.isPlaying = false
        PlayerActivity.musicService!!.mediaPlayer!!.pause()
        PlayerActivity.musicService!!.showNotification(R.drawable.play_icon)
        
        // Set play/pause button icon in PlayerActivity
        updatePlayPauseButton(PlayerActivity.binding.playPauseBtnPA, R.drawable.play_icon)
        
        // Set play/pause button icon in NowPlaying if it's initialized
        try {
            updatePlayPauseButton(NowPlaying.binding.playPauseBtnNP, R.drawable.play_icon)
        } catch(e: Exception) {
            // Handle case when NowPlaying isn't initialized
        }
    }
    
    // Helper method that handles different button types
    private fun updatePlayPauseButton(button: Any, iconRes: Int) {
        when (button) {
            is ExtendedFloatingActionButton -> button.setIconResource(iconRes)
            is FloatingActionButton -> button.setImageResource(iconRes)
        }
    }

    private fun prevNextSong(increment: Boolean, context: Context) {
        setSongPosition(increment = increment)
        PlayerActivity.musicService!!.createMediaPlayer()
        Glide.with(context)
            .load(musicListPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_player_icon_screen).centerCrop())
            .into(binding.songImgPA)
        PlayerActivity.binding.songNamePA.text = PlayerActivity.musicListPA[songPosition].title
        
        try {
            Glide.with(context)
                .load(PlayerActivity.musicListPA[PlayerActivity.songPosition].artUri)
                .apply(RequestOptions().placeholder(R.drawable.music_player_icon_splash_screen).centerCrop())
                .into(NowPlaying.Companion.binding.songImgNP)
            NowPlaying.Companion.binding.songNameNP.text = PlayerActivity.musicListPA[PlayerActivity.songPosition].title
        } catch(e: Exception) {
            // Handle case when NowPlaying isn't initialized
        }
        
        playMusic()
        PlayerActivity.fIndex = favouriteChecker(PlayerActivity.musicListPA[PlayerActivity.songPosition].id)
        if(PlayerActivity.isFavourite) PlayerActivity.binding.favouriteBtnPA.setImageResource(R.drawable.favorite_icon)
        else PlayerActivity.binding.favouriteBtnPA.setImageResource(R.drawable.favorite_emp_icon)
    }
}
