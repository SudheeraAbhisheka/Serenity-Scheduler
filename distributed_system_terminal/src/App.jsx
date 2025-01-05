import React, { useState, useEffect, useRef } from 'react';

function App() {
    const [messages, setMessages] = useState([]);
    const [serverLoads, setServerLoads] = useState({});
    const [lastUpdated, setLastUpdated] = useState(null);
    const eventSourceRef = useRef(null);
    const messagesContainerRef = useRef(null);

    useEffect(() => {
        const sseUrl = 'http://localhost:8084/api/servers/subscribe';
        const es = new EventSource(sseUrl);

        es.addEventListener('update', (event) => {
            const newMessage = JSON.parse(event.data); // Parse the incoming JSON data
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

    const fetchServerLoads = async () => {
        try {
            const response = await fetch('http://localhost:8084/api/get-servers');
            if (!response.ok) {
                throw new Error('Failed to fetch server loads');
            }
            const data = await response.json();
            setServerLoads(data);
            setLastUpdated(new Date().toLocaleTimeString());
        } catch (error) {
            console.error('Error fetching server loads:', error);
        }
    };

    const clearMessages = () => {
        setMessages([]);
    };

    return (
        <div style={{ margin: '20px' }}>
            <h1>Server Status Dashboard</h1>
            <button
                onClick={clearMessages}
                style={{
                    marginBottom: '10px',
                    padding: '5px 10px',
                    backgroundColor: '#007BFF',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer',
                }}
            >
                Clear Terminal
            </button>
            <button
                onClick={fetchServerLoads}
                style={{
                    marginBottom: '10px',
                    marginLeft: '10px',
                    padding: '5px 10px',
                    backgroundColor: '#28A745',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer',
                }}
            >
                Update Server Loads
            </button>
            {lastUpdated && (
                <div style={{ marginBottom: '10px', fontStyle: 'italic' }}>
                    Last Updated: {lastUpdated}
                </div>
            )}
            <div
                ref={messagesContainerRef}
                style={{
                    border: '1px solid #aaa',
                    padding: '10px',
                    height: '300px',
                    overflowY: 'auto',
                }}
            >
                <h3>All Messages</h3>
                {messages.length === 0 && <div>No messages yet.</div>}
                {messages.length > 0 && (
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <thead>
                        <tr>
                            <th style={{ border: '1px solid #ddd', padding: '8px' }}>Key</th>
                            <th style={{ border: '1px solid #ddd', padding: '8px' }}>Value</th>
                            <th style={{ border: '1px solid #ddd', padding: '8px' }}>Weight</th>
                            <th style={{ border: '1px solid #ddd', padding: '8px' }}>Generated At</th>
                            <th style={{ border: '1px solid #ddd', padding: '8px' }}>Executed</th>
                            <th style={{ border: '1px solid #ddd', padding: '8px' }}>Priority</th>
                            <th style={{ border: '1px solid #ddd', padding: '8px' }}>Processing Time (s)</th>
                            <th style={{ border: '1px solid #ddd', padding: '8px' }}>Total Wait (s)</th>
                            <th style={{ border: '1px solid #ddd', padding: '8px' }}>Server Key</th>
                        </tr>
                        </thead>
                        <tbody>
                        {messages.map((msg, idx) => {
                            const processingTime =
                                (msg.endOfProcessAt - msg.startOfProcessAt) / 1000 || 0; // Calculate in seconds
                            const totalWait =
                                (msg.startOfProcessAt - msg.generatedAt) / 1000 || 0; // Calculate Total Wait in seconds
                            return (
                                <tr key={idx}>
                                    <td style={{ border: '1px solid #ddd', padding: '8px' }}>{msg.key}</td>
                                    <td style={{ border: '1px solid #ddd', padding: '8px' }}>{msg.value}</td>
                                    <td style={{ border: '1px solid #ddd', padding: '8px' }}>{msg.weight}</td>
                                    <td style={{ border: '1px solid #ddd', padding: '8px' }}>{msg.generatedAt}</td>
                                    <td style={{ border: '1px solid #ddd', padding: '8px' }}>{String(msg.executed)}</td>
                                    <td style={{ border: '1px solid #ddd', padding: '8px' }}>{msg.priority}</td>
                                    <td style={{ border: '1px solid #ddd', padding: '8px' }}>{processingTime.toFixed(3)}</td>
                                    <td style={{ border: '1px solid #ddd', padding: '8px' }}>{totalWait.toFixed(3)}</td>
                                    <td style={{ border: '1px solid #ddd', padding: '8px' }}>{msg.serverKey}</td>
                                </tr>
                            );
                        })}
                        </tbody>
                    </table>
                )}
            </div>
            <div
                style={{
                    marginTop: '20px',
                    border: '1px solid #aaa',
                    padding: '10px',
                }}
            >
                <h3>Server Loads</h3>
                {Object.keys(serverLoads).length === 0 ? (
                    <div>No server loads available yet.</div>
                ) : (
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <thead>
                        <tr>
                            <th style={{ border: '1px solid #ddd', padding: '8px' }}>Server ID</th>
                            <th style={{ border: '1px solid #ddd', padding: '8px' }}>Load</th>
                        </tr>
                        </thead>
                        <tbody>
                        {Object.entries(serverLoads).map(([serverId, load]) => (
                            <tr key={serverId}>
                                <td style={{ border: '1px solid #ddd', padding: '8px' }}>{serverId}</td>
                                <td style={{ border: '1px solid #ddd', padding: '8px' }}>{load}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                )}
            </div>
        </div>
    );
}

export default App;
