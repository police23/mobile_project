package com.example.musicplayer

import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.NotificationCompat

class MusicService : Service() {
    private var myBinder = MyBinder()
    var mediaPlayer: MediaPlayer? = null
    private lateinit var mediaSession: MediaSessionCompat

    override fun onBind(intent: Intent?): IBinder? {
        Log.d("MusicService", "Service onBind")
        mediaSession = MediaSessionCompat(baseContext, "My Music")
        return myBinder
    }

    inner class MyBinder:Binder() {
        fun currentService(): MusicService {
            return this@MusicService
        }
    }

    fun showNotification() {
        Log.d("Noti", "Chuẩn bị noti")
        Log.d("title", "${PlayerActivity.musicListPA[PlayerActivity.songPosition].title}")
        Log.d("artist", "${PlayerActivity.musicListPA[PlayerActivity.songPosition].artist}")
        Log.d("CHANNEL_ID", "${ApplicationClass.CHANNEL_ID}")
       val notification = NotificationCompat.Builder(baseContext, ApplicationClass.CHANNEL_ID)
            .setContentTitle(PlayerActivity.musicListPA[PlayerActivity.songPosition].title)
            .setContentText(PlayerActivity.musicListPA[PlayerActivity.songPosition].artist)
            .setSmallIcon(R.drawable.playlist_icon)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.music_player_icon_screen))
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken))
           .setPriority(NotificationCompat.PRIORITY_HIGH)
           .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

           .addAction(R.drawable.previous_icon,"Previous", null)
           .addAction(R.drawable.play_icon,"Play", null)
           .addAction(R.drawable.next_icon,"Next", null)
           .addAction(R.drawable.exit_icon,"Exit", null)
            .build()

        startForeground(13, notification)
        Log.d("Noti", "noti xong")
    }
}