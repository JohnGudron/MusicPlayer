package org.hyperskill.musicplayer

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentContainerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.lang.IllegalStateException

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SongRecyclerAdapter
    private lateinit var loadDialogAdapter: ArrayAdapter<String>
    private lateinit var deleteDialogAdapter: ArrayAdapter<String>
    private lateinit var loadDialog: AlertDialog
    private lateinit var deleteDialog: AlertDialog
    private lateinit var fragmentContainer: FragmentContainerView

    private lateinit var playlistStore: PlaylistStore //TODO

    var mediaPlayer: MediaPlayer? = null
    val vm: MusicPlayerViewModel by viewModels()

    @SuppressLint("NewApi")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = DbHelper(this).writableDatabase //TODO
        playlistStore = PlaylistStore(db)

        // retrieving all playlists from the DB and adding them to VM
        playlistStore.allPlaylists(findSongs()).forEach {
            vm.addPlaylist(it)
        }

        // Recycler View initiating
        adapter = SongRecyclerAdapter(vm.playerState.value!!, ::onItemPlayPauseBtnLongClick, ::onItemPlayPauseBtnClick)
        recyclerView = findViewById(R.id.mainSongList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false)

        // Fragment container initiating
        fragmentContainer = findViewById(R.id.mainFragmentContainer)

        // Delete and Load dialogs initiating
        loadDialogAdapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            vm.allPlaylists.value?.map { it.name } ?: emptyList())

        deleteDialogAdapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            vm.allPlaylists.value?.map { it.name }?.filter { it != "All Songs" } ?: emptyList())

        deleteDialog = createDeleteDialog(deleteDialogAdapter)
        loadDialog = createLoadDialog(loadDialogAdapter)

        val searchBtn = findViewById<Button>(R.id.mainButtonSearch)
        searchBtn.setOnClickListener {
            //adapter.updateDataAndState(list.map { Track(it, TrackState.STOPPED) }, PlayerState.PLAY_MUSIC)
            when {
                ContextCompat.checkSelfPermission(this.applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED -> {
                    val foundSongs = findSongs()// implement here finding functionality
                    if (mediaPlayer == null) {
                        vm.unPrepareMediaPlayer()
                        // TODO try change to initMediaPlayer function
                        val newMediaPlayer = MediaPlayer.create(this,ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, foundSongs[0].id ))
                        var init = true

                        newMediaPlayer.setOnCompletionListener {
                            it.seekTo(0)
                            vm.unPrepareMediaPlayer()
                            it.stop()
                            it.prepare()
                            changeCurrentTrackState(TrackState.STOPPED)
                        }
                        newMediaPlayer.setOnPreparedListener {
                            if (init) mediaPlayer = newMediaPlayer
                            init = false
                            vm.prepareMediaPlayer()
                            it.seekTo(0) // WARNING
                            // Set the mutable var to the new instance
                        }
                    }
                    vm.updateAllSongs(foundSongs)
                    when (vm.playerState.value) {
                        PlayerState.PLAY_MUSIC -> {
                            // TODO need to find all cases of changing current track (I think only in setCurrentPlaylist fun maybe)
                            vm.setCurrentPlaylist("All Songs", vm.allSongs.value ?: emptyList())
                        }

                        PlayerState.ADD_PLAYLIST -> {
                            vm.setSelectorPlaylist(
                                "All Songs",
                                vm.allSongs.value?.map { SongSelector(it, SelectState.NOT_SELECTED) }
                                    ?: emptyList())
                        }

                        null -> throw IllegalStateException("Something went completely wrong with Player state")
                    }

                }
                ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                    AlertDialog.Builder(this)
                        .setTitle("Permission required")
                        .setMessage("This app needs permission to access this feature.")
                        .setPositiveButton("Grant") { _, _ ->
                            ActivityCompat.requestPermissions(
                                this,arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                               1,
                            )
                        }
                        .setNegativeButton("Cancel", null)
                        .show()

                }
                else -> {
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        1,
                    )

                }
            }
        }

        vm.currentPlaylist.observe(this) { playlist ->
            adapter.updateDataAndState(playlist.second, vm.playerState.value!!)
        }

        vm.currentSelectorList.observe(this) { playlist ->
            adapter.updateDataAndState(playlist.second, vm.playerState.value!!)
        }

        vm.playerState.observe(this) { playerState ->
            if (playerState == PlayerState.ADD_PLAYLIST) {
                adapter.updateDataAndState(vm.allSongs.value?.map { SongSelector(it, SelectState.NOT_SELECTED) }
                    ?: emptyList(), PlayerState.ADD_PLAYLIST)
            } else {
                adapter.updateDataAndState(vm.currentPlaylist.value?.second ?: emptyList<Track>(), PlayerState.PLAY_MUSIC) //TODO
            }
        }

        vm.allPlaylists.observe(this) { playlists ->
            val playlistItems = playlists?.map { it.name } ?: emptyList()
            deleteDialogAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, playlistItems.filter { it!="All Songs" })
            loadDialogAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, playlistItems)
            deleteDialog = createDeleteDialog(deleteDialogAdapter)
            loadDialog = createLoadDialog(loadDialogAdapter)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val foundSongs = findSongs()
                    vm.updateAllSongs(foundSongs)
                    when (vm.playerState.value) {
                        PlayerState.PLAY_MUSIC -> {
                            vm.setCurrentPlaylist("All Songs", vm.allSongs.value ?: emptyList())
                        }

                        PlayerState.ADD_PLAYLIST -> {
                            vm.setSelectorPlaylist(
                                "All Songs",
                                vm.allSongs.value?.map { SongSelector(it, SelectState.NOT_SELECTED) }
                                    ?: emptyList())
                        }

                        null -> throw IllegalStateException("Something went completely wrong with Player state")
                    }
                } else {
                    Toast.makeText(this, "Songs cannot be loaded without permission", Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_bar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mainMenuAddPlaylist -> {
                if (vm.playerState.value != PlayerState.ADD_PLAYLIST) {
                    if ((vm.allSongs.value ?: emptyList()).isEmpty()) {
                        Toast.makeText(
                            this,
                            "no songs loaded, click search to load songs",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    } else {
                        vm.changePlayerState(PlayerState.ADD_PLAYLIST)
                        changePlayerMode(PlayerState.ADD_PLAYLIST)
                    }
                }
            }
            R.id.mainMenuLoadPlaylist -> {
                loadDialog.show()
            }
            R.id.mainMenuDeletePlaylist -> {
                deleteDialog.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun initMediaPlayer() {
        vm.unPrepareMediaPlayer()
        // TODO check case when nothing found
        val newMediaPlayer = MediaPlayer.create(this,ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, vm.currentTrack.value!!.song.id ))
        var init = true

        newMediaPlayer.setOnCompletionListener {
            it.seekTo(0)
            vm.unPrepareMediaPlayer()
            it.stop()
            it.prepare()
            changeCurrentTrackState(TrackState.STOPPED)
        }
        newMediaPlayer.setOnPreparedListener {
            if (init) mediaPlayer = newMediaPlayer
            init = false
            vm.prepareMediaPlayer()
            it.seekTo(0) // WARNING
            // Set the mutable var to the new instance
        }
    }
    private fun findSongs(): List<Song> {
        val songs = mutableListOf<Song>()
        val uri =
            if (Build.VERSION.SDK_INT >= 29) {
                MediaStore.Audio.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )
        val selection = ""
        val selectionArgs = arrayOf<String>()
        val sortOrder = ""
        val query = applicationContext.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                // Then, we get the values of columns for a given image.
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val artist = cursor.getString(artistColumn)
                val duration = cursor.getLong(durationColumn)

                // Finally, we store the result in our defined list.
                if (artist != null) songs.add(Song(id, name, artist, duration)) // TODO narrow place
            }
        }

        return songs
    }

    // This fun change current mode to mode received in argument
    fun changePlayerMode(mode: PlayerState) {
        if (mode == PlayerState.PLAY_MUSIC) {
            vm.changePlayerState(PlayerState.PLAY_MUSIC)
            displayPlayerControlFragment()
        } else {
            vm.changePlayerState(PlayerState.ADD_PLAYLIST)
            displayAddPlaylistFragment()
        }
    }

    private fun changeTrackMP(_changing: Boolean) {
        mediaPlayer!!.reset()
        vm.unPrepareMediaPlayer()
        mediaPlayer!!.setDataSource(this,ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, vm.currentTrack.value!!.song.id))
        mediaPlayer!!.prepare()
        var changing = _changing

        mediaPlayer!!.setOnPreparedListener {
            if (changing) {
                mediaPlayer!!.start()
            } else {
                it.seekTo(0) // WARNING
            }
            changing = false
            vm.prepareMediaPlayer()
        }
    }

    fun changeCurrentTrackState(newState: TrackState) {
        if (vm.currentTrack.value == null) {
            Toast.makeText(this, "You should find some songs at first", Toast.LENGTH_LONG).show()
        } else {
            vm.selectCurrentTrack(Track(vm.currentTrack.value!!.song, newState))
        }
    }

    fun getCurrentTrackState(): TrackState? {
        return vm.currentTrack.value?.state
    }

    fun addPlaylist(name:String, songs: List<Song>) {
        // inserting DB here
        // TODO should implement all features
        if (name in (vm.allPlaylists.value?.map { it.name } ?: listOf<String>())) {
            playlistStore.deletePlaylist(name)
            vm.deletePlaylistByName(name)
        }
        vm.addPlaylist(PlayList(name,songs))
        songs.forEach {
            playlistStore.insert(name, it.id)
        }
    }

    fun deletePlaylistFromDB(name: String) {

    }

    private fun getPlaylistByName(name:String): PlayList {
        return vm.selectPlaylist(name)
    }

    private fun displayAddPlaylistFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, MainAddPlaylistFragment())
            .commit()
    }

    private fun displayPlayerControlFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, MainPlayerControllerFragment())
            .commit()
    }

    private fun onItemPlayPauseBtnClick(track: Track) {
        if (vm.currentTrack.value?.song?.id != track.song.id) {
            vm.selectCurrentTrack(track)
            changeTrackMP(true)
        } else {
            vm.selectCurrentTrack(track)
            if (mediaPlayer?.isPlaying != true) mediaPlayer?.start()
            else mediaPlayer!!.pause()
        }
    }

    private fun onItemPlayPauseBtnLongClick(track: Track) {
        changePlayerMode(PlayerState.ADD_PLAYLIST)
        adapter.updateDataAndState(vm.allSongs.value?.map {
            if (it.id == track.song.id) SongSelector(it, SelectState.IS_SELECTED)
            else SongSelector(it, SelectState.NOT_SELECTED)
        } ?: emptyList(), PlayerState.ADD_PLAYLIST)
    }

    fun getSelectedSongs(): List<Song> {
        return (adapter.data as List<SongSelector>).filter { it.state == SelectState.IS_SELECTED }.map { it.song }
    }

    private fun createLoadDialog(adapter: ArrayAdapter<String>): AlertDialog {
        return AlertDialog.Builder(this).setTitle("choose playlist to load")
            .setNegativeButton("cancel", null)
            .setAdapter(adapter) { _, position ->
                val selectedPlaylist = getPlaylistByName(adapter.getItem(position)!!)
                if (vm.playerState.value == PlayerState.PLAY_MUSIC) {
                    if (vm.setCurrentPlaylist(selectedPlaylist.name,selectedPlaylist.songs)) {
                        // TODO решить всегда ли вызывать эту функцию
                        if (mediaPlayer == null) {
                            initMediaPlayer()
                        }
                        else changeTrackMP(false)
                        val x = mediaPlayer
println()
                    }
                } else {
                    val alreadySelected = getSelectedSongs()
                    val selectors = selectedPlaylist.songs.map {
                        if (it in alreadySelected) SongSelector(it, SelectState.IS_SELECTED)
                        else SongSelector(it, SelectState.NOT_SELECTED)
                    }
                    vm.setSelectorPlaylist(selectedPlaylist.name, selectors)
                }
            }
            .create()
    }

    private fun createDeleteDialog(adapter: ArrayAdapter<String>): AlertDialog {
        return AlertDialog.Builder(this).setTitle("choose playlist to delete")
            .setNegativeButton("cancel", null)
            .setAdapter(adapter) { _, position ->
                val selectedPlaylist = getPlaylistByName(adapter.getItem(position)!!)
                if (selectedPlaylist.name == vm.currentPlaylist.value?.first) {
                    vm.setCurrentPlaylist("All Songs", vm.allSongs.value ?: emptyList())
                }
                if (selectedPlaylist.name == vm.currentSelectorList.value?.first) {
                    vm.setSelectorPlaylist(
                        "All Songs",
                        vm.allSongs.value?.map { SongSelector(it, SelectState.NOT_SELECTED) }
                            ?: emptyList())
                }
                playlistStore.deletePlaylist(selectedPlaylist.name)
                // TODO implement deleting playlist from db
                vm.deletePlaylistByName(selectedPlaylist.name)
                // !Just a reminder that there maybe an error, cause observers triggered "data" property inside recyclerView adapter
            }
            .create()
    }
}
