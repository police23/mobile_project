package com.example.musicplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MusicDownloadAdapter(private var tracks: List<Map<String, String>> ) :
    RecyclerView.Adapter<MusicDownloadAdapter.MusicViewHolder>() {

    class MusicViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.imageMD)
        val name: TextView = view.findViewById(R.id.musicName)
        val artist: TextView = view.findViewById(R.id.artistName)
        val downloadBtn: Button = view.findViewById(R.id.download)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.music_download, parent, false)
        return MusicViewHolder(view)
    }

    override fun getItemCount(): Int = tracks.size

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val track = tracks[position]
        holder.name.text = track["name"]
        holder.artist.text = track["artistName"]
        Glide.with(holder.itemView).load(track["image"]).into(holder.image)

        holder.downloadBtn.setOnClickListener {
            val context = holder.itemView.context
            val songName = "${track["name"]} ${track["artistName"]}"
            val encodedSongName = java.net.URLEncoder.encode(songName, "UTF-8")
            val apiUrl = "http://10.0.141.143:5000/download?song_name=$encodedSongName"


            // Gửi request trên luồng nền
            Thread {
                try {
                    val connection = java.net.URL(apiUrl).openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"

                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }

                    val json = org.json.JSONObject(response)
                    val downloadUrl = "http://10.0.141.143:5000" + json.getString("download_url")
                    val videoTitle = json.getString("video_title")

                    // Dùng DownloadManager để tải file .mp3
                    val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                    val uri = downloadUrl.toUri()

                    val request = android.app.DownloadManager.Request(uri).apply {
                        setTitle("Tải xuống: $videoTitle")
                        setDescription("Đang tải file mp3")
                        setDestinationInExternalPublicDir(
                            android.os.Environment.DIRECTORY_DOWNLOADS,
                            "$videoTitle.mp3"
                        )
                        setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setAllowedNetworkTypes(android.app.DownloadManager.Request.NETWORK_WIFI or android.app.DownloadManager.Request.NETWORK_MOBILE)
                    }

                    downloadManager.enqueue(request)
                    MainActivity.downloaded = true
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        android.widget.Toast.makeText(context, "Lỗi tải file", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
    }
    fun updateData(newTracks: List<Map<String, String>>) {
        tracks = newTracks
        notifyDataSetChanged()
    }
}