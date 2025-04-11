package com.example.musicplayer

import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity() {

    companion object {
        var musicListPA = ArrayList<Music>()
        var songPosition = 0
        var mediaPlayer : MediaPlayer? = null
        var isPlaying : Boolean = false
    }

    private lateinit var binding: ActivityPlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MusicPlayer)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeLayout()
        binding.playPauseBtnPA.setOnClickListener{
            if (isPlaying) pauseMusic()
            else playMusic()
        }

    }
    private fun setLayout() {
        Glide.with(this)
            .load(musicListPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_player_icon_screen).centerCrop())
            .into(binding.songImgPA)
        binding.songNamePA.text = musicListPA[songPosition].title

    }
    private fun createMediaPlayer() {

        try {
            if (mediaPlayer == null) mediaPlayer = MediaPlayer()
            mediaPlayer!!.reset()
            mediaPlayer!!.setDataSource(musicListPA[songPosition].path)
            mediaPlayer!!.prepare()
            mediaPlayer!!.start()
            isPlaying = true
            binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)

        }
        catch (e: Exception) {
            return
        }
    }
    private fun initializeLayout() {
        songPosition = intent.getIntExtra("index", 0)
        when (intent.getStringExtra("class")) {
            "MusicAdapter" -> {
                musicListPA = ArrayList(MainActivity.MusicListMA)
                setLayout()
                createMediaPlayer()
            }
        }
    }
    private fun playMusic() {
        binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
        isPlaying = true
        mediaPlayer!!.start()
    }
    private fun pauseMusic() {
        binding.playPauseBtnPA.setIconResource(R.drawable.play_icon)
        isPlaying = false
        mediaPlayer!!.pause()
    }





}