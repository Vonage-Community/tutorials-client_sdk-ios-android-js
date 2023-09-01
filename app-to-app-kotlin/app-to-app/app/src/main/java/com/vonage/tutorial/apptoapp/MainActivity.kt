package com.vonage.tutorial.apptoapp

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.forEach
import com.vonage.android_core.VGClientConfig
import com.vonage.clientcore.core.api.ClientConfigRegion
import com.vonage.voice.api.*

class MainActivity : AppCompatActivity() {

    //Replace this with your generated JWT's
    private val aliceJWT = ""
    private val bobJWT = ""

    private lateinit var client: VoiceClient
    private var otherUser: String = ""
    private var onGoingCallID: CallId? = null
    private var callInviteID: CallId? = null

    private lateinit var connectionStatusTextView: TextView
    private lateinit var waitingForIncomingCallTextView: TextView
    private lateinit var loginAsAlice: Button
    private lateinit var loginAsBob: Button
    private lateinit var startCallButton: Button
    private lateinit var answerCallButton: Button
    private lateinit var rejectCallButton: Button
    private lateinit var endCallButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // request permissions
        val callsPermissions = arrayOf(Manifest.permission.RECORD_AUDIO)
        ActivityCompat.requestPermissions(this, callsPermissions, 123)

        // init views
        connectionStatusTextView = findViewById(R.id.connectionStatusTextView)
        waitingForIncomingCallTextView = findViewById(R.id.waitingForIncomingCallTextView)
        loginAsAlice = findViewById(R.id.loginAsAlice)
        loginAsBob = findViewById(R.id.loginAsBob)
        startCallButton = findViewById(R.id.startCallButton)
        answerCallButton = findViewById(R.id.answerCallButton)
        rejectCallButton = findViewById(R.id.rejectCallButton)
        endCallButton = findViewById(R.id.endCallButton)

        loginAsAlice.setOnClickListener { loginAsAlice() }
        loginAsBob.setOnClickListener { loginAsBob() }
        answerCallButton.setOnClickListener { answerCall() }
        rejectCallButton.setOnClickListener { rejectCall() }
        endCallButton.setOnClickListener { endCall() }
        startCallButton.setOnClickListener { startCall() }

        client = VoiceClient(this.application.applicationContext)
        val config = VGClientConfig(ClientConfigRegion.US)
        config.enableWebsocketInvites = true
        client.setConfig(config)

        client.setCallInviteListener { callId, from, channelType ->
            callInviteID = callId
            runOnUiThread {
                hideUI()
                answerCallButton.visibility = View.VISIBLE
                rejectCallButton.visibility = View.VISIBLE
            }
        }

        client.setOnCallHangupListener { callId, callQuality, isRemote ->
            onGoingCallID = null
            runOnUiThread {
                hideUI()
                startCallButton.visibility = View.VISIBLE
                waitingForIncomingCallTextView.visibility = View.VISIBLE
            }
        }

    }

    private fun hideUI() {
        val content = findViewById<LinearLayout>(R.id.content)
        content.forEach { it.visibility = View.GONE }
    }

    private fun loginAsAlice() {
        otherUser = "Bob"
        client.createSession(aliceJWT) {
                err, sessionId ->
            when {
                err != null -> {
                    runOnUiThread {
                        hideUI()
                        connectionStatusTextView.visibility = View.VISIBLE
                        connectionStatusTextView.text = err.localizedMessage
                    }
                }
                else -> {
                    runOnUiThread {
                        hideUI()
                        connectionStatusTextView.visibility = View.VISIBLE
                        connectionStatusTextView.text = "Connected"
                        startCallButton.visibility = View.VISIBLE
                        waitingForIncomingCallTextView.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun loginAsBob() {
        otherUser = "Alice"
        client.createSession(bobJWT) {
                err, sessionId ->
            when {
                err != null -> {
                    runOnUiThread {
                        hideUI()
                        connectionStatusTextView.visibility = View.VISIBLE
                        connectionStatusTextView.text = err.localizedMessage
                    }
                }
                else -> {
                    runOnUiThread {
                        hideUI()
                        connectionStatusTextView.visibility = View.VISIBLE
                        connectionStatusTextView.text = "Connected"
                        startCallButton.visibility = View.VISIBLE
                        waitingForIncomingCallTextView.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startCall() {
        client.serverCall(mapOf("to" to otherUser)) {
                err, outboundCall ->
            when {
                err != null -> {
                    runOnUiThread {
                        connectionStatusTextView.text = err.localizedMessage
                    }
                }
                else -> {
                    onGoingCallID = outboundCall
                    runOnUiThread {
                        hideUI()
                        endCallButton.visibility = View.VISIBLE
                    }
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
                        runOnUiThread {
                            connectionStatusTextView.text = err.localizedMessage
                        }
                    }

                    else -> {
                        onGoingCallID = it
                        runOnUiThread {
                            hideUI()
                            endCallButton.visibility = View.VISIBLE
                        }
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
                        runOnUiThread {
                            connectionStatusTextView.text = err.localizedMessage
                        }
                    }

                    else -> {
                        hideUI()
                        runOnUiThread {
                            startCallButton.visibility = View.VISIBLE
                            waitingForIncomingCallTextView.visibility = View.VISIBLE
                        }
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
                        runOnUiThread {
                            connectionStatusTextView.text = err.localizedMessage
                        }
                    }

                    else -> {
                        runOnUiThread {
                            hideUI()
                            startCallButton.visibility = View.VISIBLE
                            waitingForIncomingCallTextView.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
        onGoingCallID = null
    }
}
