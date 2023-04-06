import UIKit
import VonageClientSDKVoice

class ViewController: UIViewController {
    
    let connectionStatusLabel = UILabel()
    let client = VGVoiceClient()
    var callID: String?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
        
        connectionStatusLabel.text = "Disconnected"
        connectionStatusLabel.textAlignment = .center
        connectionStatusLabel.translatesAutoresizingMaskIntoConstraints = false
        
        view.addSubview(connectionStatusLabel)
        
        NSLayoutConstraint.activate([
            connectionStatusLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            connectionStatusLabel.centerYAnchor.constraint(equalTo: view.centerYAnchor)
        ])
        
        let config = VGClientConfig(region: .US)
        client.setConfig(config)
        client.delegate = self
        
        client.createSession("ALICE_JWT") { error, sessionId in
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                if error == nil {
                    self.connectionStatusLabel.text = "Connected"
                } else {
                    self.connectionStatusLabel.text = error?.localizedDescription
                }
            }
        }
    }
    
    func displayIncomingCallAlert(callID: String, caller: String) {
        let alert = UIAlertController(title: "Incoming call from", message: caller, preferredStyle: .alert)
        
        alert.addAction(UIAlertAction(title: "Answer", style: .default, handler: { _ in
            self.client.answer(callID) { error in
                if error == nil {
                    self.callID = callID
                }
            }
        }))
        
        alert.addAction(UIAlertAction(title: "Reject", style: .destructive, handler: { _ in
            self.client.reject(callID) { error in
                if let error {
                    self.connectionStatusLabel.text = error.localizedDescription
                }
            }
        }))
        
        self.present(alert, animated: true, completion: nil)
    }
}

extension ViewController: VGVoiceClientDelegate {
    
    func voiceClient(_ client: VGVoiceClient, didReceiveInviteForCall callId: String, from caller: String, withChannelType type: String) {
        DispatchQueue.main.async { [weak self] in
            self?.displayIncomingCallAlert(callID: callId, caller: caller)
        }
    }
    
    func voiceClient(_ client: VGVoiceClient, didReceiveInviteCancelForCall callId: String, with reason: VGVoiceInviteCancelReasonType) {
        DispatchQueue.main.async { [weak self] in
            self?.dismiss(animated: true)
        }
    }
    
    func voiceClient(_ client: VGVoiceClient, didReceiveHangupForCall callId: String, withQuality callQuality: VGRTCQuality) {
        DispatchQueue.main.async { [weak self] in
            self?.callID = nil
            self?.connectionStatusLabel.text = "Call Ended"
        }
    }
    
    func client(_ client: VGBaseClient, didReceiveSessionErrorWith reason: VGSessionErrorReason) {
        let reasonString: String!
        
        switch reason {
        case .EXPIRED_TOKEN:
            reasonString = "Expired Token"
        case .PING_TIMEOUT, .TRANSPORT_CLOSED:
            reasonString = "Network Error"
        @unknown default:
            reasonString = "Unknown"
        }
        
        DispatchQueue.main.async { [weak self] in
            self?.connectionStatusLabel.text = reasonString
        }
    }
}
