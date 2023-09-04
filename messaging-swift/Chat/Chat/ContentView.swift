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
                TextField("Token", text: $loginModel.jwt)
                    .textFieldStyle(.roundedBorder)
                Button("Login") {
                    Task {
                        await loginModel.login()
                    }
                }.buttonStyle(.bordered)
            }
            .padding()
            .navigationDestination(isPresented: $loginModel.isLoggedIn) {
                if let client = loginModel.client {
                    let chatViewModel = ChatViewModel(client: client)
                    ChatView(chatViewModel: chatViewModel)
                }
            }
        }.alert(isPresented: $loginModel.isError) {
            Alert(title: Text(loginModel.error))
        }
    }
}

@MainActor
final class LoginViewModel: ObservableObject {
    @Published var jwt = ""
    @Published var error = ""
    @Published var isError = false
    @Published var isLoggedIn = false
    var client: VGChatClient?
    
    func login() async {
//        VGBaseClient.setDefaultLoggingLevel(.verbose)
        let config = VGClientConfig(region: .US)
        client = VGChatClient()
        client?.setConfig(config)
        
        do {
            let _ = try await client?.createSession(jwt)
            isLoggedIn = true
        } catch {
            self.error = error.localizedDescription
            self.isError = true
        }
    }
}
