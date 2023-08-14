import UIKit
import VonageClientSDKVoice

class ViewController: UIViewController {
    
    var connectionStatusLabel = UILabel()
    var callButton = UIButton(type: .roundedRect)
    let client = VGVoiceClient()
    var callID: String?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
        
        connectionStatusLabel.text = "Disconnected"
        connectionStatusLabel.textAlignment = .center
        connectionStatusLabel.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(connectionStatusLabel)

        callButton.setTitle("Call", for: .normal)
        callButton.translatesAutoresizingMaskIntoConstraints = false
        callButton.alpha = 0
        callButton.addTarget(self, action: #selector(callButtonPressed(_:)), for: .touchUpInside)
        view.addSubview(callButton)
        
        NSLayoutConstraint.activate([
            connectionStatusLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            connectionStatusLabel.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            
            callButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            callButton.topAnchor.constraint(equalTo: connectionStatusLabel.bottomAnchor, constant: 24)
        ])
        
        VGVoiceClient.isUsingCallKit = false
        let config = VGClientConfig(region: .US)
        client.setConfig(config)
        
        client.createSession("ALICE_JWT") { error, sessionId in
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                if error == nil {
                    self.callButton.alpha = 1
                    self.connectionStatusLabel.text = "Connected"
                } else {
                    self.connectionStatusLabel.text = error?.localizedDescription
                }
            }
        }
    }
    
    @IBAction func callButtonPressed(_ sender: Any) {
        if callID == nil {
            placeCall()
        } else {
            endCall()
        }
    }
    
    func placeCall() {
        callButton.setTitle("End Call", for: .normal)
        client.serverCall(["to": "PHONE_NUMBER"]) { error, callId in
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                if error == nil {
                    self.callID = callId
                } else {
                    self.callButton.setTitle("Call", for: .normal)
                    self.connectionStatusLabel.text = error?.localizedDescription
                }
            }
        }
    }
    
    func endCall() {
        guard let callID else { return }
        client.hangup(callID) { error in
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                if error == nil {
                    self.callID = nil
                    self.callButton.setTitle("Call", for: .normal)
                }
            }
        }
    }
}
