package org.hyperskill.musicplayer

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteStatement
import android.os.Build
import androidx.annotation.RequiresApi
import org.intellij.lang.annotations.Language
import java.io.Closeable
import java.util.concurrent.atomic.AtomicReference



class DbHelper(
    context: Context,
) : SQLiteOpenHelper(context, "musicPlayerDatabase.db", null, 1) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys = 1")
        db.execSQL("PRAGMA trusted_schema = 0")
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE playlist(
            |    _id INTEGER PRIMARY KEY
            |,   playlistName TEXT NOT NULL
            |,   songId TEXT NOT NULL
            |)""".trimMargin()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw UnsupportedOperationException() // we have single schema version, no migrations yet
    }
}

class SongStore(val db: SQLiteDatabase) : Closeable {

    private companion object {
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