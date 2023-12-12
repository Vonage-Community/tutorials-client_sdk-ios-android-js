//
//  ContentView.swift
//  Chat
//
//  Created by Abdulhakim Ajetunmobi on 31/08/2023.
//

import SwiftUI
import VonageClientSDKChat

struct ContentView: View {
    @StateObject private var loginModel = LoginViewModel()
    
    var body: some View {
        NavigationStack {
            VStack {
                Button("Login as Alice") {
                    Task {
                        await loginModel.login("Alice")
                    }
                }.buttonStyle(.bordered)
                Button("Login as Bob") {
                    Task {
                        await loginModel.login("Bob")
                    }
                }.buttonStyle(.bordered)
            }
            .padding()
            .navigationDestination(isPresented: $loginModel.isLoggedIn) {
                let chatViewModel = ChatViewModel(client: loginModel.client)
                ChatView(chatViewModel: chatViewModel)
            }
        }.alert(isPresented: $loginModel.isError) {
            Alert(title: Text(loginModel.error))
        }
    }
}

@MainActor
final class LoginViewModel: ObservableObject {
    @Published var error = ""
    @Published var isError = false
    @Published var isLoggedIn = false
    
    private let aliceJwt = "ALICE_JWT"
    private let bobJwt = "BOB_JWT"
    
    let client = VGChatClient()
    
    func login(_ username: String) async {
        do {
            let jwt = username == "Alice" ? aliceJwt : bobJwt
            try await client.createSession(jwt)
            isLoggedIn = true
        } catch {
            self.error = error.localizedDescription
            self.isError = true
        }
    }
}
