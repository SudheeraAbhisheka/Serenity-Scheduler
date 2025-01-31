package org.example.server1.service;

import com.example.KeyValueObject;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;

@Service
public class PriorityBasedScheduling {
    private final SchedulingAlgorithms schedulingAlgorithms;
    private final LoadBalancingAlgorithm loadBalancingAlgorithm;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    @Setter
    private BlockingQueue<KeyValueObject> blockingQueuePriorityS;
    private boolean waitingThreads = false;

    private final Object secondLock = new Object();
    private boolean isReachedLimit = false;

    public PriorityBasedScheduling(SchedulingAlgorithms schedulingAlgorithm, LoadBalancingAlgorithm loadBalancingAlgorithm){
        this.schedulingAlgorithms = schedulingAlgorithm;
        this.loadBalancingAlgorithm = loadBalancingAlgorithm;
    }

    public void priorityBasedScheduling(LinkedHashMap<Integer, Double> thresholdTime, String completeFetchOrLB) {
        final Object lock = new Object();

        ConcurrentHashMap<Integer, Queue<ArrivedTimeObject>> queuePriorityX = new ConcurrentHashMap<>();
        if(completeFetchOrLB.equals("complete-fetch")){
            schedulingAlgorithms.setDynamicBlockingQueue(new LinkedBlockingQueue<>(1));
        }
        else if(completeFetchOrLB.equals("load-balancing")){
            loadBalancingAlgorithm.setWlbQueue(new LinkedBlockingQueue<>());
        }

        for(Integer key : thresholdTime.keySet()) {
            queuePriorityX.put(key, new ConcurrentLinkedQueue<>());
            System.out.println("Key: " + key);
        }

        executorService.submit(() -> {
            int sumOfInnerQueues = 0;
            while (!Thread.currentThread().isInterrupted()) {
                KeyValueObject keyValueObject = null;

                try {
                    /*if(blockingQueuePriorityS.isEmpty()){
                        if(waitingThreads){
                            System.out.println("Queue is getting empty. notified");

                            synchronized (lock) {
                                lock.notify();
                            }
                        }

                    }*/
                    keyValueObject = blockingQueuePriorityS.take();

                    sumOfInnerQueues = queuePriorityX.values().stream()
                            .mapToInt(Queue::size)
                            .sum();

                    if(completeFetchOrLB.equals("load-balancing")){
                        if (waitingThreads && sumOfInnerQueues + 1 >= 50) {
                            synchronized (lock) {
                                lock.notify();
                            }
                        }
                    }
                    else {
                        if (waitingThreads) {
                            synchronized (lock) {
                                lock.notify();
                            }
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                if(queuePriorityX.get(keyValueObject.getPriority()) != null){
                    queuePriorityX.get(keyValueObject.getPriority()).add(
                            new ArrivedTimeObject(System.currentTimeMillis(), keyValueObject)
                    );
                }
                else{
                    throw new RuntimeException("queuePriorityX.get(keyValueObject.getPriority()) == null");
                }

                if(sumOfInnerQueues + 1 >= 50){
                    isReachedLimit = true;
                    synchronized (secondLock){
                        secondLock.notify();
                    }
                }
            }
        });

        executorService.submit(() -> {
            List<OldObject> oldObjects = new ArrayList<>();

            while (!Thread.currentThread().isInterrupted()) {
                if(completeFetchOrLB.equals("load-balancing") && !isReachedLimit){
                    synchronized (secondLock){
                        try {
                            secondLock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                try {
                    for (Map.Entry<Integer, Queue<ArrivedTimeObject>> entry : queuePriorityX.entrySet()) {
                        Integer priority = entry.getKey();
                        Queue<ArrivedTimeObject> priorityQueue = entry.getValue();

                        if (!priorityQueue.isEmpty()) {
                            long age = System.currentTimeMillis() - priorityQueue.peek().getArrivedTime();

                            if (age > thresholdTime.get(priority)) {
                                oldObjects.add(new OldObject(age, priorityQueue.poll().getKeyValueObject()));
                            }

                        }
                    }

                    if (!oldObjects.isEmpty()) {
                        oldObjects.sort(Comparator.comparingLong(OldObject::getAge).reversed());
                        KeyValueObject k = oldObjects.remove(0).getKeyValueObject();
                        System.out.println("old object");

                        if(completeFetchOrLB.equals("complete-fetch")){
                            schedulingAlgorithms.getDynamicBlockingQueue().put(k);
                        }
                        else if(completeFetchOrLB.equals("load-balancing")){
                            loadBalancingAlgorithm.getWlbQueue().put(k);
                        }

                    } else {
                        boolean priorityQueuesAreEmpty = true;

                        for(Queue<ArrivedTimeObject> priorityQueue : queuePriorityX.values()){
                            if(!priorityQueue.isEmpty()){
                                KeyValueObject task = priorityQueue.poll().getKeyValueObject();
                                System.out.println("completed: "+task.getKey()+", priority: "+task.getPriority());
                                for(Queue<ArrivedTimeObject> pq : queuePriorityX.values()){
                                    System.out.printf("%s, ", pq.size());
                                }
                                System.out.println();


                                if(completeFetchOrLB.equals("complete-fetch")){
                                    schedulingAlgorithms.getDynamicBlockingQueue().put(task);
                                }
                                else if(completeFetchOrLB.equals("load-balancing")){
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

        executorService.submit(() ->{
            while(!Thread.currentThread().isInterrupted()){
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (waitingThreads && completeFetchOrLB.equals("load-balancing")) {
                    synchronized (lock) {
                        lock.notify();
                    }
                }
            }
        });
    }
}
