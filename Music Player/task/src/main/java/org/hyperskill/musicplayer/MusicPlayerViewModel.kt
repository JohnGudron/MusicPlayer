package org.hyperskill.musicplayer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MusicPlayerViewModel : ViewModel() {

    // we change only _currentPlaylist value and only inside vm, currentPlaylist for exposing data for Views
    private val _currentPlaylist = MutableLiveData<Pair<String, List<Track>>>()
    val currentPlaylist: LiveData<Pair<String, List<Track>>> get() = _currentPlaylist

    private val _currentSelectorList = MutableLiveData<Pair<String, List<SongSelector>>>()
    val currentSelectorList: LiveData<Pair<String, List<SongSelector>>> get() = _currentSelectorList

    private val _allSongs = MutableLiveData<List<Song>>(emptyList())
    val allSongs: LiveData<List<Song>> get() = _allSongs

    private val _allPlaylists = MutableLiveData(listOf(PlayList("All Songs", allSongs.value!!)))
    val allPlaylists: LiveData<List<PlayList>> get() = _allPlaylists

    // same scheme as for currentPlaylist
    private val _currentTrack = MutableLiveData<Track?>()
    val currentTrack: LiveData<Track?> get() = _currentTrack

    private val _playerState = MutableLiveData(PlayerState.PLAY_MUSIC)
    val playerState: LiveData<PlayerState> get() = _playerState

    private val _mediaPlayerPrepared = MutableLiveData(false)
    val mediaPlayerState: LiveData<Boolean> get() = _mediaPlayerPrepared

    fun prepareMediaPlayer() {
        _mediaPlayerPrepared.value = true
    }

    fun unPrepareMediaPlayer() {
        _mediaPlayerPrepared.value = false
    }

    fun addPlaylist(playlist: PlayList) {
        _allPlaylists.value = (_allPlaylists.value ?: emptyList()) + playlist
    }

    fun deletePlaylistByName(name:String) {
        _allPlaylists.value = (_allPlaylists.value ?: emptyList()) - selectPlaylist(name)
    }

    private fun updatePlaylist(updatedPlaylist: PlayList) {
        val currentPlaylists = _allPlaylists.value ?: emptyList()
        val updatedList = currentPlaylists.map { playlist ->
            if (playlist.name == updatedPlaylist.name) {
                updatedPlaylist // Replace the existing playlist with the updated one
            } else {
                playlist // Keep other playlists as they are
            }
        }
        _allPlaylists.value = updatedList
    }

    fun updateAllSongs(songs: List<Song>) {
        _allSongs.value = songs
        updatePlaylist(PlayList("All Songs", allSongs.value!!))
    }

    fun setCurrentPlaylist(name: String, songs: List<Song>): Boolean {
        val tracks = songs.map { Track(it, TrackState.STOPPED) }
        var setCurrentTrack = true
        tracks.forEach {
            // saving current track state
            if (it.song.id == currentTrack.value?.song?.id) {
                setCurrentTrack = false
                it.state = currentTrack.value!!.state
            }
        }
        // no need to use selectCurrentTrack fun cause changing current track won't affect other entities
        if (setCurrentTrack) _currentTrack.value = tracks.first()
        _currentPlaylist.value = Pair(name,tracks)

        return setCurrentTrack // need for changing track in MP
    }

    fun setSelectorPlaylist(name: String, songs: List<SongSelector>) {
        _currentSelectorList.value = Pair(name,songs)
    }

    private fun changeTrackState(track: Track, newState: TrackState) {
        val currentPlaylistValue = _currentPlaylist.value?.second.orEmpty()
        val updatedPlaylist = currentPlaylistValue.map { oldTrack ->
            if (oldTrack.song.id == track.song.id) {
                oldTrack.copy(state = newState)
            } else {
                oldTrack
            }
        }
        _currentPlaylist.value = Pair(_currentPlaylist.value!!.first,updatedPlaylist)
    }

    fun selectCurrentTrack(track: Track) {
        if (track.song.id != currentTrack.value?.song?.id && currentTrack.value != null) { // new current track chosen
            changeTrackState(currentTrack.value!!, TrackState.STOPPED) // changing state of the old current track
            _currentTrack.value = track
            changeTrackState(track, track.state)
        } else {
            // there is no current track now or the same track chosen
            _currentTrack.value = track
            changeTrackState(currentTrack.value!!, track.state)
        }
    }

    fun changePlayerState(newState: PlayerState) {
        _playerState.value = newState
    }

    fun selectPlaylist(name: String): PlayList {
        return allPlaylists.value!!.find { it.name == name }!!
    }
}