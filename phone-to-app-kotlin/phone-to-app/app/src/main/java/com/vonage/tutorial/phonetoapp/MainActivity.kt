package com.vonage.tutorial.phonetoapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.vonage.android_core.VGClientConfig
import com.vonage.clientcore.core.api.ClientConfigRegion
import com.vonage.voice.api.*

class MainActivity : AppCompatActivity() {

    private val aliceJWT = ""
    private var onGoingCallID: CallId? = null
    private var callInviteID: CallId? = null
    private lateinit var client: VoiceClient

    private lateinit var connectionStatusTextView: TextView
    private lateinit var answerCallButton: Button
    private lateinit var rejectCallButton: Button
    private lateinit var endCallButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // request permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 123);
        }

        // init views
        connectionStatusTextView = findViewById(R.id.connectionStatusTextView)
        answerCallButton = findViewById(R.id.answerCallButton)
        rejectCallButton = findViewById(R.id.rejectCallButton)
        endCallButton = findViewById(R.id.endCallButton)

        answerCallButton.setOnClickListener { answerCall() }
        rejectCallButton.setOnClickListener { rejectCall() }
        endCallButton.setOnClickListener { endCall() }

        client = VoiceClient(this.application.applicationContext)
        val config = VGClientConfig(ClientConfigRegion.US)
        config.enableWebsocketInvites = true
        client.setConfig(config)

        client.setCallInviteListener { callId, from, channelType ->
            callInviteID = callId
            runOnUiThread {
                answerCallButton.visibility = View.VISIBLE
                rejectCallButton.visibility = View.VISIBLE
                endCallButton.visibility = View.GONE
            }
        }

        client.setOnCallHangupListener { callId, callQuality, isRemote ->
            onGoingCallID = null
            answerCallButton.visibility = View.GONE
            rejectCallButton.visibility = View.GONE
            endCallButton.visibility = View.GONE
        }

        client.createSession(aliceJWT) {
                err, sessionId ->
            when {
                err != null -> {
                    connectionStatusTextView.text = err.localizedMessage
                }
                else -> {
                    connectionStatusTextView.text = "Connected"
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun answerCall() {
        callInviteID?.let {
            client.answer(it) {
                    err ->
                when {
                    err != null -> {
                        connectionStatusTextView.text = err.localizedMessage
                    }

                    else -> {
                        onGoingCallID = it
                        answerCallButton.visibility = View.GONE
                        rejectCallButton.visibility = View.GONE
                        endCallButton.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun rejectCall() {
        callInviteID?.let {
            client.reject(it) { err ->
                when {
                    err != null -> {
                        connectionStatusTextView.text = err.localizedMessage
                    }

                    else -> {
                        answerCallButton.visibility = View.GONE
                        rejectCallButton.visibility = View.GONE
                        endCallButton.visibility = View.GONE
                    }
                }
            }
            onGoingCallID = null
        }
    }

    private fun endCall() {
        onGoingCallID?.let {
            client.hangup(it) {
                    err ->
                when {
                    err != null -> {
                        connectionStatusTextView.text = err.localizedMessage
                    }

                    else -> {
                        answerCallButton.visibility = View.GONE
                        rejectCallButton.visibility = View.GONE
                        endCallButton.visibility = View.GONE
                    }
                }
            }
        }
        onGoingCallID = null
    }
}