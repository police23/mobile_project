package com.example.musicplayer

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.icu.number.Precision.increment
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.NumberPicker
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.database.MusicDatabaseHelper
import com.example.musicplayer.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity(), ServiceConnection, MediaPlayer.OnCompletionListener {

    companion object {
        var musicListPA = ArrayList<Music>()
        var songPosition = 0
        var mediaPlayer : MediaPlayer? = null
        var isPlaying : Boolean = false
        var musicService: MusicService? = null
        var nowPlayingId : String = ""
        var isFavourite: Boolean = false
        var fIndex : Int = -1
        @SuppressLint("StaticFieldLeak")
        lateinit var binding: ActivityPlayerBinding
        var repeatMode: Int = 0 // 0: off, 1: repeat all, 2: repeat one
    }

    private var timerRunnable: Runnable? = null
    private var currentTimerOption = 0 // 0: Off, 1-8: predefined times, 9: custom

    private val handler = Handler()
    private lateinit var dbHelper: MusicDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MusicPlayer)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = MusicDatabaseHelper(this)

        initializeLayout()

        binding.backBtnPA.setOnClickListener { finish() }
        binding.favouriteBtnPA.setOnClickListener {
            fIndex = favouriteChecker(musicListPA[songPosition].id)
            if(isFavourite){
                isFavourite = false
                binding.favouriteBtnPA.setImageResource(R.drawable.favorite_emp_icon)
                FavouriteActivity.favouriteSongs.removeAt(fIndex)
            } else{
                isFavourite = true
                binding.favouriteBtnPA.setImageResource(R.drawable.favorite_icon)
                FavouriteActivity.favouriteSongs.add(musicListPA[songPosition])
            }
        }

        binding.playPauseBtnPA.setOnClickListener {
            if (isPlaying) pauseMusic() else playMusic()
        }

        binding.previousBtnPA.setOnClickListener {
            prevNextSong(increment = false)
        }
        binding.nextBtnPA.setOnClickListener {
            prevNextSong(increment = true)
        }
        binding.repeatBtnPA.setOnClickListener {
            when(repeatMode) {
                0 -> {
                    repeatMode = 1
                    binding.repeatBtnPA.setImageResource(R.drawable.repeat_icon)
                    binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_blue))
                    Toast.makeText(this, "Repeat Playlist", Toast.LENGTH_SHORT).show()
                }
                1 -> {
                    repeatMode = 2
                    binding.repeatBtnPA.setImageResource(R.drawable.repeat_one_icon)
                    binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_blue))
                    Toast.makeText(this, "Repeat Song", Toast.LENGTH_SHORT).show()
                }
                2 -> {
                    repeatMode = 0
                    binding.repeatBtnPA.setImageResource(R.drawable.repeat_off_icon)
                    binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_pink))
                    Toast.makeText(this, "Repeat Off", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.timerBtnPA.setOnClickListener {
            showTimerOptions()
        }

        binding.seekBarPA.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) musicService!!.mediaPlayer!!.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun showTimerOptions() {
        val times = arrayOf("Off", "5", "10", "15", "30", "45", "60", "90", "120", "Custom")
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Timer (minutes)")
            .setSingleChoiceItems(times, currentTimerOption) { dialog, which ->
                if (which == 0) {
                    // Off option selected - cancel timer
                    timerRunnable?.let { 
                        handler.removeCallbacks(it)
                        timerRunnable = null
                        Toast.makeText(this, "Timer turned off", Toast.LENGTH_SHORT).show()
                    }
                    currentTimerOption = 0
                    dialog.dismiss()
                } else if (which == times.size - 1) {
                    // Custom option selected - show time picker dialog
                    dialog.dismiss()
                    showCustomTimerDialog()
                } else {
                    val minutes = times[which].toInt()
                    scheduleTimer(minutes)
                    Toast.makeText(this, "Timer set for $minutes minutes", Toast.LENGTH_SHORT).show()
                    currentTimerOption = which
                    dialog.dismiss()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCustomTimerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.custom_timer_dialog, null)
        val hourPicker = dialogView.findViewById<NumberPicker>(R.id.hourPicker)
        val minutePicker = dialogView.findViewById<NumberPicker>(R.id.minutePicker)
        
        // Configure hour picker
        hourPicker.minValue = 0
        hourPicker.maxValue = 23
        
        // Configure minute picker
        minutePicker.minValue = 0
        minutePicker.maxValue = 59
        
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Set Custom Timer")
            .setView(dialogView)
            .setPositiveButton("Set") { _, _ ->
                val hours = hourPicker.value
                val minutes = minutePicker.value
                
                if (hours == 0 && minutes == 0) {
                    Toast.makeText(this, "Timer cannot be 0 minutes", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val totalMinutes = (hours * 60) + minutes
                scheduleTimer(totalMinutes)
                
                val timeMessage = buildString {
                    if (hours > 0) {
                        append("$hours hour")
                        if (hours > 1) append("s")
                        if (minutes > 0) append(" and ")
                    }
                    if (minutes > 0 || hours == 0) {
                        append("$minutes minute")
                        if (minutes > 1) append("s")
                    }
                }
                Toast.makeText(this, "Timer set for $timeMessage", Toast.LENGTH_SHORT).show()
                currentTimerOption = 9  // Custom option
            }
            .setNegativeButton("Cancel", null)
            .create()
            
        dialog.show()
    }

    private fun scheduleTimer(minutes: Int) {
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = Runnable {
            pauseMusic()
            Toast.makeText(this, "Timer ended. Music stopped.", Toast.LENGTH_SHORT).show()
            currentTimerOption = 0 // Reset to Off when timer completes
        }
        handler.postDelayed(timerRunnable!!, minutes * 60 * 1000L)
    }

    private fun setLayout() {
        fIndex = favouriteChecker(musicListPA[songPosition].id)
        Glide.with(this)
            .load(musicListPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_player_icon_screen).centerCrop())
            .into(binding.songImgPA)
        binding.songNamePA.text = musicListPA[songPosition].title

        when(repeatMode) {
            0 -> binding.repeatBtnPA.setImageResource(R.drawable.repeat_off_icon)
            1 -> {
                binding.repeatBtnPA.setImageResource(R.drawable.repeat_icon)
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_blue))
            }
            2 -> {
                binding.repeatBtnPA.setImageResource(R.drawable.repeat_one_icon)
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_blue))
            }
        }

        if (isFavourite) {
            binding.favouriteBtnPA.setImageResource(R.drawable.favorite_icon)
        } else {
            binding.favouriteBtnPA.setImageResource(R.drawable.favorite_emp_icon)
        }
    }

    private fun createMediaPlayer() {
        try {
            if (musicService!!.mediaPlayer == null) musicService!!.mediaPlayer = MediaPlayer()
            musicService!!.mediaPlayer!!.reset()
            musicService!!.mediaPlayer!!.setDataSource(musicListPA[songPosition].path)
            musicService!!.mediaPlayer!!.prepare()
            musicService!!.mediaPlayer!!.start()

            musicService!!.mediaPlayer!!.isLooping = (repeatMode == 2)

            isPlaying = true
            binding.playPauseBtnPA.setIcon(ContextCompat.getDrawable(this, R.drawable.pause_icon))
            musicService!!.showNotification(R.drawable.pause_icon)
            binding.tvSeekBarStart.text = formatSongDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
            binding.tvSeekBarEnd.text = formatSongDuration(musicService!!.mediaPlayer!!.duration.toLong())
            binding.seekBarPA.progress = 0
            binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
            musicService!!.mediaPlayer!!.setOnCompletionListener(this)
            nowPlayingId = musicListPA[songPosition].id
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
        val intentClass = intent.getStringExtra("class")
        when (intentClass) {
            "PlaylistDetailsAdapter"->{
                val intent = Intent(this, MusicService::class.java)
                bindService(intent,this,BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList(PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist)
                setLayout()
            }

            "FavouriteAdapter"-> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent,this,BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList(FavouriteActivity.favouriteSongs)
                setLayout()
            }
                "NowPlaying" -> {
                setLayout()
                binding.tvSeekBarStart.text = formatSongDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
                binding.tvSeekBarEnd.text = formatSongDuration(musicService!!.mediaPlayer!!.duration.toLong())
                binding.seekBarPA.progress = musicService!!.mediaPlayer!!.currentPosition
                binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
                if (isPlaying) {
                    binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
                } else {
                    binding.playPauseBtnPA.setIconResource(R.drawable.play_icon)
                }
            }
            "MusicAdapter" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent,this,BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList(MainActivity.MusicListMA)
                setLayout()
                createMediaPlayer()
            }
            "MainActivity" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent,this,BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList(MainActivity.MusicListMA)
                musicListPA.shuffle()
                setLayout()
                createMediaPlayer()
            }
            "PlaylistDetailsShuffle"->{
                val intent = Intent(this, MusicService::class.java)
                bindService(intent,this,BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList(PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist)
                musicListPA.shuffle()
                setLayout()
            }
        }
    }

    private fun playMusic() {
        binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
        musicService!!.showNotification(R.drawable.pause_icon)
        isPlaying = true
        musicService!!.mediaPlayer!!.start()
    }

    private fun pauseMusic() {
        binding.playPauseBtnPA.setIconResource(R.drawable.play_icon)
        musicService!!.showNotification(R.drawable.play_icon)
        isPlaying = false
        musicService!!.mediaPlayer!!.pause()
    }

    private fun prevNextSong(increment: Boolean) {
        if (increment) {
            setSongPosition(increment = true)
            setLayout()
            createMediaPlayer()
        } else {
            setSongPosition(increment = false)
            setLayout()
            createMediaPlayer()
        }

    }

    private fun updateFavoriteIcon() {
        val currentMusic = musicListPA[songPosition]
        if (currentMusic.isFavorite)
            binding.favouriteBtnPA.setImageResource(R.drawable.favorite_filled_icon)
        else
            binding.favouriteBtnPA.setImageResource(R.drawable.favorite_emp_icon)
    }

     override fun onServiceConnected(name : ComponentName?, service: IBinder?) {
        val binder = service as MusicService.MyBinder
        musicService = binder.currentService()
        createMediaPlayer()
        musicService!!.seekBarSetup()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicService = null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onCompletion(mp: MediaPlayer?) {
        if(repeatMode == 2) {
            mp?.seekTo(0)
            mp?.start()
            return
        } else if(repeatMode == 1) {
            setSongPosition(increment = true)
            createMediaPlayer()
            setLayout()
        } else {
            if(songPosition < musicListPA.size - 1) {
                setSongPosition(increment = true)
                createMediaPlayer()
                setLayout()
            } else {
                pauseMusic()
                songPosition = 0
                setLayout()
                binding.seekBarPA.progress = 0
                binding.tvSeekBarStart.text = "0:00"
            }
        }
    }
}
