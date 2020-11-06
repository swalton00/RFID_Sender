package com.spw.rfid

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import purejavacomm.CommPortIdentifier
import purejavacomm.NoSuchPortException
import purejavacomm.SerialPort
import purejavacomm.SerialPortEvent
import purejavacomm.SerialPortEventListener
import purejavacomm.UnsupportedCommOperationException

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class ReaderClass extends Thread {
    private static final Logger logger = LogManager.getLogger(ReaderClass.class)

    DataOutputStream output = null
    //ServerSocket serverSocket
    //Socket socket
    int networkPort = 0
    String comPort = null
    SerialPort serialPort = null;
    InputStream serialStream = null
    //InputStream socketStream = null
    //BufferedReader socketReader = null
    public static final int MESSAGE_LENGTH = 16
    String rawString
    BlockingQueue<byte[]> msgQueue
    SocketManager clientManager


    public ReaderClass() {
        logger.trace("in the default constructor")
    }

    public ReaderClass(String comPort) {
        logger.debug("In the constructor with a String (port name) {}", comPort)
        setCommPort(comPort)
    }

    public ReaderClass(String comPort, int networkPort) {
        logger.debug("setting up for comPort {} and network port {}", comPort, networkPort)
        this.networkPort = networkPort
        //setNetwork(networkPort)
        setCommPort(comPort)
    }

    public boolean checkValid(byte[] input, int length) {
        if (length != 16)
            return false;  // must be exactly 16 bytes long
        if (input[0] != 0x02)
            return false; // byte 1 must be a STX
        if (input[15] != 0x03)
            return false;  // last byte must be ETX
        if (input[13] != 0x0d)
            return false;   // chould end with CR
        if (input[14] != 0x0a)
            return false;    // plus LF
        byte[] byteVal = new byte[12]; // 10 bytes of data plue 2 bytes for checksum
        for (int i = 0; i < 12; i++) {
            byteVal[i] = input[i + 1];
        }
        String stringVal;
        try {
            stringVal = new String(byteVal, "UTF-8");
        } catch (Exception e) {
            return false;  // if not valid hex, can't be a good tag
        }
        byte[] decoded = stringVal.decodeHex();
        if (decoded.length != 6)
            return false; // must be 5 bytes + a check digit
        int checksum = 0;
        for (int i = 0; i < 5; i++) {
            checksum = checksum ^ decoded[i];
        }
        return checksum == decoded[5];
    }

    public String returnTag(byte[] input) {
        byte[] byteVal = new byte[12]; // 10 bytes of data plus 2 bytes for checksum
        for (int i = 0; i < 12; i++) {
            byteVal[i] = input[i + 1];
        }
        String stringVal = "bad hex value";
        try {
            stringVal = new String(byteVal, "UTF-8");
        } catch (Exception e) {
            logger.error("bad hex value {}", stringVal)
        }
        return stringVal;
    }

    static void showSerialPortList() {
        logger.debug("returning a list of the Comm Ports")
        Enumeration<String> portList = CommPortIdentifier.getPortIdentifiers()
        while (portList.hasMoreElements()) {
            CommPortIdentifier cpi = portList.nextElement()
            if (cpi.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                println(cpi.getName())
            }
        }
        println("All com ports listed")
    }

    void setNetwork(int port) {
        logger.debug("setting up for network port {}", port)
        //serverSocket = new ServerSocket(port)
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    String bytesToHex(byte[] bytes, int count) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < count; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public readSerialPort() {
        byte[] dataReceived = new byte[16]
        try {
            int received = 0
            while (received < MESSAGE_LENGTH) {
                received += serialStream.read(dataReceived, received, MESSAGE_LENGTH - received)
            }
            String firstString = bytesToHex(dataReceived, received)
            logger.trace("received the following in hex: {}", firstString)
            byte[] newMsg = new byte[received]
            if (checkValid(dataReceived, received)) {
                rawString = dataReceived
                String newTag = returnTag(dataReceived)
                newTag = newTag.substring(0, 10)
                logger.info("New tag -- {}", newTag)
               // println("read a new tag ${newTag}")
                ArrayList<String> tagReceived = new ArrayList<>()
                tagReceived.add(newTag)
                //  add the send here
              /*  if (socketReader.ready()) {
                    logger.trace("Reader has data - getting it")
                    char[] buffer = new char[50]
                    int haveRead = socketReader.read(buffer, 0, buffer.size())
                    byte[] bytes = new byte[haveRead]
                    for (int i in 0..haveRead - 1) {
                        bytes[i] = (byte) buffer[i]
                    }
                    String translated = bytesToHex(bytes, haveRead)
                    logger.trace("Read {} characters which are {}", haveRead, translated)
                    logger.info("Read a tag: {}", newTag)
                }
  */              logger.trace("about to write {}", dataReceived)
               // msgQueue.add(rawString)
                clientManager.addToQueue(dataReceived);
                //output.write(dataReceived, 0, received)
                logger.trace("added to the message queue")
            } else {
                logger.error("bad tag checksum for {}", bytesToHex(dataReceived, received))
            }
        } catch (Exception e) {
            logger.error("Exception reading - {}", e.getMessage(), e)
        }
    }

    void readerStop() {
        logger.debug("Stopping this reader")
        clientManager.stopRun()
        clientManager.interrupt()
        serialPort.close();
    }

    void listener(SerialPortEvent portEvent) {
        if (portEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            println("have data available")
            readSerialPort()
        } else {
            logger.error("Got a serial port event of unknown type, {}", portEvent)
        }
    }

public void setCommPort(String port) {
        logger.debug("setting comm port to {}", port)
        comPort = port
    }

    @Override
    void run() {
        boolean hadError = false
        if (comPort == null || networkPort == 0) {
            throw new Exception("Both Com Port and Network port must be specified")
        }
        logger.debug("setting up the serial port")
        //msgQueue = new LinkedBlockingQueue<byte[]>()
        try {
            CommPortIdentifier cpi = CommPortIdentifier.getPortIdentifier(comPort)
            serialPort = cpi.open("RR Tools", 1500)
            serialPort.enableReceiveTimeout(20000)
            serialPort.enableReceiveThreshold(16)
            serialStream = serialPort.getInputStream()
            logger.debug("starting a reader on the Serial Port -- waiting for tags")
            serialPort.addEventListener([serialEvent: { event -> listener(event) }] as SerialPortEventListener)
            serialPort.notifyOnDataAvailable(true)
            logger.debug("serial port now setup")
        } catch (NoSuchPortException nspe) {
            logger.debug("No Such port for port {}", comPort)
            hadError = true
        } catch (PortInUseException) {
            logger.debug("Port {} is already in use", comPort)
            hadError = true
        } catch (UnsupportedCommOperationException uco) {
            logger.error("Unsupported comm operation thrown for port {}", comPort, uco)
            hadError = true
        } catch (Exception e) {
            logger.error("Exception opening port {}", comPort, e)
            hadError = true
        }
        if (hadError) {
            logger.error("throwing an exception to force termination - this COM port failed")
            throw new RuntimeException("Error during initialization")
        }
        logger.debug("now accepting network connections")
        clientManager = new SocketManager(networkPort, msgQueue)
        clientManager.start()
        /*socket = serverSocket.accept()
        output = new DataOutputStream(socket.getOutputStream())
        socketStream = socket.getInputStream()
        socketReader = new BufferedReader(new InputStreamReader(socketStream))
*/
    }
}

