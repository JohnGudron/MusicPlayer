package org.hyperskill.musicplayer

import android.annotation.SuppressLint
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi

class TestActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        val db = TestDbHelper(this).writableDatabase
        val store = RowStore(db)

        val showBtn = findViewById<Button>(R.id.show_btn)
        val insertBtn = findViewById<Button>(R.id.insert_btn)
        val nameText = findViewById<EditText>(R.id.rowNameEditText)
        val valueText = findViewById<EditText>(R.id.rowValueEditText)
        val name = findViewById<TextView>(R.id.rowNameTextView)
        val value = findViewById<TextView>(R.id.rowValueTextView)


        insertBtn.setOnClickListener {
            store.insert("${nameText.text}", "${valueText.text}")
        }
        showBtn.setOnClickListener {
            val row = store.readByName("${nameText.text}")
            name.text = "${row.name}"
            value.text = "${row.value}"
        }





    }
}