package com.spw.rfid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

public class QueueManager extends Thread {
    private static final Logger log = LogManager.getLogger(QueueManager.class);
    Map<Integer, SocketSender> clientList = null;
    BlockingQueue<byte[]> msgQueue;
    private boolean continueRunning = true;

    public QueueManager(BlockingQueue<byte[]> msgQueue, LinkedHashMap<Integer, SocketSender> clientList) {
        log.debug("Creating the Queue Manger");
        this.msgQueue = msgQueue;
        this.clientList = clientList;
        this.setName("QueueManager");
    }

    public void stopRun() {
        log.debug("Stopping the queue manager");
        continueRunning = false;
    }


    @Override
    public void run() {
        log.debug("Starting the queue manager");
        while (continueRunning) {
            try {
                log.debug("waiting for a message");
                byte[] newMsg = msgQueue.take();
                log.debug("Got a new message");
                clientList.forEach((id, client) -> {
                    client.addMsg(newMsg);
                });
            } catch (InterruptedException ie) {
                log.debug("Queue Manager was Interrupted");
                continueRunning = false;
            }
        }
        log.debug("QueueManager now terminating");
    }
}
