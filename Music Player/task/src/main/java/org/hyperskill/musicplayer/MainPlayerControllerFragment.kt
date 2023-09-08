package org.hyperskill.musicplayer

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
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlin.concurrent.thread


class MainPlayerControllerFragment : Fragment() {

    private lateinit var seekBar: SeekBar
    private lateinit var currentTime: TextView
    private lateinit var totalTime: TextView
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
        totalTime = view.findViewById<TextView>(R.id.controllerTvTotalTime)

        controllerBtnPlayPause.setOnClickListener {
            if (activity.getCurrentTrackState() == TrackState.PLAYING) {
                activity.changeCurrentTrackState(TrackState.PAUSED)
            } else {
                activity.changeCurrentTrackState(TrackState.PLAYING)
            }

            if (activity.mediaPlayer != null){
                if (activity.mediaPlayer!!.isPlaying) {
                    activity.mediaPlayer!!.pause()
                } else {
                    activity.mediaPlayer!!.start()
                }
            } else {
                Toast.makeText(activity, "You should find some songs at first", Toast.LENGTH_LONG).show()
            }
        }

        controllerBtnStop.setOnClickListener {
            activity.mediaPlayer?.stop()
            activity.changeCurrentTrackState(TrackState.STOPPED)
            activity.mediaPlayer?.prepare()
            seekBar.progress = 0
            currentTime.text = 0L.toFormattedTime()

        }

        seekBar = view.findViewById<SeekBar>(R.id.controllerSeekBar)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    activity.mediaPlayer?.seekTo(progress*1000)
                    currentTime.text = (progress.toLong() * 1000).toFormattedTime()
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
            // TODO maybe it worth to replace all seekBars features inside startUpdatingSeekbar
            if (activity.mediaPlayer != null) {
                val duration = activity.mediaPlayer!!.duration
                seekBar.max = if (duration % 1000 > 0) duration / 1000 else duration / 1000 + 1
            }
        }

        activity.vm.mediaPlayerState.observe(activity) {
            if (it) startUpdatingSeekBar(activity.mediaPlayer, activity)
        }

    }

    fun startUpdatingSeekBar(mediaPlayer: MediaPlayer?, activity: MainActivity) {
        totalTime.text = (215000).toLong().toFormattedTime()/*(mediaPlayer?.duration?.toLong()
            ?: 0L).toFormattedTime()*/
        //seekBar.max = mediaPlayer!!.duration / 1000
        //seekBar.progress = (mediaPlayer!!.currentPosition) / 1000

        thread {
            // TODO
            handler.removeCallbacksAndMessages(null)

            handler.postDelayed(object : Runnable {
                override fun run() {
                    if (mediaPlayer != null && !isUserSeeking && activity.vm.mediaPlayerState.value!!) {
                        val currentTimeMillis = if (mediaPlayer.currentPosition % 1000 > 0) mediaPlayer.currentPosition + mediaPlayer.currentPosition % 1000 else mediaPlayer.currentPosition
                        seekBar.progress = currentTimeMillis / 1000 //TODO
                        currentTime.text =
                            (currentTimeMillis.toLong()).toFormattedTime() // TODO
                    }
                    handler.postDelayed(this, 100) // Update every second
                }

            }, 0)
        }

    }
}