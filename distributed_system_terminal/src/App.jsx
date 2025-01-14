import React, { useState, useEffect, useRef } from 'react';

function App() {
    const [messages, setMessages] = useState([]);
    const [serverLoads, setServerLoads] = useState({});
    const [lastUpdated, setLastUpdated] = useState(null);
    const [algorithmDetails, setAlgorithmDetails] = useState({ algorithm: '', messageBroker: '' });
    const [serverId, setServerId] = useState(''); // New state for serverId input
    const eventSourceRef = useRef(null);
    const messagesContainerRef = useRef(null);

    useEffect(() => {
        const sseUrl = 'http://localhost:8084/api/servers/subscribe';
        const es = new EventSource(sseUrl);

        es.addEventListener('update', (event) => {
            setMessages((prev) => [...prev, event.data]);
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
                throw new Error('Failed to fetch servers');
            }
            const data = await response.json();
            setServerLoads(data);
            setLastUpdated(new Date().toLocaleTimeString());
        } catch (error) {
            console.error('Error fetching servers:', error);
        }
    };

    const fetchAlgorithmDetails = async () => {
        try {
            const response = await fetch('http://localhost:8083/consumer-one/get-server1-details');
            if (!response.ok) {
                throw new Error('Failed to fetch algorithm details');
            }
            const data = await response.json();
            setAlgorithmDetails(data);
        } catch (error) {
            console.error('Error fetching algorithm details:', error);
        }
    };

    const clearMessages = () => {
        setMessages([]);
    };

    const clearServerSpeeds = () => {
        setServerLoads({});
    };

    const crashAServer = async () => {
        try {
            const response = await fetch('http://localhost:8084/api/crash-server', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(serverId),
            });

            if (!response.ok) {
                throw new Error('Failed to crash server');
            }

            const result = await response.json();
            if (result) {
                alert(`Server with ID: ${serverId} has been successfully crashed.`);
            } else {
                alert(`Failed to crash server with ID: ${serverId}.`);
            }

            setServerId(''); // Clear input after call
        } catch (error) {
            console.error('Error crashing server:', error);
            alert('Failed to crash server. Please check the console for details.');
        }
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
                onClick={clearServerSpeeds}
                style={{
                    marginBottom: '10px',
                    marginLeft: '10px',
                    padding: '5px 10px',
                    backgroundColor: '#FF5733',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer',
                }}
            >
                Clear Server Speeds
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
                Update Servers
            </button>

            <button
                onClick={fetchAlgorithmDetails}
                style={{
                    marginBottom: '10px',
                    marginLeft: '10px',
                    padding: '5px 10px',
                    backgroundColor: '#17A2B8',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer',
                }}
            >
                Get Algorithm Details
            </button>

            <div style={{ marginTop: '20px' }}>
                <input
                    type="text"
                    value={serverId}
                    onChange={(e) => setServerId(e.target.value)}
                    placeholder="Enter Server ID"
                    style={{ padding: '5px', marginRight: '10px', width: '200px' }}
                />
                <button
                    onClick={crashAServer}
                    style={{
                        padding: '5px 10px',
                        backgroundColor: '#DC3545',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer',
                    }}
                >
                    Crash Server
                </button>
            </div>

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
                <ul>
                    {messages.map((msg, index) => (
                        <li key={index}>{msg}</li>
                    ))}
                </ul>
            </div>
            <div
                style={{
                    marginTop: '20px',
                    border: '1px solid #aaa',
                    padding: '10px',
                    height: '200px',
                    overflowY: 'auto',
                }}
            >
                <h3>Server Speeds</h3>
                {Object.keys(serverLoads).length === 0 ? (
                    <div>No servers available yet.</div>
                ) : (
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <thead>
                        <tr>
                            <th style={{ border: '1px solid #ddd', padding: '8px' }}>Server ID</th>
                            <th style={{ border: '1px solid #ddd', padding: '8px' }}>Speed</th>
                        </tr>
                        </thead>
                        <tbody>
                        {Object.entries(serverLoads).map(([serverId, speed]) => (
                            <tr key={serverId}>
                                <td style={{ border: '1px solid #ddd', padding: '8px' }}>{serverId}</td>
                                <td style={{ border: '1px solid #ddd', padding: '8px' }}>{speed}</td>
                            </tr>
                        ))}
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
                <h3>Algorithm and Message Broker Details</h3>
                <p><strong>Algorithm:</strong> {algorithmDetails.algorithm}</p>
                <p><strong>Message Broker:</strong> {algorithmDetails.messageBroker}</p>
            </div>
        </div>
    );
}

export default App;