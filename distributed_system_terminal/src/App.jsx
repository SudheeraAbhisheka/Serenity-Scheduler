import React, { useState, useEffect, useRef } from 'react';

function App() {
    const [messages, setMessages] = useState([]);
    const eventSourceRef = useRef(null);
    const messagesContainerRef = useRef(null);

    useEffect(() => {
        const sseUrl = 'http://localhost:8084/api/servers/subscribe';
        const es = new EventSource(sseUrl);

        es.addEventListener('update', (event) => {
            const newMessage = event.data;
            setMessages((prev) => [...prev, newMessage]);
        });

        es.onerror = (err) => {
            console.error('EventSource failed:', err);
        };

        eventSourceRef.current = es;

        return () => {
            if (eventSourceRef.current) {
                eventSourceRef.current.close();
            }
        };
    }, []);

    useEffect(() => {
        if (messagesContainerRef.current) {
            messagesContainerRef.current.scrollTop = messagesContainerRef.current.scrollHeight;
        }
    }, [messages]);

    return (
        <div style={{ margin: '20px' }}>
            <h1>Server Status Dashboard</h1>
            <div
                ref={messagesContainerRef}
                style={{
                    border: '1px solid #aaa',
                    padding: '10px',
                    height: '300px',
                    overflowY: 'auto'
                }}
            >
                <h3>All Messages</h3>
                {messages.length === 0 && <div>No messages yet.</div>}
                {messages.map((msg, idx) => (
                    <div key={idx} style={{ marginBottom: '5px', fontFamily: 'monospace' }}>
                        {msg}
                    </div>
                ))}
            </div>
        </div>
    );
}

export default App;
