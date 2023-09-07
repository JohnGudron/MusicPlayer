package org.hyperskill.musicplayer

data class Song(val id: Long, val title: String, val artist:String, val duration:Long)

sealed class PlayerItemType

data class Track(val song: Song, var state: TrackState) : PlayerItemType()

data class SongSelector(val song: Song, var state: SelectState): PlayerItemType()

data class PlayList(val name: String, val songs: List<Song>)

enum class TrackState {
    PLAYING,
    PAUSED,
    STOPPED
}

enum class PlayerState {
    PLAY_MUSIC,
    ADD_PLAYLIST
}

enum class SelectState {
    IS_SELECTED,
    NOT_SELECTED
}
