package com.vonage.tutorial.chat

import android.app.Application
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.runBlocking


class MainActivity : ComponentActivity() {
    private val chatState by viewModels<ChatViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompositionLocalProvider(LocalChatState provides chatState) {
                ApplicationSwitcher()
            }
        }
    }
}

val LocalChatState = compositionLocalOf<ChatViewModel> { error("Login State Context Not Found!") }

@Composable
fun ApplicationSwitcher() {
    val vm = LocalChatState.current
    val error = vm.error
    if (vm.isLoggedIn) {
        ChatScreen()
    } else {
        LoginScreen()
    }
    if(vm.isError){
        Toast.makeText(LocalContext.current, error, Toast.LENGTH_LONG).show()
        vm.isError = false
    }
}

@Composable
fun LoginScreen() {
    val vm = LocalChatState.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { vm.login("Alice") }) {
            Text("Login as Alice")
        }
        Button(onClick = { vm.login("Bob") }) {
            Text("Login as Bob")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    val vm = LocalChatState.current
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (vm.events.isEmpty()){
            CircularProgressIndicator(
                modifier = Modifier.width(64.dp).height(300.dp),
                color = MaterialTheme.colorScheme.secondary,
            )
        } else {
            LazyColumn() {
                items(vm.events) { event ->
                    val (text, isUser) = vm.generateDisplayedText(event)
                    Text(text)
                }
            }
        }
        Row(){
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Message") }
            )
            Button(onClick = {
                runBlocking {
                    vm.sendMessage(text)
                }
            }) {
                Text("Send")
            }
        }
    }

    runBlocking {
        vm.getMemberIDIfNeeded()
        vm.getConversationEvents()
    }
}