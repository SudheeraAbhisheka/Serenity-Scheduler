package org.example.server1.service;

import com.example.TaskObject;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
public class PriorityBasedScheduling {
    private final CompleteFetchAlgorithm completeFetchAlgorithm;
    private final LoadBalancingAlgorithm loadBalancingAlgorithm;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    @Setter
    private BlockingQueue<TaskObject> blockingQueuePriorityS;
    private boolean waitingThreads = false;
    private long arrivedTime;
    volatile long waitingTime1 = 500;
    @Setter
    volatile long waitingTime2 = 10000;
    long maxAge = 0;
    private static final String PRIORITY_COMPLETE_FETCH = "priority-complete-fetch";
    private static final String PRIORITY_LOAD_BALANCING = "priority-load-balancing";

    public PriorityBasedScheduling(CompleteFetchAlgorithm schedulingAlgorithm, LoadBalancingAlgorithm loadBalancingAlgorithm){
        this.completeFetchAlgorithm = schedulingAlgorithm;
        this.loadBalancingAlgorithm = loadBalancingAlgorithm;
    }

    public void priorityBasedScheduling(LinkedHashMap<Integer, Long> thresholdTime, String completeFetchOrLB) {
        final Object lock = new Object();
        int UNDEFINED = 0;

        ConcurrentHashMap<Integer, Queue<ArrivedTimeObject>> queuePriorityX = new ConcurrentHashMap<>();
        if(completeFetchOrLB.equals(PRIORITY_COMPLETE_FETCH)){
            completeFetchAlgorithm.setDynamicBlockingQueue(new LinkedBlockingQueue<>(1));
        }
        else if(completeFetchOrLB.equals(PRIORITY_LOAD_BALANCING)){
            loadBalancingAlgorithm.setWlbQueue(new LinkedBlockingQueue<>());
        }

        queuePriorityX.put(UNDEFINED, new ConcurrentLinkedQueue<>());
        for(Map.Entry<Integer, Long> entry : thresholdTime.entrySet()) {
            int priority = entry.getKey();
            long age = entry.getValue();

            queuePriorityX.put(priority, new ConcurrentLinkedQueue<>());
            if(age > maxAge){
                maxAge = age;
            }
        }
        thresholdTime.put(UNDEFINED, maxAge);

        executorService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                TaskObject task = null;
                int priority = 0;

                try {
                    task = blockingQueuePriorityS.take();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                priority = task.getPriority();

                arrivedTime = System.currentTimeMillis();
                ArrivedTimeObject arrivedTimeObject = new ArrivedTimeObject(arrivedTime, task);

                if(queuePriorityX.containsKey(priority)){
                    queuePriorityX.get(priority).add(arrivedTimeObject);
                }
                else{
//                    queuePriorityX.get(UNDEFINED).add(arrivedTimeObject);
                }
            }
        });

        executorService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                long oldest = 0;
                int priorityOfOldest = 0;
                TaskObject oldestTask;

                try {
                    for (Map.Entry<Integer, Queue<ArrivedTimeObject>> entry : queuePriorityX.entrySet()) {
                        Integer priority = entry.getKey();
                        Queue<ArrivedTimeObject> priorityQueue = entry.getValue();

                        if (!priorityQueue.isEmpty()) {
                            long age = System.currentTimeMillis() - priorityQueue.peek().getArrivedTime();

                            if (age > thresholdTime.get(priority)) {
                                if(age > oldest){
                                    oldest = age;
                                    priorityOfOldest = priority;
                                }
                            }

                        }
                    }

                    if(oldest != 0){
                        oldestTask = queuePriorityX.get(priorityOfOldest).poll().getTaskObject();

                        if(completeFetchOrLB.equals(PRIORITY_COMPLETE_FETCH)){
                            completeFetchAlgorithm.getDynamicBlockingQueue().put(oldestTask);
                        }
                        else if(completeFetchOrLB.equals(PRIORITY_LOAD_BALANCING)){
                            loadBalancingAlgorithm.getWlbQueue().put(oldestTask);
                        }
                    }
                    else {
                        boolean priorityQueuesAreEmpty = true;

                        for(Queue<ArrivedTimeObject> priorityQueue : queuePriorityX.values()){
                            if(!priorityQueue.isEmpty()){
                                TaskObject task = priorityQueue.poll().getTaskObject();

                                if(completeFetchOrLB.equals(PRIORITY_COMPLETE_FETCH)){
                                    completeFetchAlgorithm.getDynamicBlockingQueue().put(task);
                                }
                                else if(completeFetchOrLB.equals(PRIORITY_LOAD_BALANCING)){
                                    loadBalancingAlgorithm.getWlbQueue().put(task);
                                }
                                priorityQueuesAreEmpty = false;

                                break;
                            }
                        }

                        if(priorityQueuesAreEmpty){
                            synchronized (lock) {
                                try {
                                    waitingThreads = true;
                                    lock.wait();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                } finally {
                                    waitingThreads = false;
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        executorService.submit(() -> {
            long lastlyUnlockedFor = Long.MAX_VALUE;
            long indicator = 0;

            while(!Thread.currentThread().isInterrupted()){
                long currentTime = System.currentTimeMillis();
                long timeDifferance = currentTime - arrivedTime;

                if(timeDifferance >= waitingTime1){
                    if (waitingThreads && arrivedTime != lastlyUnlockedFor) {
                        synchronized (lock) {
                            lock.notify();
                            lastlyUnlockedFor = arrivedTime;
                            indicator = 0;
                        }
                    }
                }else {
                    indicator++;

                    if(waitingTime1 * indicator > waitingTime2){
                        if (waitingThreads) {
                            synchronized (lock) {
                                lock.notify();
                            }
                        }

                        indicator = 0;
                    }
                }

                try {
                    Thread.sleep(waitingTime1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
