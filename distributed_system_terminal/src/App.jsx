import React, { useState, useEffect, useRef } from 'react';

function App() {
    const [serverId, setServerId] = useState('');
    const [messages, setMessages] = useState([]);
    const eventSourceRef = useRef(null);

    const connectToServer = () => {
        // If there's an existing EventSource connection, close it
        if (eventSourceRef.current) {
            eventSourceRef.current.close();
        }

        // Construct the SSE endpoint URL
        const sseUrl = `http://localhost:8084/api/servers/${serverId}/subscribe`;

        // Create a new EventSource instance
        const es = new EventSource(sseUrl);

        es.addEventListener('update', (event) => {
            // Incoming event data is in event.data
            const newMessage = event.data;
            // Use functional updates to preserve current messages
            setMessages((prev) => [...prev, newMessage]);
        });

        es.onerror = (err) => {
            console.error('EventSource failed:', err);
            // Optionally handle reconnection logic here
        };

        eventSourceRef.current = es;
    };

    useEffect(() => {
        // Cleanup on unmount
        return () => {
            if (eventSourceRef.current) {
                eventSourceRef.current.close();
            }
        };
    }, []);

    return (
        <div style={{ margin: '20px' }}>
            <h1>Server Status Dashboard</h1>

            <div style={{ marginBottom: '10px' }}>
                <input
                    type="text"
                    placeholder="Enter serverId"
                    value={serverId}
                    onChange={(e) => setServerId(e.target.value)}
                    style={{ marginRight: '10px' }}
                />
                <button onClick={connectToServer}>Connect</button>
            </div>

            <div style={{ border: '1px solid #aaa', padding: '10px', height: '300px', overflowY: 'auto' }}>
                <h3>Messages from Server {serverId}</h3>
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
