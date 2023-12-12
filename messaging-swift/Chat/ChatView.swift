//
//  ChatView.swift
//  Chat
//
//  Created by Abdulhakim Ajetunmobi on 31/08/2023.
//

import SwiftUI
import VonageClientSDKChat

struct ChatView: View {
    @StateObject var chatViewModel: ChatViewModel
    @State private var message: String = ""
    
    var body: some View {
        VStack {
            if chatViewModel.events.isEmpty {
                ProgressView()
            } else {
                VStack {
                    List {
                        ForEach(chatViewModel.events, id: \.id) { event in
                            switch event.kind {
                            case .memberJoined, .memberLeft:
                                let displayText = chatViewModel.generateDisplayText(event)
                                Text(displayText.body)
                                    .frame(maxWidth: .infinity, alignment: .center)
                            case.messageText:
                                let displayText = chatViewModel.generateDisplayText(event)
                                Text(displayText.body)
                                    .frame(maxWidth: .infinity, alignment: displayText.isUser ? .trailing : .leading)
                            default:
                                EmptyView()
                            }
                        }.listRowSeparator(.hidden)
                    }.listStyle(.plain)
                    
                    Spacer()
                    
                    HStack {
                        TextField("Message", text: $message)
                        Button("Send") {
                            Task {
                                await chatViewModel.sendMessage(message)
                                self.message = ""
                            }
                        }.buttonStyle(.bordered)
                    }.padding(8)
                }
            }
        }.onAppear {
            Task {
                await chatViewModel.getMemberIDIfNeeded()
                await chatViewModel.getConversationEvents()
            }
        }
    }
}

@MainActor
final class ChatViewModel: NSObject, ObservableObject {
    private let conversationID = "CON-ID"
    
    private var client: VGChatClient
    private var memberID: String?
    
    @Published var events: [VGPersistentConversationEvent] = []
    
    init(client: VGChatClient) {
        self.client = client
        super.init()
        client.delegate = self
    }
    
    func getMemberIDIfNeeded() async {
        guard memberID == nil else { return }
        await getMemberID()
    }
    
    private func getMemberID() async {
        let member = try? await client.getConversationMember(conversationID, memberId: "me")
        memberID = member?.id
        
        if memberID == nil {
            memberID = try? await client.joinConversation(conversationID)
        }
    }
    
    func getConversationEvents() async {
        let params = VGGetConversationEventsParameters(order: .asc, pageSize: 100)
        let eventsPage = try? await client.getConversationEvents(conversationID, parameters: params)
        self.events = eventsPage?.events ?? []
    }
    
    func sendMessage(_ message: String) async {
        _ = try? await client.sendMessageTextEvent(conversationID, text: message)
    }
    
    func generateDisplayText(_ event: VGPersistentConversationEvent) -> (body: String, isUser: Bool) {
        var from = "System"
        
        switch event.kind {
        case .memberJoined:
            let memberJoinedEvent = event as! VGMemberJoinedEvent
            from = memberJoinedEvent.body.user.name
            return ("\(from) joined", false)
        case .memberLeft:
            let memberLeftEvent = event as! VGMemberLeftEvent
            from = memberLeftEvent.body.user.name
            return ("\(from) left", false)
        case .messageText:
            let messageTextEvent = event as! VGMessageTextEvent
            var isUser = false
            
            if let userInfo = messageTextEvent.from as? VGEmbeddedInfo {
                isUser = userInfo.memberId == memberID
                from = isUser ? "" : "\(userInfo.user.name): "
            }
            
            return ("\(from) \(messageTextEvent.body.text)", isUser)
        default:
            return ("", false)
        }
    }
}

extension ChatViewModel: VGChatClientDelegate {
    nonisolated func chatClient(_ client: VGChatClient, didReceiveConversationEvent event: VGConversationEvent) {
        Task { @MainActor in
            self.events.append(event as! VGPersistentConversationEvent)
        }
    }
    
    nonisolated func client(_ client: VGBaseClient, didReceiveSessionErrorWith reason: VGSessionErrorReason) {}
}
