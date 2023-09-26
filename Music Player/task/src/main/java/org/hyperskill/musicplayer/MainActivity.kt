package org.hyperskill.musicplayer

import android.app.AlertDialog
import android.content.ContentUris
import android.media.MediaPlayer
import android.net.Uri
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
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.findFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    lateinit var adapter: SongRecyclerAdapter
    private lateinit var loadDialogAdapter: ArrayAdapter<String>
    private lateinit var deleteDialogAdapter: ArrayAdapter<String>
    private lateinit var loadDialog: AlertDialog
    private lateinit var deleteDialog: AlertDialog
    private lateinit var fragmentContainer: FragmentContainerView
    var mediaPlayer: MediaPlayer? = null
    val vm: MusicPlayerViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Recycler View initiating
        adapter = SongRecyclerAdapter(vm.playerState.value!!, ::onItemPlayPauseBtnLongClick, ::onItemPlayPauseBtnClick)
        recyclerView = findViewById<RecyclerView>(R.id.mainSongList)
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
            if (mediaPlayer == null) {

                vm.unPrepareMediaPlayer()
                val newMediaPlayer = MediaPlayer.create(this,R.raw.wisdom)
                var init = true

                newMediaPlayer.setOnCompletionListener {
                    it.seekTo(0)
                    vm.unPrepareMediaPlayer()
                    it.stop()
                    it.prepareAsync()
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

            val foundSongs = findSongs()// implement here finding functionality
            vm.updateAllSongs(foundSongs)
            when (vm.playerState.value) {
                PlayerState.PLAY_MUSIC -> {
                    // TODO need to find all cases of changing current track (I think only in setCurrentPlaylist fun maybe)
                    vm.setCurrentPlaylist("All Songs",vm.allSongs.value ?: emptyList<Song>())
                } PlayerState.ADD_PLAYLIST -> {
                vm.setSelectorPlaylist(
                    "All Songs",
                    vm.allSongs.value?.map { SongSelector(it, SelectState.NOT_SELECTED) }
                        ?: emptyList<SongSelector>())
                }
            }

        }

        vm.currentPlaylist.observe(this) { playlist ->
            adapter.updateDataAndState(playlist.second, vm.playerState.value!!)
        }

        vm.currentSelectorlist.observe(this) { playlist ->
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_bar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mainMenuAddPlaylist -> {
                if (vm.playerState.value != PlayerState.ADD_PLAYLIST) {
                    if ((vm.allSongs.value ?: emptyList<Song>()).isEmpty()) {
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

    // This fun for finding songs in DBMS
    fun findSongs(): List<Song> {
        //TODO
        return listOf(
            Song(1,"title1", "artist1", 215000),
            Song(2,"title2", "artist2", 215000),
            Song(3,"title3", "artist3", 215000),
            Song(4,"title4", "artist4", 215000),
            Song(5,"title5", "artist5", 215000),
            Song(6,"title6", "artist6", 215000),
            Song(7,"title7", "artist7", 215000),
            Song(8,"title8", "artist8", 215000),
            Song(9,"title9", "artist9", 215000),
            Song(10,"title10", "artist10", 215000)
        )
    }

    // This fun change current mode to mode received in argument
    fun changePlayerMode(mode: PlayerState) {
        if (mode == PlayerState.PLAY_MUSIC) {
            vm.changePlayerState(PlayerState.PLAY_MUSIC)
            displayPlayerControlFragment()
            findViewById<Button>(R.id.tempBtn).text = vm.playerState.value.toString()
        } else {
            vm.changePlayerState(PlayerState.ADD_PLAYLIST)
            displayAddPlaylistFragment()
            findViewById<Button>(R.id.tempBtn).text = vm.playerState.value.toString()
        }
    }

    fun changeTrackMP() {
        mediaPlayer!!.reset()
        vm.unPrepareMediaPlayer()
        val newMediaPlayer = MediaPlayer.create(this,R.raw.wisdom)
        var changing = true
        var afterStop = false
        newMediaPlayer.setOnCompletionListener {
            it.seekTo(0)
            vm.unPrepareMediaPlayer()
            it.stop()
            it.prepareAsync()
            changeCurrentTrackState(TrackState.STOPPED)
        }
        newMediaPlayer.setOnPreparedListener {
            if (!afterStop) mediaPlayer = newMediaPlayer
            if (changing) {
                mediaPlayer!!.start()
            } else {
                it.seekTo(0) // WARNING
            }
            changing = false
            afterStop = true
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
        vm.addPlaylist(PlayList(name,songs))
    }

    fun getPlaylistByName(name:String): PlayList {
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
        // TODO
        if (vm.currentTrack.value?.song?.id != track.song.id) {
            vm.selectCurrentTrack(track)
            changeTrackMP()
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

    fun createLoadDialog(adapter: ArrayAdapter<String>): AlertDialog {
        return AlertDialog.Builder(this).setTitle("choose playlist to load")
            .setNegativeButton("cancel", null)
            .setAdapter(adapter) { _, position ->
                val selectedPlaylist = getPlaylistByName(adapter.getItem(position)!!)
                if (vm.playerState.value == PlayerState.PLAY_MUSIC) {
                    if (vm.setCurrentPlaylist(selectedPlaylist.name,selectedPlaylist.songs)) {
                        changeTrackMP()
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

    fun createDeleteDialog(adapter: ArrayAdapter<String>): AlertDialog {
        return AlertDialog.Builder(this).setTitle("choose playlist to delete")
            .setNegativeButton("cancel", null)
            .setAdapter(adapter) { _, position ->
                val selectedPlaylist = getPlaylistByName(adapter.getItem(position)!!)
                if (selectedPlaylist.name == vm.currentPlaylist.value?.first) {
                    vm.setCurrentPlaylist("All Songs", vm.allSongs.value ?: emptyList<Song>())
                }
                if (selectedPlaylist.name == vm.currentSelectorlist.value?.first) {
                    vm.setSelectorPlaylist(
                        "All Songs",
                        vm.allSongs.value?.map { SongSelector(it, SelectState.NOT_SELECTED) }
                            ?: emptyList<SongSelector>())
                }
                vm.deletePlaylistByName(selectedPlaylist.name)
                // TODO Just a reminder that there maybe an error, cause observers triggered "data" property inside recyclerView adapter
            }
            .create()
    }
}
