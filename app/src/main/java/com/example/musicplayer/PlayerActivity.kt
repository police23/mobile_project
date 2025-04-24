package com.example.musicplayer

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.database.MusicDatabaseHelper
import com.example.musicplayer.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity() {

    companion object {
        var musicListPA = ArrayList<Music>()
        var songPosition = 0
        var mediaPlayer : MediaPlayer? = null
        var isPlaying : Boolean = false

        fun togglePlayback() {
            mediaPlayer?.let {
                if (isPlaying) {
                    it.pause()
                    isPlaying = false
                } else {
                    it.start()
                    isPlaying = true
                }
            }
        }
        fun nextSongControl() {
            if (musicListPA.isNotEmpty()) {
                songPosition = (songPosition + 1) % musicListPA.size
                mediaPlayer?.reset()
                try {
                    mediaPlayer?.setDataSource(musicListPA[songPosition].path)
                    mediaPlayer?.prepare()
                    mediaPlayer?.start()
                    isPlaying = true
                } catch (e: Exception) { }
            }
        }
        fun prevSongControl() {
            if (musicListPA.isNotEmpty()) {
                songPosition = if (songPosition == 0) musicListPA.size - 1 else songPosition - 1
                mediaPlayer?.reset()
                try {
                    mediaPlayer?.setDataSource(musicListPA[songPosition].path)
                    mediaPlayer?.prepare()
                    mediaPlayer?.start()
                    isPlaying = true
                } catch (e: Exception) { }
            }
        }
    }

    private var repeatMode = 0  // 0: off, 1: repeat all, 2: repeat one
    private var timerRunnable: Runnable? = null
    private lateinit var binding: ActivityPlayerBinding
    private val handler = Handler()
    private lateinit var dbHelper: MusicDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MusicPlayer)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dbHelper = MusicDatabaseHelper(this)

        // Added back button click listener
        binding.backBtnPA.setOnClickListener { finish() }

        initializeLayout()
        updateFavoriteIcon()

        binding.favouriteBtnPA.setOnClickListener {
            val currentMusic = musicListPA[songPosition]
            currentMusic.isFavorite = !currentMusic.isFavorite
            updateFavoriteIcon()
            dbHelper.updateFavoriteStatus(currentMusic)
            Toast.makeText(this,
                if (currentMusic.isFavorite) "Added to favorites" else "Removed from favorites",
                Toast.LENGTH_SHORT).show()
        }

        binding.playPauseBtnPA.setOnClickListener {
            if (isPlaying) pauseMusic() else playMusic()
        }

        binding.repeatBtnPA.setOnClickListener {
            when (repeatMode) {
                0 -> {
                    repeatMode = 1
                    mediaPlayer?.isLooping = false
                    binding.repeatBtnPA.setImageResource(R.drawable.repeat_playlist_icon)
                    Toast.makeText(this, "Repeat All", Toast.LENGTH_SHORT).show()
                }
                1 -> {
                    repeatMode = 2
                    mediaPlayer?.isLooping = true
                    binding.repeatBtnPA.setImageResource(R.drawable.repeat_one_icon)
                    Toast.makeText(this, "Repeat One", Toast.LENGTH_SHORT).show()
                }
                2 -> {
                    repeatMode = 0
                    mediaPlayer?.isLooping = false
                    binding.repeatBtnPA.setImageResource(R.drawable.repeat_off_icon)
                    Toast.makeText(this, "Repeat Off", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.timerBtnPA.setOnClickListener {
            val times = arrayOf("5", "10", "15", "30", "45", "60", "90", "120")
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Timer (minutes)")
                .setItems(times) { dialog, which ->
                    val minutes = times[which].toInt()
                    scheduleTimer(minutes)
                    Toast.makeText(this, "Timer set for $minutes minutes", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        binding.seekBarPA.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && mediaPlayer != null) mediaPlayer!!.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) { }
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) { }
        })
    }

    private fun scheduleTimer(minutes: Int) {
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = Runnable {
            pauseMusic()
            Toast.makeText(this, "Timer ended. Music stopped.", Toast.LENGTH_SHORT).show()
        }
        handler.postDelayed(timerRunnable!!, minutes * 60 * 1000L)
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
            binding.tvSeekBarStart.text = formatDuration(mediaPlayer!!.currentPosition)
            binding.tvSeekBarEnd.text = formatDuration(mediaPlayer!!.duration)

            if (repeatMode == 1) {
                mediaPlayer!!.setOnCompletionListener {
                    songPosition = (songPosition + 1) % musicListPA.size
                    setLayout()
                    createMediaPlayer()
                }
            } else {
                mediaPlayer!!.setOnCompletionListener(null)
            }

            if (repeatMode != 2) mediaPlayer!!.isLooping = false

            mediaPlayer!!.start()
            isPlaying = true
            binding.playPauseBtnPA.setIcon(ContextCompat.getDrawable(this, R.drawable.pause_icon))
            binding.seekBarPA.max = mediaPlayer!!.duration
            updateSeekBar()
        } catch (e: Exception) {
            return
        }
    }

    private fun formatDuration(duration: Int): String {
        val minutes = duration / 1000 / 60
        val seconds = duration / 1000 % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun updateSeekBar() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (mediaPlayer != null) {
                    val currentPosition = mediaPlayer!!.currentPosition
                    binding.seekBarPA.progress = currentPosition
                    binding.tvSeekBarStart.text = formatDuration(currentPosition)
                    handler.postDelayed(this, 1000)
                }
            }
        }, 0)
    }

    private fun initializeLayout() {
        val newIndex = intent.getIntExtra("index", 0)
        val intentClass = intent.getStringExtra("class")
        when (intentClass) {
            "MusicAdapter" -> {
                musicListPA = ArrayList(MainActivity.MusicListMA)
                if (mediaPlayer != null && songPosition == newIndex && isPlaying) {
                    setLayout() // update UI without resetting playback
                } else {
                    songPosition = newIndex
                    setLayout()
                    createMediaPlayer() // start new song or reinitialize
                }
            }
            "MiniPlayer" -> {
                setLayout()
                mediaPlayer?.let {
                    binding.tvSeekBarStart.text = formatDuration(it.currentPosition)
                    binding.tvSeekBarEnd.text = formatDuration(it.duration)
                    binding.seekBarPA.max = it.duration
                    updateSeekBar()
                }
            }
            else -> {
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

    private fun updateFavoriteIcon() {
        val currentMusic = musicListPA[songPosition]
        if (currentMusic.isFavorite)
            binding.favouriteBtnPA.setImageResource(R.drawable.favorite_filled_icon)
        else
            binding.favouriteBtnPA.setImageResource(R.drawable.favorite_emp_icon)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
