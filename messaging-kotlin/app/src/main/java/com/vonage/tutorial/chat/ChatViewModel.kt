package com.vonage.tutorial.chat

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.AndroidViewModel
import com.vonage.android_core.VGError
import com.vonage.chat.ChatClient
import com.vonage.clientcore.core.api.models.EmbeddedInfo
import com.vonage.clientcore.core.api.models.GetConversationEventsParameters
import com.vonage.clientcore.core.api.models.MemberJoinedConversationEvent
import com.vonage.clientcore.core.api.models.MemberLeftConversationEvent
import com.vonage.clientcore.core.api.models.MessageTextEvent
import com.vonage.clientcore.core.api.models.PersistentConversationEvent
import com.vonage.clientcore.core.api.models.PresentingOrder

class ChatViewModel(application: Application) : AndroidViewModel(application = application) {
    private val aliceJwt = "" //Set to ALICE JWT
    private val bobJwt = "" //Set to BOB JWT
    private val conversationID = "" //Set to created conversation ID that has Bob and Alice as members

    var isLoggedIn by mutableStateOf(false)
    var isError by mutableStateOf(false)
    var error = ""
    private var memberID = "" //logged in users member ID for this conversation

    var events : SnapshotStateList<PersistentConversationEvent> = mutableStateListOf()

    private var client = ChatClient(getApplication<Application>().applicationContext)

    fun login(username: String) {
        val jwt = if(username == "Alice") aliceJwt else bobJwt
        client.createSession(jwt) { err, sessionId ->
            when {
                err != null -> {
                    isError = true
                    error = err.localizedMessage?.toString() ?: ""
                }
                else -> {
                    client.setOnConversationEventListener {
                        events.add(it as PersistentConversationEvent)
                    }
                    isLoggedIn = true
                }
            }
        }
    }

    suspend fun getMemberIDIfNeeded(){
        if(memberID.isNotEmpty()) return else getMemberID()
    }

    private suspend fun getMemberID() {
        try {
            val member = client.getConversationMember(conversationID,  "me")
            memberID = member.id
        }
        catch (e: VGError) {
            //User not yet a member of the conversation
            memberID = client.joinConversation(conversationID)
        }
        catch (err:Error) {
            isError = true
            error = err.localizedMessage?.toString() ?: ""
        }
    }

    suspend fun getConversationEvents(){
        val params = GetConversationEventsParameters(PresentingOrder.ASC,100)
        try {
            val eventsPage = client.getConversationEvents(conversationID, params)
            events.clear()
            events.addAll(eventsPage.events.toMutableStateList())
        }
        catch (err:Error) {
            isError = true
            error = err.localizedMessage?.toString() ?: ""
        }
    }

    suspend fun sendMessage(message: String){
        try {
            client.sendMessageTextEvent(conversationID, message)
        } catch (err:Error) {
            isError = true
            error = err.localizedMessage?.toString() ?: ""
        }
    }

    fun generateDisplayedText(event: PersistentConversationEvent): Pair<String, Boolean>{
        var from = "System"
        return when(event){
            is MemberJoinedConversationEvent -> {
                val from = event.body.user.name
                "$from joined" to false
            }
            is MemberLeftConversationEvent -> {
                val from = event.body.user.name
                "$from left" to false
            }
            is MessageTextEvent -> {
                var isUser = false
                val userInfo = event.from as EmbeddedInfo
                isUser = userInfo.memberId == memberID
                from = if(isUser) "" else "${userInfo.user.name}: "
                "$from ${event.body.text}" to isUser
            }
            else -> {
                "" to false
            }
        }
    }
}