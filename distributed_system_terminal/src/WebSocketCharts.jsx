import React, { useEffect, useState } from "react";
import { Stomp } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import ReactECharts from "echarts-for-react";

const WebSocketCharts = () => {
    const [data, setData] = useState({});

    useEffect(() => {
        const socket = new SockJS("http://localhost:8084/websocket-server");
        const stompClient = Stomp.over(socket);

        stompClient.connect({}, () => {
            stompClient.subscribe("/topic/serverDetails", (message) => {
                const newData = JSON.parse(message.body);
                setData((prev) => ({ ...prev, ...newData }));
            });
        });

        return () => {
            if (stompClient) stompClient.disconnect();
        };
    }, []);

    const generateCharts = () => {
        return Object.entries(data).map(([serverId, metrics], index) => {
            const chartOptions = {
                title: {
                    text: `Server ${serverId}`,
                },
                tooltip: {},
                legend: {
                    data: ["Metrics"],
                },
                xAxis: {
                    type: "category",
                    data: ["Speed", "Capacity", "Load"],
                },
                yAxis: {
                    type: "value",
                },
                series: [
                    {
                        name: "Metrics",
                        type: "bar",
                        data: [metrics.speed, metrics.capacity, metrics.load],
                    },
                ],
            };

            return (
                <div key={index} style={{ marginBottom: "20px" }}>
                    <ReactECharts option={chartOptions} />
                </div>
            );
        });
    };

    return <div>{generateCharts()}</div>;
};

export default WebSocketCharts;
