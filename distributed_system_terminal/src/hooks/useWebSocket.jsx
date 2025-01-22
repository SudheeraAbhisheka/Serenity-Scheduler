import { useEffect, useRef, useState } from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";

const useWebSocket = (topicUrl) => {
    const [messages, setMessages] = useState([]);
    const stompClientRef = useRef(null);

    useEffect(() => {
        const socket = new SockJS("http://localhost:8084/websocket-server");
        const stompClient = new Client({
            webSocketFactory: () => socket,
            onConnect: () => {
                console.log("Connected to WebSocket");
                stompClient.subscribe(topicUrl, (msg) => {
                    if (msg.body) {
                        setMessages((prev) => [...prev, JSON.parse(msg.body)]);
                    }
                });
            },
            onDisconnect: () => {
                console.log("Disconnected from WebSocket");
            },
            onStompError: (frame) => {
                console.error("Broker reported error:", frame.headers["message"]);
                console.error("Additional details:", frame.body);
            },
        });

        stompClientRef.current = stompClient;
        stompClient.activate();

        return () => {
            if (stompClientRef.current) {
                stompClientRef.current.deactivate();
            }
        };
    }, [topicUrl]);

    return messages;
};

export default useWebSocket;
