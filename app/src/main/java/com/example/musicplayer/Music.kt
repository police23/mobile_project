package com.example.musicplayer

data class Music(
    val id: String,
    val title: String,
    val album: String,
    val artist: String,
    val path: String,
    val duration: Long = 0,
    val artUri: String
) {
    // Default constructor needed for some Android components
    constructor() : this("", "", "", "", "", 0, "")
}
