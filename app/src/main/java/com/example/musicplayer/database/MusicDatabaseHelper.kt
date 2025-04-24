package com.example.musicplayer.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.musicplayer.Music

class MusicDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "MusicPlayer.db"
        private const val DATABASE_VERSION = 2 // bumped version
        private const val TAG = "MusicDatabaseHelper"

        private const val TABLE_MUSIC = "Music"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_ALBUM = "album"
        private const val COLUMN_ARTIST = "artist"
        private const val COLUMN_PATH = "path"
        private const val COLUMN_DURATION = "duration"
        private const val COLUMN_ART_URI = "artUri"
        private const val COLUMN_FAVORITE = "favorite" // new column for favorite flag
    }

    override fun onCreate(db: SQLiteDatabase?) {
        try {
            val createTableQuery = """
                CREATE TABLE $TABLE_MUSIC (
                    $COLUMN_ID TEXT PRIMARY KEY,
                    $COLUMN_TITLE TEXT,
                    $COLUMN_ALBUM TEXT,
                    $COLUMN_ARTIST TEXT,
                    $COLUMN_PATH TEXT,
                    $COLUMN_DURATION INTEGER,
                    $COLUMN_ART_URI TEXT,
                    $COLUMN_FAVORITE INTEGER DEFAULT 0
                )
            """
            db?.execSQL(createTableQuery)
            Log.d(TAG, "Database created successfully")
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error creating database", e)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        try {
            db?.execSQL("DROP TABLE IF EXISTS $TABLE_MUSIC")
            onCreate(db)
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error upgrading database", e)
        }
    }

    fun saveMusicList(musicList: ArrayList<Music>) {
        if (musicList.isEmpty()) return
        
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Clear existing data
            db.delete(TABLE_MUSIC, null, null)
            
            // Insert new data
            for (music in musicList) {
                val values = ContentValues().apply {
                    put(COLUMN_ID, music.id)
                    put(COLUMN_TITLE, music.title)
                    put(COLUMN_ALBUM, music.album)
                    put(COLUMN_ARTIST, music.artist)
                    put(COLUMN_PATH, music.path)
                    put(COLUMN_DURATION, music.duration)
                    put(COLUMN_ART_URI, music.artUri)
                    put(COLUMN_FAVORITE, if (music.isFavorite) 1 else 0) // save favorite flag
                }
                db.insertWithOnConflict(TABLE_MUSIC, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
            Log.d(TAG, "Saved ${musicList.size} songs to database")
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error saving music list", e)
        } finally {
            db.endTransaction()
        }
    }

    fun getAllMusic(): ArrayList<Music> {
        val musicList = ArrayList<Music>()
        val db = readableDatabase
        
        try {
            db.query(TABLE_MUSIC, null, null, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndexOrThrow(COLUMN_ID)
                    val titleIdx = cursor.getColumnIndexOrThrow(COLUMN_TITLE)
                    val albumIdx = cursor.getColumnIndexOrThrow(COLUMN_ALBUM)
                    val artistIdx = cursor.getColumnIndexOrThrow(COLUMN_ARTIST)
                    val pathIdx = cursor.getColumnIndexOrThrow(COLUMN_PATH)
                    val durationIdx = cursor.getColumnIndexOrThrow(COLUMN_DURATION)
                    val artUriIdx = cursor.getColumnIndexOrThrow(COLUMN_ART_URI)
                    val favoriteIdx = cursor.getColumnIndexOrThrow(COLUMN_FAVORITE)
                    
                    do {
                        try {
                            val music = Music(
                                id = cursor.getString(idIdx),
                                title = cursor.getString(titleIdx),
                                album = cursor.getString(albumIdx),
                                artist = cursor.getString(artistIdx),
                                path = cursor.getString(pathIdx),
                                duration = cursor.getLong(durationIdx),
                                artUri = cursor.getString(artUriIdx),
                                isFavorite = (cursor.getInt(favoriteIdx) == 1)  // load favorite flag
                            )
                            musicList.add(music)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error reading music data from cursor", e)
                        }
                    } while (cursor.moveToNext())
                }
            }
            Log.d(TAG, "Retrieved ${musicList.size} songs from database")
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error getting music list", e)
        }
        
        return musicList
    }

    fun updateFavoriteStatus(music: Music) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FAVORITE, if(music.isFavorite) 1 else 0)
        }
        // Update the row matching the song id
        db.update(TABLE_MUSIC, values, "$COLUMN_ID = ?", arrayOf(music.id))
    }
}
