package org.hyperskill.musicplayer

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import java.nio.channels.Selector

class SongRecyclerAdapter(
    playerState: PlayerState,
    private val onItemPlayPauseBtnLongClick: (Track) -> Unit,
    private val onItemPlayPauseBtnClick: (Track) -> Unit
)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    //var tracks = songList.map { Track(it, TrackState.STOPPED) }
    //var selectors = songList.map {SongSelector(it, SelectState.NOT_SELECTED)}
    var data: List<PlayerItemType> = emptyList()
    var state = playerState

    companion object {
        private const val PLAY_VIEW = 1
        private const val SELECTOR_VIEW = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when(viewType) {
            PLAY_VIEW -> {
                SongViewHolder(inflater.inflate(R.layout.list_item_song,parent,false))
            } else -> {
                SelectorViewHolder(inflater.inflate(R.layout.list_item_song_selector,parent, false))
            }
        }
    }

    override fun getItemCount(): Int {
       return data.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (data[position]) {
            is SongSelector -> SELECTOR_VIEW
            is Track -> PLAY_VIEW
            else -> throw IllegalArgumentException("Invalid data type at position $position")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SongViewHolder -> {
                val track: Track = data[position] as Track
                holder.onBind(track.song, track.state)
                holder.playBtn.setOnClickListener {
                    onItemPlayPauseBtnClick(
                        Track(
                            track.song,
                            if (track.state == TrackState.PLAYING) TrackState.PAUSED else TrackState.PLAYING
                        )
                    ) // changing track state in argument
                }
                holder.itemView.setOnLongClickListener {
                    onItemPlayPauseBtnLongClick(track)
                    true
                }
            }
            is SelectorViewHolder -> {
                val selector: SongSelector = data[position] as SongSelector
                holder.onBind(selector.song, selector.state)
                holder.itemView.setOnClickListener {
                    holder.checkBox.isChecked = !holder.checkBox.isChecked
                    if (holder.checkBox.isChecked) {
                        holder.itemView.setBackgroundColor(Color.LTGRAY)
                        (data[position] as SongSelector).state = SelectState.IS_SELECTED
                    } else {
                        holder.itemView.setBackgroundColor(Color.WHITE)
                        (data[position] as SongSelector).state = SelectState.NOT_SELECTED
                    }
                }
            }
        }
    }

    fun updateDataAndState(items: List<PlayerItemType>, newState: PlayerState) {
        data = items
        if (state!= newState) state = newState
        notifyDataSetChanged()
    }
    }

    /*fun updateTrackList(trackList: List<Track>) {
        tracks = trackList
        notifyDataSetChanged()
    }

    fun updateSelectorList(songList: List<Song>) {
        selectors = songList.map { (SongSelector(it, SelectState.NOT_SELECTED)) }
        notifyDataSetChanged()
    }*/



    /*fun updateState(newState: PlayerState) {

        notifyDataSetChanged()
    }*/


class SongViewHolder(songView: View) : RecyclerView.ViewHolder(songView) {
    val playBtn: ImageButton = songView.findViewById(R.id.songItemImgBtnPlayPause)
    private val artist: TextView = songView.findViewById(R.id.songItemTvArtist)
    private val title: TextView = songView.findViewById(R.id.songItemTvTitle)
    private val duration: TextView = songView.findViewById(R.id.songItemTvDuration)

    fun onBind(song: Song, trackState: TrackState) {
        playBtn.setImageResource(
            if (trackState == TrackState.PLAYING) R.drawable.ic_pause
            else R.drawable.ic_play
        )
        artist.text = song.artist
        title.text = song.title
        duration.text = song.duration.toFormattedTime()
    }

}

class SelectorViewHolder(selectorView: View) : RecyclerView.ViewHolder(selectorView) {
    val checkBox: CheckBox = selectorView.findViewById(R.id.songSelectorItemCheckBox)
    private val artist: TextView = selectorView.findViewById(R.id.songSelectorItemTvArtist)
    private val title: TextView = selectorView.findViewById(R.id.songSelectorItemTvTitle)
    private val duration: TextView = selectorView.findViewById(R.id.songSelectorItemTvDuration)

    fun onBind(song: Song, selectState: SelectState) {
        checkBox.isChecked = (
                selectState == SelectState.IS_SELECTED
        )
        if (checkBox.isChecked) {
            itemView.setBackgroundColor(Color.LTGRAY)}
        artist.text = song.artist
        title.text = song.title
        duration.text = song.duration.toFormattedTime()
    }
}

fun Long.toFormattedTime(): String {
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}