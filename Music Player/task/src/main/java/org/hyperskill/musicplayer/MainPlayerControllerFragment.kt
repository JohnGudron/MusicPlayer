package org.hyperskill.musicplayer

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import kotlin.concurrent.thread


class MainPlayerControllerFragment : Fragment() {

    private lateinit var seekBar: SeekBar
    private lateinit var currentTime: TextView
    private lateinit var handler: Handler
    private var isUserSeeking = false // allow us to stop updating SeekBar when user touches seekbar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main_player_controller, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = (requireActivity() as MainActivity)

        val controllerBtnPlayPause = view.findViewById<Button>(R.id.controllerBtnPlayPause)
        val controllerBtnStop = view.findViewById<Button>(R.id.controllerBtnStop)
        currentTime = view.findViewById<TextView>(R.id.controllerTvCurrentTime)
        val totalTime = view.findViewById<TextView>(R.id.controllerTvTotalTime)

        controllerBtnPlayPause.setOnClickListener {
            if (activity.getCurrentTrackState() == TrackState.PLAYING) {
                activity.changeCurrentTrackState(TrackState.PAUSED)
            } else {
                activity.changeCurrentTrackState(TrackState.PLAYING)
            }
            //TODO
            if (activity.mediaPlayer!!.isPlaying) {
                activity.mediaPlayer!!.pause()
            } else {
                activity.mediaPlayer!!.start()
            }
        }

        controllerBtnStop.setOnClickListener {
            activity.mediaPlayer?.seekTo(0)
            activity.mediaPlayer?.stop()
            activity.changeCurrentTrackState(TrackState.STOPPED)
            activity.mediaPlayer?.prepareAsync()
            seekBar.progress = 0
            currentTime.text = 0L.toFormattedTime()
        }

        seekBar = view.findViewById<SeekBar>(R.id.controllerSeekBar)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    activity.mediaPlayer?.seekTo(progress*1000) //TODO
                    currentTime.text =
                        activity.mediaPlayer?.currentPosition?.toLong()?.toFormattedTime() // TODO
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isUserSeeking = false
            }
        })

        handler = Handler(Looper.getMainLooper())

        activity.vm.currentTrack.observe(activity) {
            // TODO
            if (activity.mediaPlayer != null) {
                seekBar.max = activity.mediaPlayer!!.duration / 1000
                //startUpdatingSeekBar(activity.mediaPlayer)
                // startUpdatingSeekBar(activity.mediaPlayer)
            }
        }

        activity.vm.mediaPlayerState.observe(activity) {
            startUpdatingSeekBar(activity.mediaPlayer)
        }

    }

    fun startUpdatingSeekBar(mediaPlayer: MediaPlayer?) {
        thread {
            // TODO
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed(object : Runnable {
                override fun run() {
                        if (mediaPlayer != null && !isUserSeeking && mediaPlayer.isPlaying) {
                            seekBar.progress = mediaPlayer.currentPosition / 1000  //TODO
                            currentTime.text =
                                (mediaPlayer?.currentPosition?.toLong()
                                    ?: 0L).toFormattedTime() // TODO
                        }
                    handler.postDelayed(this, 1000) // Update every second
                }

            }, 0)
        }
    }
}