package com.example.musicplayer

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.databinding.ActivityDownloadBinding
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


class DownloadActivity : AppCompatActivity() {
    private lateinit var searchInput: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MusicDownloadAdapter
    private lateinit var binding : ActivityDownloadBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MusicPlayer)
        binding = ActivityDownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        searchInput = binding.searchEditText
        recyclerView = binding.trackRecyclerView
        binding.backBtnDL.setOnClickListener { finish() }
        adapter = MusicDownloadAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        val spapi = SpotifyAPI()

        lifecycleScope.launch {

            searchInput.textChanges()
                .debounce(500)
                .filter { it != null && it.isNotBlank() }
                .map { it.toString() }
                .distinctUntilChanged()
                .collect { query ->

                    val tracks = spapi.searchTrack(query)
                    adapter.updateData(tracks)
                }
        }
    }

    fun EditText.textChanges(): Flow<CharSequence?> = callbackFlow {
        val listener = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                trySend(s)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        addTextChangedListener(listener)
        awaitClose { removeTextChangedListener(listener) }
    }
}