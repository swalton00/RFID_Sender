package com.spw.rfid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SocketSender extends Thread {
    private static final Logger log = LogManager.getLogger(SocketSender.class);
    private Socket socket;
    private BlockingQueue<byte[]> queue;
    private Integer selfId;
    private LinkedHashMap<Integer, SocketSender> socketList;
    private boolean continueRunning = true;

    /**
     * Creates the Socket sender with all required elements
     * Parameters
     * @param socket the socket to be used for sending RFID messages
     * @param selfId an identifier (Integer) used to find myself in the list (for cleanup)
     * @param socketList a list of all SocketSenders (including myself)
     */
    public SocketSender(Socket socket, Integer selfId, LinkedHashMap<Integer, SocketSender> socketList) {
        log.debug("Creating a Socket Sender");
        this.socket = socket;
        this.queue = queue;
        this.selfId = selfId;
        this.socketList = socketList;
        queue = new LinkedBlockingQueue<byte[]>();
    }

    public void addMsg(byte[] newMsg) {
        log.debug("adding a message to the queue");
        queue.add(newMsg);
    }

    /**
     * Request that SocketSender terminate
     */
    public void stopRun() {
        log.debug("Stopping the run of this SocketSender");
        continueRunning = false;
    }

    /**
     * Beginning to run the socket sender (and will continue until interrupted)
     *
     */
    @Override
    public void run() {
        log.debug("Beginning execution of this SocketSender");
        DataOutputStream output = null;
        InputStream socketStream = null;
        BufferedReader socketReader = null;
        try {
            output = new DataOutputStream(socket.getOutputStream());
            socketStream = socket.getInputStream();
            socketReader = new BufferedReader(new InputStreamReader(socketStream));
            if (socketReader.ready()) {
                log.trace("Reader has data - getting it");
                char[] buffer = new char[50];
                int read = socketReader.read(buffer, 0, buffer.length);
                String stringRead = new String(buffer);
                log.debug("read the following {}", stringRead);
            }
        } catch (IOException e) {
            log.error("Got an error creating the output stream", e);
        }
        while (continueRunning) {
            try {
                log.debug("about to wait for a message");
                byte[] newMessage = queue.take();
                log.debug("got a message - about to write");
                try {
                       output.write(newMessage);
                } catch (IOException io) {
                    continueRunning = false;
                    log.error("Error attempting output write, terminating", io);
                }
            } catch (InterruptedException ie) {
                log.info("SocketSender has been interrupted");
                continueRunning = false;
            }
        }
        try {
            output.close();
        } catch (IOException io) {
            log.error("IO Exception closing network connection", io);
        }
        socketList.remove(selfId);
        log.info("This socket has now terminated");
    }

}
