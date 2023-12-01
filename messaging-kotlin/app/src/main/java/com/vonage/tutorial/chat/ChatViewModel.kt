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
    private val aliceJwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpYXQiOjE3MDEzNTE3MTksImp0aSI6IjNjNGE4MzkwLThmODYtMTFlZS1hOTNhLTkxZDk3OGRmMzYwNCIsImFwcGxpY2F0aW9uX2lkIjoiOTFhMDhiYzgtYzAyZi00NmJiLWExMTktNzA5NjMxMmYxZDRhIiwic3ViIjoiQWxpY2UiLCJleHAiOjE3MDEzNTE3NDEwOTcsImFjbCI6eyJwYXRocyI6eyIvKi9jb252ZXJzYXRpb25zLyoqIjp7fSwiLyovc2Vzc2lvbnMvKioiOnt9LCIvKi9kZXZpY2VzLyoqIjp7fSwiLyovcHVzaC8qKiI6e30sIi8qL2tub2NraW5nLyoqIjp7fSwiLyovbGVncy8qKiI6e319fX0.mbs5RVB8OHPZ09lSCSot4D0eMYmO_FgoQUfIvB5epzULrhK3KLvyJiR4n_y6ZOsjEpwk3ILfmzW_X5ukFebxv1xepOFQ4dRc1kgbn0nXWPnjjBEP4zODl9YbGyWiv0B0ydoNLXV8nFrTHBjUjGmTbXSdmvvl41db-8BDYMOwE-1SqiCZVz8l8xAlXFQMv_tkymra_2RxTNdmxqgnDOXIgv4PsbVUwLgzvdTxSRK8-P_ZpoZcsx_LMOKWFMRH1tK0l83OZp9tjrk6wszr1LGJAS2k8ESbkEAkAY-QNvsDTQhiDASOVdB2Nkiv9tORw9Rn_QonLa9eY40nYWz3E5uA1Q" //Set to ALICE JWT
    private val bobJwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpYXQiOjE3MDEzNTE3NDksImp0aSI6IjRlMTRjZTAwLThmODYtMTFlZS05NGU2LTJkNzE3Y2E5MzEwZSIsImFwcGxpY2F0aW9uX2lkIjoiOTFhMDhiYzgtYzAyZi00NmJiLWExMTktNzA5NjMxMmYxZDRhIiwic3ViIjoiQm9iIiwiZXhwIjoxNzAxMzUxNzcwOTQ0LCJhY2wiOnsicGF0aHMiOnsiLyovY29udmVyc2F0aW9ucy8qKiI6e30sIi8qL3Nlc3Npb25zLyoqIjp7fSwiLyovZGV2aWNlcy8qKiI6e30sIi8qL3B1c2gvKioiOnt9LCIvKi9rbm9ja2luZy8qKiI6e30sIi8qL2xlZ3MvKioiOnt9fX19.jqOwYWeN02R_3XPPTuxShEMAHOq-ZakxlqSLCLXnntUxL_GnO8J6eNKC1ATpLW5g_M1jjbNOY8krCezAwKeMsG2aazHOlfpPkNClSiIFVpLwUdswHRKV94kLxjH6wmwZ_c48u_ZopxgnHD0LgX_L51XFW8YhqWITqA98l5i5bJwg73OhwTopxYJrLr08EBleIyYIS7Ytm11-_P17g6Bbe6iydETzErnom6u7qU0HxeyPzmj9JM-pEY7DjOrlKBdV1_vBJj6HDTZM5VulNNqdr-2oyCZ1eH-keWZIbMntmNlYH3ix-d7wNEB5B3r8ShvizQY7p0IjQERaS1koTqXQfw" //Set to BOB JWT
    private val conversationID = "CON-7be0ac0f-5309-4418-970b-f476cc7e232a" //Set to created conversation ID that has Bob and Alice as members

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
            events = eventsPage.events.toMutableStateList()
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