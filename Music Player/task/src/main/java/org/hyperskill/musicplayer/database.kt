package org.hyperskill.musicplayer

import android.content.Context
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

class PlaylistStore(private val db: SQLiteDatabase) : Closeable {

    fun allPlaylists(allSongs: List<Song>): List<PlayList> {
        val cursor = db.rawQuery("SELECT DISTINCT playlistName FROM playlist", null)
        val result = mutableListOf<PlayList>()

        cursor.use { cursor1 ->
            while (cursor1.moveToNext()) {
                val name = cursor1.getString(0)
                result.add(playListByName(name, allSongs))
            }
        }
        return result
    }

    private fun playListByName(name: String, allSongs: List<Song>): PlayList {
        val cursor = db.rawQuery("SELECT * FROM playlist WHERE playlistName = ?", arrayOf(name))
        val list = mutableListOf<Song>()
        cursor.use { cursor1 ->
            while (cursor1.moveToNext()) {
                val id = cursor1.getLong(1)
                val song = allSongs.find { it.id == id }
                if (song != null) list.add(song)
            }
        }
        return PlayList(name, list)
    }

    private val insertRef = AtomicReference<SQLiteStatement>()

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
    }

    /**
     * Poll for an existing prepared statement or create a new one.
     * This ensures thread-safe statement reuse:
     * any thread can take or prepare a statement,
     * but two threads couldn't use one statement at the same time.
     *
     * The [ref] must be used with the same [query].
     * The statement provided to the [block] must never leak outside.
     */

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
