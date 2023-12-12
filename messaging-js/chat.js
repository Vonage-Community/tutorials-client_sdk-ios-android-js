const ALICE_JWT = "";
const BOB_JWT = "";
const CONVERSATION_ID = "";

const aliceLoginBtn = document.getElementById("alice-login");
const bobLoginBtn = document.getElementById("bob-login");

const messageTextarea = document.getElementById("message-textarea");
const messageFeed = document.getElementById("message-feed");
const sendButton = document.getElementById("send");

let userToken;
let myMember;

function formatMessage(sender, message) {
    const rawDate = new Date(Date.parse(message.timestamp));
    const options = {
        weekday: "long",
        year: "numeric",
        month: "long",
        day: "numeric",
        hour: "numeric",
        minute: "numeric",
        second: "numeric",
    };
    const formattedDate = rawDate.toLocaleDateString(undefined, options);
    let text = "";

    if (message.from.memberId !== myMember.id) {
        text = `<span style="color:red">${sender.userName.replace(/</g,"&lt;")} (${formattedDate}): <b>${message.body.text.replace(/</g,"&lt;")}</b></span>`;
    } else {
        text = `me (${formattedDate}): <b>${message.body.text.replace(/</g,"&lt;")}</b>`;
    }
    return text + "<br />";
}

function handleEvent(event) {
    let formattedMessage;
    switch (event.kind) {
        case "member:invited":
            formattedMessage = `${event.body.user.name} was invited.<br/>`;
            break;
        case "member:joined":
            formattedMessage = `${event.body.user.name} joined.<br/>`;
            break;
        case "member:left":
            formattedMessage = `${event.body.user.name} left.<br/>`;
            break;
        case "message:text":
            const sender = {
                displayName: event.from.displayName,
                memberId: event.from.memberId,
                userName: event.from.user.name,
                userId: event.from.user.id,
            };
            formattedMessage = formatMessage(sender, event);
            break;
    }
    messageFeed.innerHTML = messageFeed.innerHTML + formattedMessage;
}

async function run() {
    const client = new vonageClientSDK.VonageClient();
    try {
        await client.createSession(userToken);

        // Get my Member information
        myMember = await client.getConversationMember(CONVERSATION_ID, "me");

        document.getElementById("messages").style.display = "block";
        document.getElementById("login").style.display = "none";

        // Load events that happened before the page loaded
        const params = {
            order: "asc",
            pageSize: 100,
        };
        const eventsPage = await client.getConversationEvents(CONVERSATION_ID, params);
        eventsPage.events.forEach((event) => handleEvent(event));
    } catch (error) {
        console.error("Error: ", error);
        return;
    }

    client.on("conversationEvent", (event) => {
        handleEvent(event);
    });

  // Listen for clicks on the submit button and send the existing text value
  sendButton.addEventListener("click", () => {
    client
      .sendMessageTextEvent(CONVERSATION_ID, messageTextarea.value)
      .then((timestamp) => {
        console.log("Successfully sent text message at ", timestamp);
        messageTextarea.value = "";
      })
      .catch((error) => {
        console.error("Error sending text message: ", error);
      });
  });
}

aliceLoginBtn.addEventListener("click", () => {
    userToken = ALICE_JWT;
    run();
});
  
bobLoginBtn.addEventListener("click", () => {
    userToken = BOB_JWT;
    run();
});
  
  