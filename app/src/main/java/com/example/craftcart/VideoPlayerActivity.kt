package com.example.craftcart

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class VideoPlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PRODUCT_ID = "EXTRA_PRODUCT_ID"
        const val EXTRA_PRODUCT_NAME = "EXTRA_PRODUCT_NAME"
        const val EXTRA_VIDEO_RES_ID = "EXTRA_VIDEO_RES_ID"

        private const val KEY_PLAYBACK_POSITION = "KEY_PLAYBACK_POSITION"
        private const val KEY_PLAY_WHEN_READY = "KEY_PLAY_WHEN_READY"
    }

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var loadingProgress: ProgressBar

    private var videoResId: Int = 0
    private var playbackPosition: Long = 0L
    private var playWhenReady: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        videoResId = intent.getIntExtra(EXTRA_VIDEO_RES_ID, 0)
        val productName = intent.getStringExtra(EXTRA_PRODUCT_NAME)

        playerView = findViewById(R.id.playerView)
        loadingProgress = findViewById(R.id.videoLoadingProgress)

        val titleView = findViewById<TextView>(R.id.videoTitle)
        if (!productName.isNullOrBlank()) {
            titleView?.text = productName
        }

        findViewById<TextView>(R.id.videoBackButton)?.setOnClickListener {
            finish()
        }

        if (savedInstanceState != null) {
            playbackPosition = savedInstanceState.getLong(KEY_PLAYBACK_POSITION, 0L)
            playWhenReady = savedInstanceState.getBoolean(KEY_PLAY_WHEN_READY, true)
        }

        if (videoResId == 0) {
            Toast.makeText(this, R.string.video_playback_error, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        player?.let {
            playbackPosition = it.currentPosition
            playWhenReady = it.playWhenReady
        }
        outState.putLong(KEY_PLAYBACK_POSITION, playbackPosition)
        outState.putBoolean(KEY_PLAY_WHEN_READY, playWhenReady)
    }

    private fun initializePlayer() {
        if (player != null || videoResId == 0) return

        val exoPlayer = ExoPlayer.Builder(applicationContext).build()
        player = exoPlayer
        playerView.player = exoPlayer

        val videoUri = Uri.parse("android.resource://$packageName/$videoResId")
        val mediaItem = MediaItem.fromUri(videoUri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.playWhenReady = playWhenReady
        exoPlayer.seekTo(playbackPosition)

        loadingProgress.visibility = View.VISIBLE

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        loadingProgress.visibility = View.VISIBLE
                    }
                    Player.STATE_READY -> {
                        loadingProgress.visibility = View.GONE
                    }
                    Player.STATE_ENDED -> {
                        loadingProgress.visibility = View.GONE
                    }
                    Player.STATE_IDLE -> {
                        // Remains or waiting
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                loadingProgress.visibility = View.GONE
                Toast.makeText(this@VideoPlayerActivity, R.string.video_playback_error, Toast.LENGTH_SHORT).show()
                finish()
            }
        })

        exoPlayer.prepare()
    }

    private fun releasePlayer() {
        player?.let {
            playbackPosition = it.currentPosition
            playWhenReady = it.playWhenReady
            it.release()
        }
        player = null
        playerView.player = null
    }
}
