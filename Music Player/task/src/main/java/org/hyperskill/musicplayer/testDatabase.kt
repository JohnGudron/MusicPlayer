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


data class Row(val id: Long, val name: String, val value: String)

class TestDbHelper(context: Context): SQLiteOpenHelper(context, "testDb.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE test_table(" +
                "_id INTEGER PRIMARY KEY" +
                ", name TEXT" +
                ", value TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        throw UnsupportedOperationException() // we have single schema version, no migrations yet
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.execSQL("PRAGMA trusted_schema = 0")
        //db.execSQL("PRAGMA trusted_schema = 0")
    }
}

class RowStore(val db: SQLiteDatabase): Closeable {

    fun readByName(name: String): Row {
        val cursor = db.rawQuery("SELECT * FROM test_table WHERE name=?", arrayOf(name))
        cursor.moveToNext()
        return Row(cursor)
    }
    private val insertRef = AtomicReference<SQLiteStatement>()
    @RequiresApi(Build.VERSION_CODES.O)
    fun insert(name: String, value: String): Long =
        withStatement(insertRef, "INSERT INTO test_table (name, value) VALUES (?, ?)") {
            bindString(1, name)
            bindString(2, value)
            executeInsert()
        }


    override fun close() {
        insertRef.getAndSet(null)?.close()
        /*updateRef.getAndSet(null)?.close()
        deleteRef.getAndSet(null)?.close()*/
    }

    fun Row(cursor: Cursor): Row {
        return Row(cursor.getLong(0), cursor.getString(1), cursor.getString(2))
    }

    private inline fun <R> withStatement(
        reference: AtomicReference<SQLiteStatement>,
        @Language("SQL") query: String,
        block: SQLiteStatement.() -> R
    ): R {
        val statement = reference.getAndSet(null) ?: db.compileStatement(query)
        try {
            return statement.block()
        } finally {
            reference.getAndSet(statement)?.close()
        }
    }
}
