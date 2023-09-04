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
                            case .memberJoined:
                                Text("\(chatViewModel.generateFromText(event.from, event.kind).from) joined")
                                    .frame(maxWidth: .infinity, alignment: .center)
                            case.messageText:
                                let messageTextEvent = event as! VGTextMessageEvent
                                let formattedFrom = chatViewModel.generateFromText(messageTextEvent.from, event.kind)
                                Text("\(formattedFrom.from)\(messageTextEvent.body.text)")
                                    .frame(maxWidth: .infinity, alignment: formattedFrom.isUser ? .trailing : .leading)
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
                await chatViewModel.getConversationIfNeeded()
                await chatViewModel.getConversationEvents()
            }
        }
    }
}

@MainActor
final class ChatViewModel: NSObject, ObservableObject {
    private var client: VGChatClient
    private var conversationID: String?
    private var memberID: String?
    
    @Published var events: [VGConversationEvent] = []
    
    init(client: VGChatClient) {
        self.client = client
    }
    
    func getConversationIfNeeded() async {
        client.delegate = self
        guard conversationID == nil else { return }
        await getConversation()
    }
    
    func getConversationEvents() async {
        guard conversationID != nil else { return }
        let eventsPage = try? await client.getConversationEvents(conversationID!, order: .asc, pageSize: 100, cursor: nil, eventFilter: nil)
        self.events = eventsPage!.events
    }
    
    func sendMessage(_ message: String) async {
        guard conversationID != nil else { return }
        _ = try? await client.sendTextMessage(conversationID!, text: message)
    }
    
    func generateFromText(_ eventFrom: VGFrom, _ eventKind: VGEventKind) -> (from: String, isUser: Bool) {
        var from = "System"
        if let userInfo = eventFrom as? VGEmbeddedInfo {
            from = "\(userInfo.user.name): "
            
            // Remove name label for own messages
            if userInfo.memberId == memberID && eventKind == .messageText {
                return ("", true)
            }
        }
        return (from, false)
    }
    
    // TODO: replaced with get conversation by ID
    private func getConversation() async {
        let conversations = try? await client.getConversations().conversations
        let conversation = conversations?.first
        conversationID = conversation?.id
        memberID = conversation?.memberId
    }
}

extension ChatViewModel: VGChatClientDelegate {
    nonisolated func chatClient(_ client: VGChatClient, didReceiveConversationEvent event: VGConversationEvent) {
        Task { @MainActor in
            self.events.append(event)
        }
    }
    
    nonisolated func client(_ client: VGBaseClient, didReceiveSessionErrorWith reason: VGSessionErrorReason) {}
}
