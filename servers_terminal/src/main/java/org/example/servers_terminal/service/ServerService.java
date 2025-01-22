package org.example.servers_terminal.service;

import com.example.KeyValueObject;
import com.example.SpeedAndCapObj;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ServerService {
    private final RestTemplate restTemplate;
    private final AtomicInteger sendMessageCount = new AtomicInteger(0);
    private String url;
    private ScheduledExecutorService scheduler;

    public ServerService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean setServersMany(int noOfQueues, SpeedAndCapObj speedAndCapObj){
        String url = "http://localhost:8084/api/set-server-many?noOfQueues=" + noOfQueues;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<SpeedAndCapObj> request = new HttpEntity<>(speedAndCapObj, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean setServersOneByOne(SpeedAndCapObj speedAndCapObj){
        String url = "http://localhost:8084/api/set-server-one";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<SpeedAndCapObj> request = new HttpEntity<>(speedAndCapObj, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean setHeartBeatIntervals(Map<String, Integer> heartBeatIntervals) {
        try{
            String url = "http://localhost:8084/api/set-heart-beat-intervals";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Integer>> request = new HttpEntity<>(heartBeatIntervals, headers);

            restTemplate.postForEntity(url, request, String.class);

            return true;
        }
        catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                return false;
            }

            throw e;
        }
    }

    public boolean restartServer() {
        String url = "http://localhost:8084/api/restart";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.err.println("Error while restarting server: " + e.getMessage());
            return false;
        }
    }

}
