package com.example.musicplayer

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    private var repeatMode = 0  // 0: off, 1: repeat all, 2: repeat one
    private var timerRunnable: Runnable? = null
    private lateinit var binding: ActivityPlayerBinding
    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MusicPlayer)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeLayout()

        binding.playPauseBtnPA.setOnClickListener {
            if (isPlaying) pauseMusic() else playMusic()
        }

        // Updated repeat button: cycles through off -> repeat all -> repeat one -> off
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
                    binding.repeatBtnPA.setImageResource(R.drawable.repeat_off_icon) // original icon for off mode
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
            
            // Set completion listener based on repeat mode
            if (repeatMode == 1) {
                mediaPlayer!!.setOnCompletionListener {
                    songPosition = (songPosition + 1) % musicListPA.size
                    setLayout()
                    createMediaPlayer()
                }
            } else {
                mediaPlayer!!.setOnCompletionListener(null)
            }
            
            // For repeat one mode, looping is enabled; otherwise, ensure it is off.
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

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacksAndMessages(null)
    }
}
