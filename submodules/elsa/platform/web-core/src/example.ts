import {SubsequentSubscriptionsManager, WebSocketFacade} from "./websocket";
import $ from 'jquery'

const webSocketFacade = new WebSocketFacade({
    connectUrl: "ws://localhost:8080/websocket"
})

const subsequentSubscriptionsManager = new SubsequentSubscriptionsManager(webSocketFacade)

async function subscribe(){
    await subsequentSubscriptionsManager.subscribe({
        remoting: "ldocs-core",
        group: "common",
        subscription: "document-change",
        parameter: {
            directoryId: ($("#parameter")[0] as any).value
        },
        handler: (event) =>{
            $("#greetings").append(`<tr><td>changed ${event.documentId}</td></tr>`);
        }
    })
    //@ts-ignore
    $("#web-socket-status")[0].value = "Connected"
    //@ts-ignore
    $("#subscription-status")[0].value = "Subscribed"
}

async function unsubscribe(){
    await subsequentSubscriptionsManager.unsubscribe("ldocs-core", "common", "document-change")
    //@ts-ignore
    $("#subscription-status")[0].value = "Unsubscribed"
}

async function init(){
    //@ts-ignore
    $("#web-socket-status")[0].value = "Not connected"
    $("#subscribe").click(subscribe)
    $("#unsubscribe").click(unsubscribe)
}

$( document ).ready(init)
