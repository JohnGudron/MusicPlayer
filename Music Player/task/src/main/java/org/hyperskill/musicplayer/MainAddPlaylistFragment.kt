package org.hyperskill.musicplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment


class MainAddPlaylistFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main_add_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val okBtn = view.findViewById<Button>(R.id.addPlaylistBtnOk)
        val cancelBtn = view.findViewById<Button>(R.id.addPlaylistBtnCancel)
        val name = view.findViewById<EditText>(R.id.addPlaylistEtPlaylistName)

        cancelBtn.setOnClickListener {
            (requireActivity() as MainActivity).changePlayerMode(PlayerState.PLAY_MUSIC)
        }

        okBtn.setOnClickListener {
            val list = (requireActivity() as MainActivity).getSelectedSongs()
            when {
                list.isEmpty() -> {
                    Toast.makeText(context, "Add at least one song to your playlist", Toast.LENGTH_LONG).show()
                } name.text.toString().isEmpty() -> {
                    Toast.makeText(context, "Add a name to your playlist", Toast.LENGTH_LONG).show()
                } name.text.toString() == "All Songs" -> {
                    Toast.makeText(context, "All Songs is a reserved name choose another playlist name", Toast.LENGTH_LONG).show()
                }
                else -> {
                    (requireActivity() as MainActivity).addPlaylist(name.text.toString(), list)
                    (requireActivity() as MainActivity).changePlayerMode(PlayerState.PLAY_MUSIC)
                }
            }
        }
    }

}