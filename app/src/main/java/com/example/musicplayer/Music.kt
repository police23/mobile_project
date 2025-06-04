package com.example.musicplayer
import com.example.musicplayer.PlayerActivity.Companion.musicListPA
import com.example.musicplayer.PlayerActivity.Companion.songPosition
import java.io.File
import java.util.concurrent.TimeUnit
data class Music(
    val id: String = "",
    val title: String = "",
    val album: String = "",
    val artist: String = "",
    val path: String = "",
    val duration: Long = 0,
    val artUri: String = "",
    var isFavorite: Boolean = false // added property for favorites
)

class Playlist {
    lateinit var name: String
    lateinit var playlist: ArrayList<Music>
    lateinit var createdOn: String
}
class MusicPlaylist {
    var ref: ArrayList<Playlist> = ArrayList()
}
fun formatSongDuration(duration: Long): String {
    val minutes = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS)
    val seconds = (TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS) - minutes*TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES))
    return String.format("%02d:%02d", minutes, seconds)
}

fun setSongPosition(increment: Boolean) {
    // For repeat one (mode 2), position change is handled in onCompletion
    // This handles normal playback (mode 0) and repeat all (mode 1)
    if (increment) {
        if (musicListPA.size - 1 == songPosition) {
            songPosition = 0
        } else {
            ++songPosition
        }
    } else {
        if (songPosition == 0) {
            songPosition = musicListPA.size - 1
        } else {
            --songPosition
        }
    }
}

fun favouriteChecker(id: String): Int {
    PlayerActivity.isFavourite = false
    FavouriteActivity.favouriteSongs.forEachIndexed { index, music ->
        if (id == music.id) {
            PlayerActivity.isFavourite = true
            return index
        }
    }
    return -1
}

fun checkPlaylist(playlist: ArrayList<Music>): ArrayList<Music> {
    val indicesToRemove = mutableListOf<Int>()

    playlist.forEachIndexed { index, music ->
        if (!File(music.path).exists()) indicesToRemove.add(index)
    }

    indicesToRemove.sortDescending()
    indicesToRemove.forEach { index -> playlist.removeAt(index) }
    return playlist
}
