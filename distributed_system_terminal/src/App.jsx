import React, { useState, useEffect, useRef } from 'react';
import ReactECharts from 'echarts-for-react';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

import useWebSocket from './hooks/useWebSocket';

function App() {
    const [messages, setMessages] = useState([]);
    const [serverId, setServerId] = useState('');
    const eventSourceRef = useRef(null);
    const messagesContainerRef = useRef(null);

    const [messages8083, setMessages8083] = useState([]);
    const messages8083ContainerRef = useRef(null);

    useEffect(() => {
        const sseUrl = 'http://localhost:8084/api/servers/subscribe';
        const es = new EventSource(sseUrl);

        es.addEventListener('update', (event) => {
            setMessages((prev) => [...prev, event.data]);
        });

        es.onerror = (err) => {
            console.error('EventSource (8084) failed:', err);
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

    useEffect(() => {
        const sseUrl8083 = 'http://localhost:8083/consumer-one/emitter/subscribe';
        const es8083 = new EventSource(sseUrl8083);

        es8083.addEventListener('update', (event) => {
            setMessages8083((prev) => [...prev, event.data]);
        });

        es8083.onerror = (err) => {
            console.error('EventSource (8083) failed:', err);
        };

        return () => {
            es8083.close();
        };
    }, []);

    useEffect(() => {
        if (messages8083ContainerRef.current) {
            messages8083ContainerRef.current.scrollTop = messages8083ContainerRef.current.scrollHeight;
        }
    }, [messages8083]);

    const clearMessages = () => {
        setMessages([]);
        setMessages8083([]);
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
                toast.success(`Server with ID: ${serverId} has been successfully crashed.`);
            } else {
                toast.error(`Failed to crash server with ID: ${serverId}.`);
            }

            setServerId('');
        } catch (error) {
            console.error('Error crashing server:', error);
            toast.error('Failed to crash server. Please check the console for details.');
        }
    };

    const initMessages = useWebSocket('/topic/serverInit');
    const detailMessages = useWebSocket('/topic/serverDetails');

    const [serverData, setServerData] = useState({});

    useEffect(() => {
        if (initMessages.length > 0) {
            const latestInit = initMessages[initMessages.length - 1];
            setServerData((prevData) => {
                const newData = { ...prevData };
                Object.entries(latestInit).forEach(([id, details]) => {
                    newData[id] = {
                        load: newData[id]?.load ?? 0,
                        speed: details.speed,
                        capacity: details.capacity,
                    };
                });
                return newData;
            });
        }
    }, [initMessages]);

    useEffect(() => {
        if (detailMessages.length > 0) {
            const latestLoads = detailMessages[detailMessages.length - 1];
            setServerData((prevData) => {
                const newData = { ...prevData };
                Object.entries(latestLoads).forEach(([id, load]) => {
                    newData[id] = {
                        ...newData[id],
                        load: load,
                    };
                });
                return newData;
            });
        }
    }, [detailMessages]);

    const serverIds = Object.keys(serverData);
    const speeds = serverIds.map((id) => serverData[id].speed ?? 0);
    const capacities = serverIds.map((id) => serverData[id].capacity ?? 0);
    const loads = serverIds.map((id) => serverData[id].load ?? 0);

    const capacitySeries = {
        name: 'Capacity',
        type: 'bar',
        data: capacities,
        label: {
            show: true,
            position: 'top',
            formatter: (params) => {
                return params.data === 0 ? 'Crashed' : params.data;
            },
        },
        itemStyle: {
            color: (params) => {
                return params.data == null ? 'red' : '#5470C6';
            },
        },
    };

    const loadSeries = {
        name: 'Load',
        type: 'bar',
        data: loads,
        label: {
            show: true,
            position: 'top',
        },
        itemStyle: {
            color: '#91CC75',
        },
    };

    const chartOptions = {
        title: {
            text: 'Server Load vs Capacity',
            left: 'center',
        },
        tooltip: {
            trigger: 'axis',
        },
        legend: {
            data: ['Capacity', 'Load'],
            top: 30,
        },
        xAxis: {
            type: 'category',
            data: serverIds,
            axisLabel: {
                formatter: function (serverId, index) {
                    const speedVal = speeds[index];
                    return `${serverId}\n(Speed: ${speedVal})`;
                },
            },
        },
        yAxis: {
            type: 'value',
        },
        series: [capacitySeries, loadSeries],
    };

    return (
        <div
            style={{
                margin: '20px',
                display: 'flex',
                flexDirection: 'column',
                gap: '20px'
            }}
        >
            <h1>Server Status Dashboard</h1>

            {/* Chart Section */}
            <div
                style={{
                    border: '1px solid #aaa',
                    padding: '20px'
                }}
            >
                <h3>Server Load vs. Capacity</h3>
                <ReactECharts
                    option={chartOptions}
                    style={{ height: 400, width: '100%' }}
                />
            </div>

            {/* Crash Server Input/Button */}
            <div
                style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '10px'
                }}
            >
                <input
                    type="text"
                    value={serverId}
                    onChange={(e) => setServerId(e.target.value)}
                    placeholder="Enter Server ID"
                    style={{ padding: '5px', width: '200px' }}
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
                <button
                    onClick={clearMessages}
                    style={{
                        padding: '5px 10px',
                        backgroundColor: '#6c757d',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer',
                    }}
                >
                    Clear Messages
                </button>
            </div>

            {/* Messages from 8084 and 8083 side by side */}
            <div
                style={{
                    display: 'flex',
                    gap: '20px'
                }}
            >
                {/* Left column: messages from 8084 */}
                <div
                    ref={messagesContainerRef}
                    style={{
                        flex: 1,
                        border: '1px solid #aaa',
                        padding: '10px',
                        height: '200px',
                        overflowY: 'auto'
                    }}
                >
                    <h3>Message from servers</h3>
                    <ul>
                        {messages.map((msg, index) => (
                            <li key={index}>{msg}</li>
                        ))}
                    </ul>
                </div>

                {/* Right column: messages from 8083 */}
                <div
                    ref={messages8083ContainerRef}
                    style={{
                        flex: 1,
                        border: '1px solid #aaa',
                        padding: '10px',
                        height: '200px',
                        overflowY: 'auto'
                    }}
                >
                    <h3>Message from algorithm schedular</h3>
                    <ul>
                        {messages8083.map((msg, index) => (
                            <li key={index}>{msg}</li>
                        ))}
                    </ul>
                </div>
            </div>

            <ToastContainer />
        </div>
    );

}

export default App;
