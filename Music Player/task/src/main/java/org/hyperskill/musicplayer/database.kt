package org.hyperskill.musicplayer

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteStatement
import org.intellij.lang.annotations.Language
import java.io.Closeable
import java.util.concurrent.atomic.AtomicReference



class DbHelper(
    context: Context,
) : SQLiteOpenHelper(context, "musicPlayerDatabase.db", null, 1) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE playlist(
            |    _id INTEGER PRIMARY KEY
            |,   playlistName TEXT NOT NULL
            |,   songId INTEGER NOT NULL
            |)""".trimMargin()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw UnsupportedOperationException() // we have single schema version, no migrations yet
    }
}

class PlaylistStore(val db: SQLiteDatabase) : Closeable {

    fun allPlaylists(allSongs: List<Song>): List<PlayList> {
        val cursor = db.rawQuery("SELECT DISTINCT playlistName FROM playlist", null)
        val result = mutableListOf<PlayList>()

        try {
            while (cursor.moveToNext()) {
                val x = cursor.columnCount
                val y = cursor.columnNames

                val name = cursor.getString(0)
                result.add(playListByName(name, allSongs))
            }
        } finally {
            cursor.close()
        }
        return result
        // TODO 1) выбрать все уникальные имена плейлистов, 2) используя ф-цию ниже выбрать все плейлисты по именам
    }

    fun playListByName(name: String, allSongs: List<Song>): PlayList {
        val cursor = db.rawQuery("SELECT * FROM playlist WHERE playlistName = ?", arrayOf(name))
        val list = mutableListOf<Song>()
        try {
            while (cursor.moveToNext()) {
                val id = cursor.getLong(1)
                val song = allSongs.find { it.id == id }
                if (song != null) list.add(song)
            }
        } finally {
            cursor.close()
        }
        return PlayList(name, list)
    }

    private val insertRef = AtomicReference<SQLiteStatement>()

    //@RequiresApi(Build.VERSION_CODES.O)
    fun insert(playlistName: String, songId: Long) {
        withStatement(insertRef, "INSERT INTO playlist (playlistName, songId) VALUES (?, ?)") {
            bindString(1, playlistName)
            bindLong(2, songId)
            executeInsert()
        }
    }

    private val deleteRef = AtomicReference<SQLiteStatement>()
    fun deletePlaylist(playlistName: String) {
        withStatement(deleteRef, "DELETE FROM playlist WHERE playlistName=?") {
            bindString(1, playlistName)
            executeInsert()
        }
    }

    override fun close() {
        insertRef.getAndSet(null)?.close()
        deleteRef.getAndSet(null)?.close()
        /*updateRef.getAndSet(null)?.close()
        deleteRef.getAndSet(null)?.close()*/
    }

    private inline fun <R> withStatement(
        ref: AtomicReference<SQLiteStatement>,
        @Language("SQL") query: String,
        block: SQLiteStatement.() -> R,
    ): R {
        val statement = ref.getAndSet(null) ?: db.compileStatement(query)
        try {
            return statement.block()
        } finally {
            ref.getAndSet(statement)?.close()
        }
    }
}

   /* private companion object {
        private val idCol = arrayOf("_id")
        private val songCols = arrayOf("playlistName", "songId")
    }

    fun all(): List<Song> {
        val cursor = db.query("playlist", idCol, null, null, null, null, null)
        val ids = try {
            LongArray(cursor.count) { _ ->
                check(cursor.moveToNext())
                cursor.getLong(0)
            }
        } finally {
            cursor.close()
        }
        return object : AbstractList<Song>() { // TODO

            override val size: Int
                get() = ids.size

            private val memo = arrayOfNulls<Song>(size)
            @RequiresApi(Build.VERSION_CODES.O)
            override fun get(index: Int): Song =
                memo[index] ?: singleOrNull(ids[index])!!.also { memo[index] = it }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun singleOrNull(id: Long): Song? {
        val cursor = db.query("songs", songCols, "_id = ?", arrayOf(id.toString()), null, null, null)
        return try {
            if (cursor.moveToFirst()) Person(cursor)
            else null // not found
        } finally {
            cursor.close()
        }
    }
    @RequiresApi(Build.VERSION_CODES.O) // TODO
    private fun Person(cur: Cursor) = Song(
        cur.getLong(0),
        cur.getString(1),
        cur.getString(2),
        cur.getLong(3)
    )

    private val insertRef = AtomicReference<SQLiteStatement>()
    @RequiresApi(Build.VERSION_CODES.O)
    fun insert(title: String, artist: String, duration: Long): Long =
        withStatement(insertRef, "INSERT INTO songs (title, artist, duration) VALUES (?, ?, ?)") {
            bindString(1, title)
            bindString(2, artist)
            bindLong(3, duration)
            executeInsert()
        }

    private val updateRef = AtomicReference<SQLiteStatement>()
    @RequiresApi(Build.VERSION_CODES.O)
    fun update(song: Song): Unit =
        withStatement(updateRef, "UPDATE songs SET title = ?, artist = ?, duration = ? WHERE _id = ?") {
            bindString(1, song.title)
            bindString(2, song.artist)
            bindLong(3, song.duration)
            bindLong(4, song.id)
            check(executeUpdateDelete() == 1)
        }

    private val deleteRef = AtomicReference<SQLiteStatement>()
    fun delete(song: Song): Unit =
        withStatement(deleteRef, "DELETE FROM songs WHERE _id = ?") {
            bindLong(1, song.id)
            check(executeUpdateDelete() == 1)
        }

    override fun close() {
        insertRef.getAndSet(null)?.close()
        updateRef.getAndSet(null)?.close()
        deleteRef.getAndSet(null)?.close()
    }

    *//**
     * Poll for an existing prepared statement or create a new one.
     * This ensures thread-safe statement reuse:
     * any thread can take or prepare a statement,
     * but two threads couldn't use one statement at the same time.
     *
     * The [ref] must be used with the same [query].
     * The statement provided to the [block] must never leak outside.
     *//*
    private inline fun <R> withStatement(
        ref: AtomicReference<SQLiteStatement>,
        @Language("SQL") query: String,
        block: SQLiteStatement.() -> R,
    ): R {
        val statement = ref.getAndSet(null) ?: db.compileStatement(query)
        try {
            return statement.block()
        } finally {
            ref.getAndSet(statement)?.close()
        }
    }*/
