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
    private long arrivedTime;

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
            while (!Thread.currentThread().isInterrupted()) {
                KeyValueObject keyValueObject = null;

                try {
                    keyValueObject = blockingQueuePriorityS.take();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                if(queuePriorityX.get(keyValueObject.getPriority()) != null){
                    arrivedTime = System.currentTimeMillis();
                    queuePriorityX.get(keyValueObject.getPriority()).add(
                            new ArrivedTimeObject(arrivedTime, keyValueObject)
                    );
                }
                else{
                    throw new RuntimeException("queuePriorityX.get(keyValueObject.getPriority()) == null");
                }
            }
        });

        executorService.submit(() -> {
            List<OldObject> oldObjects = new ArrayList<>();

            while (!Thread.currentThread().isInterrupted()) {
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
                                    System.out.println("locking...");
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

        executorService.submit(()->{
            long lastlyUnlockedFor = Long.MAX_VALUE;
            long indicator = 0;
            final long THREAD_SLEEP_TIME = 500;

            while(!Thread.currentThread().isInterrupted()){
                long currentTime = System.currentTimeMillis();
                long timeDifferance = currentTime - arrivedTime;

                if(timeDifferance >= 500){
                    if (waitingThreads && arrivedTime != lastlyUnlockedFor) {
                        synchronized (lock) {
                            System.out.println("unlocking...");
                            lock.notify();
                            lastlyUnlockedFor = arrivedTime;
                            indicator = 0;
                        }
                    }
                }else {
                    indicator++;

                    if(THREAD_SLEEP_TIME * indicator > 5000){
                        System.out.println("caught you");
                        if (waitingThreads) {
                            synchronized (lock) {
                                lock.notify();
                            }
                        }

                        indicator = 0;
                    }
                }

                try {
                    Thread.sleep(THREAD_SLEEP_TIME);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
