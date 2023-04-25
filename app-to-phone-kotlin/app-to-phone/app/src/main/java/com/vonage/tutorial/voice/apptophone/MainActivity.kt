package com.vonage.tutorial.voice.apptophone

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.vonage.android_core.VGClientConfig
import com.vonage.clientcore.core.api.ClientConfigRegion
import com.vonage.voice.api.CallId
import com.vonage.voice.api.VoiceClient

class MainActivity : AppCompatActivity() {

    //Replace this with your generated JWT
    private val aliceJWT = ""

    private lateinit var client: VoiceClient
    private var onGoingCallID: CallId? = null

    private lateinit var startCallButton: Button
    private lateinit var endCallButton: Button
    private lateinit var connectionStatusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // request permissions
        val callsPermissions = arrayOf(Manifest.permission.RECORD_AUDIO)
        ActivityCompat.requestPermissions(this, callsPermissions, 123)

        // init views
        startCallButton = findViewById(R.id.makeCallButton)
        endCallButton = findViewById(R.id.endCallButton)
        connectionStatusTextView = findViewById(R.id.connectionStatusTextView)

        startCallButton.setOnClickListener {
            startCall()
        }

        endCallButton.setOnClickListener {
            hangup()
        }

        client = VoiceClient(this.application.applicationContext)
        client.setConfig(VGClientConfig(ClientConfigRegion.US))

        client.createSession(aliceJWT) {
            err, sessionId ->
            when {
                err != null -> {
                    connectionStatusTextView.text = err.localizedMessage
                    startCallButton.visibility = View.INVISIBLE
                    endCallButton.visibility = View.INVISIBLE
                }
                else -> {
                    connectionStatusTextView.text = "Connected"
                    startCallButton.visibility = View.VISIBLE
                    endCallButton.visibility = View.INVISIBLE
                }
            }
        }

        client.setOnCallHangupListener { callId, callQuality, isRemote ->
            onGoingCallID = null
        }
    }

    @SuppressLint("MissingPermission")
    fun startCall() {
        client.serverCall(mapOf("to" to "PHONE_NUMBER")) {
            err, outboundCall ->
            when {
                err != null -> {
                    connectionStatusTextView.text = err.localizedMessage
                }
                else -> {
                    onGoingCallID = outboundCall
                    startCallButton.visibility = View.INVISIBLE
                    endCallButton.visibility = View.VISIBLE
                }
            }
        }

    }

    private fun hangup() {
        onGoingCallID?.let {
            client.hangup(it) {
                    err ->
                when {
                    err != null -> {
                        connectionStatusTextView.text = err.localizedMessage
                    }

                    else -> {
                        onGoingCallID = null
                    }
                }
            }
        }

    }
}
