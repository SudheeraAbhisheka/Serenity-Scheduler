import React, { useState, useEffect, useRef } from 'react';
import ReactECharts from 'echarts-for-react';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

import useWebSocket from './hooks/useWebSocket';

function App() {
    // ------------------
    // SSE / WebSocket states
    // ------------------
    const [messages, setMessages] = useState([]);
    const [serverId, setServerId] = useState('');
    const messagesContainerRef = useRef(null);

    const [messages8083, setMessages8083] = useState([]);
    const messages8083ContainerRef = useRef(null);

    // For server load/capacity
    const messages8084 = useWebSocket('/topic/servers');
    const initMessages = useWebSocket('/topic/serverInit');
    const detailMessages = useWebSocket('/topic/serverDetails');
    const [serverData, setServerData] = useState({});

    // For task completion
    const [totalTasks, setTotalTasks] = useState(0);
    const [taskCompletion, setTaskCompletion] = useState({});
    const totalTasksMessages = useWebSocket('/topic/taskCompletionTotalTasks');
    const completionMessages = useWebSocket('/topic/taskCompletion');

    useEffect(() => {
        if (messagesContainerRef.current) {
            messagesContainerRef.current.scrollTop = messagesContainerRef.current.scrollHeight;
        }
    }, [messages]);

    // SSE for 8083
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
        if (messages8084.length > 0) {
            setMessages(messages8084);
        }
    }, [messages8084]);

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
            const response = await fetch('http://localhost:8084/api/crash-a-server', {
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

    // ------------------
    // Process initMessages -> serverData
    // ------------------
    useEffect(() => {
        if (initMessages.length > 0) {
            const latestInit = initMessages[initMessages.length - 1];
            setServerData((prevData) => {
                const newData = { ...prevData };
                Object.entries(latestInit).forEach(([id, details]) => {
                    newData[id] = {
                        // preserve existing load if already set
                        load: newData[id]?.load ?? 0,
                        speed: details.speed,
                        capacity: details.capacity,
                    };
                });
                return newData;
            });
        }
    }, [initMessages]);

    // ------------------
    // Process detailMessages -> serverData
    // ------------------
    useEffect(() => {
        if (detailMessages.length > 0) {
            // The last message is a map, e.g.: { server1: 30.0, server2: 45.5, ... }
            const latestLoads = detailMessages[detailMessages.length - 1];
            setServerData((prevData) => {
                const newData = { ...prevData };
                Object.entries(latestLoads).forEach(([id, load]) => {
                    newData[id] = {
                        // carry over existing speed/capacity (if any)
                        ...newData[id],
                        load: load,
                    };
                });
                return newData;
            });
        }
    }, [detailMessages]);

    // ------------------
    // Chart data
    // ------------------
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
                // If capacity is 0, you could mark it as "Crashed"
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
            top: 20,
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

    // ------------------
    // Task Completion data
    // ------------------
    useEffect(() => {
        if (totalTasksMessages.length > 0) {
            // last element in the array is the latest message
            const latestTotal = totalTasksMessages[totalTasksMessages.length - 1];
            setTotalTasks(latestTotal);
        }
    }, [totalTasksMessages]);

    useEffect(() => {
        if (completionMessages.length > 0) {
            const latestCompletion = completionMessages[completionMessages.length - 1];
            setTaskCompletion(latestCompletion);
        }
    }, [completionMessages]);

    const totalCompletedTasks = Object.values(taskCompletion).reduce((sum, val) => sum + val, 0);

    // ------------------
    // (NEW) Database Tasks Table
    // ------------------
    const [tasks, setTasks] = useState([]);
    const [isTableExpanded, setIsTableExpanded] = useState(false);

    const fetchTasks = () => {
        fetch('http://localhost:8084/api/get-tasks')
            .then((response) => response.json())
            .then((data) => {
                setTasks(data);
            })
            .catch((error) => {
                console.error('Error fetching tasks:', error);
            });
    };

    const clearAllTasks = () => {
        fetch('http://localhost:8084/api/delete-tasks', { method: 'DELETE' })
            .then(() => {
                // After successful delete, clear the tasks in state
                setTasks([]);
            })
            .catch((error) => {
                console.error('Error deleting tasks:', error);
            });
    };

    // Toggle the table; on expand, refresh tasks
    const toggleTable = () => {
        setIsTableExpanded((prev) => !prev);
        // If we are about to expand, fetch fresh tasks
        if (!isTableExpanded) {
            fetchTasks();
        }
    };

    return (
        <div
            style={{
                margin: '20px',
                width: '1480px',
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

            {/* Task Completion Section */}
            <div style={{ marginTop: '20px', border: '1px solid #aaa', padding: '10px' }}>
                <h3>Task Completion Status</h3>
                {Object.keys(taskCompletion).length > 0 ? (
                    <div>
                        <ul>
                            {Object.entries(taskCompletion).map(([id, completed]) => (
                                <li key={id}>
                                    Server <strong>{id}</strong>: {completed} tasks completed
                                </li>
                            ))}
                        </ul>
                        <p>
                            <strong>Total Completed: </strong>
                            {totalCompletedTasks} / {totalTasks}
                        </p>
                    </div>
                ) : (
                    <p>No task completion data yet.</p>
                )}
            </div>

            {/* Crash Server Input/Button */}
            <div
                style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '10px',
                    margin: '10px 0',
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
                            <li key={index}>{msg.text}</li>
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

            {/* --------------- NEW: Collapsible Database Table --------------- */}
            <div
                style={{
                    marginTop: '20px',
                    border: '1px solid #aaa',
                    padding: '10px'
                }}
            >
                {/* Toggle arrow */}
                <div
                    onClick={toggleTable}
                    style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        cursor: 'pointer',
                        userSelect: 'none'
                    }}
                >
                    {/* Simple arrow indicator */}
                    <span style={{ marginRight: '8px', fontWeight: 'bold' }}>
                        {isTableExpanded ? '▼' : '►'}
                    </span>
                    <h3 style={{ margin: 0 }}>Tasks Table</h3>
                </div>

                {/* Conditionally render table only if expanded */}
                {isTableExpanded && (
                    <div style={{ marginTop: '10px' }}>
                        <button
                            onClick={clearAllTasks}
                            style={{
                                padding: '5px 10px',
                                backgroundColor: '#DC3545',
                                color: 'white',
                                border: 'none',
                                borderRadius: '4px',
                                cursor: 'pointer',
                                marginRight: '10px'
                            }}
                        >
                            Clear All Tasks
                        </button>
                        <button
                            onClick={fetchTasks}
                            style={{
                                padding: '5px 10px',
                                backgroundColor: '#007BFF',
                                color: 'white',
                                border: 'none',
                                borderRadius: '4px',
                                cursor: 'pointer'
                            }}
                        >
                            Refresh
                        </button>

                        <table
                            style={{
                                borderCollapse: 'collapse',
                                width: '100%',
                                border: '1px solid black',
                                marginTop: '20px'
                            }}
                        >
                            <thead>
                            <tr style={{ borderBottom: '2px solid black' }}>
                                <th style={{ border: '1px solid black', padding: '8px' }}>Key</th>
                                <th style={{ border: '1px solid black', padding: '8px' }}>Value</th>
                                <th style={{ border: '1px solid black', padding: '8px' }}>Weight</th>
                                <th style={{ border: '1px solid black', padding: '8px' }}>Generated At</th>
                                <th style={{ border: '1px solid black', padding: '8px' }}>Executed</th>
                                <th style={{ border: '1px solid black', padding: '8px' }}>Priority</th>
                                <th style={{ border: '1px solid black', padding: '8px' }}>Start of Process</th>
                                <th style={{ border: '1px solid black', padding: '8px' }}>End of Process</th>
                                <th style={{ border: '1px solid black', padding: '8px' }}>Server Key</th>
                            </tr>
                            </thead>
                            <tbody>
                            {tasks.map((task) => (
                                <tr key={task.key}>
                                    <td style={{ border: '1px solid black', padding: '8px' }}>{task.key}</td>
                                    <td style={{ border: '1px solid black', padding: '8px' }}>{task.value}</td>
                                    <td style={{ border: '1px solid black', padding: '8px' }}>{task.weight}</td>
                                    <td style={{ border: '1px solid black', padding: '8px' }}>{task.generatedAt}</td>
                                    <td style={{ border: '1px solid black', padding: '8px' }}>
                                        {task.executed ? 'Yes' : 'No'}
                                    </td>
                                    <td style={{ border: '1px solid black', padding: '8px' }}>{task.priority}</td>
                                    <td style={{ border: '1px solid black', padding: '8px' }}>{task.startOfProcessAt}</td>
                                    <td style={{ border: '1px solid black', padding: '8px' }}>{task.endOfProcessAt}</td>
                                    <td style={{ border: '1px solid black', padding: '8px' }}>{task.serverKey}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            <ToastContainer />
        </div>
    );
}

export default App;
