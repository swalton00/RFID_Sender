package com.spw.rfid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;


public class SocketManager extends Thread {

    private BlockingQueue<byte[]> messageQueue;
    private int networkPort;
    private LinkedHashMap<Integer, SocketSender> clientList = null;
    private Integer clientCount = Integer.valueOf(0);
    private static final Logger log = LogManager.getLogger(SocketManager.class);
    private boolean continueRunning = true;
    private QueueManager qm = null;

    public SocketManager(int networkPort, BlockingQueue<byte[]> messages ) {
        super.setName("SocketManager");
        this.messageQueue = messages;
        this.networkPort = networkPort;
        clientList = new LinkedHashMap<Integer, SocketSender>();
    }

    public void addToQueue(byte[] newMsg) {
        log.debug("adding a new message to the queue");
        messageQueue.add(newMsg);
        log.debug("Queue has size {}", messageQueue.size());
    }
    public void stopRun() {
        log.debug("Stopping the Socket Manager thread");
        continueRunning = false;
        qm.stopRun();
        qm.interrupt();
        clientList.forEach((id, client) -> {
            client.stopRun();
            client.interrupt();
        });
    }

    @Override
    public void run() {
        log.info("Starting the Socket Manager Thread");
        messageQueue = new LinkedBlockingQueue<byte[]>();
        qm = new QueueManager(messageQueue, clientList);
        qm.start();
        ServerSocket serverSocket = null;
         while (continueRunning) {
            try {
                try {
                    serverSocket = new ServerSocket(networkPort);
                } catch (IOException e) {
                    log.error("Error creating server socket", e);
                    continueRunning = false;
                }
                Socket socket = serverSocket.accept();
                String remoteAddress = "Accepted new connection from " + socket.getInetAddress().toString();
                log.info(remoteAddress);
                clientCount++;
                SocketSender newSender = new SocketSender(socket, clientCount, clientList);
                clientList.put(clientCount, newSender);
                newSender.start();
                log.debug("SocketSender has now been started");
            } catch (IOException e) {
                log.error("Exception while attempting to accept a new network connection", e);
                continueRunning = false;
            } finally {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    log.error("Exception closing socket", e);
                }

            }

        /*    try {
                sleep(100000);
            } catch (InterruptedException e) {
                log.debug("sleep was interrupted");
            }
       */     log.debug("back from sleep - look for another incoming");
        }
        clientList.forEach((k, v) -> {
            v.stopRun();
            v.interrupt();
                });
        qm.stopRun();
        qm.interrupt();
    }


}
