package com.spw.rfid


import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.LogManager

import java.text.ParseException

class RFID_Reader {
    private static final String PROPERTY_FILE_NAME = "RFIDread.properties"
    private static final Logger logger = LogManager.getLogger(RFID_Reader.class)
    static boolean hadError = false   // should we stop before executing?
    static Properties properties

    static void main(String[] args) {
        Options options = new Options()
        Option help = Option.builder("h")
                .required(false)
                .desc("Print this message")
                .longOpt("help")
                .build()
        Option list = Option.builder("l")
                .required(false)
                .desc("List the available com ports")
                .longOpt("list")
                .build()
        Option create = Option.builder("c")
                .required(false)
                .desc("Create a properties file containing these options")
                .longOpt("Create")
                .build()
        Option readerOpt = Option.builder("r")
                .numberOfArgs(2)
                .longOpt("reader")
                .argName("Com port> <TCP port")
                .desc("Set the com port and TCP/IP port for this connection")
                .build()
        options.addOption(help)
        options.addOption(list)
        options.addOption(create)
        options.addOption(readerOpt)
        logger.info("Starting the RFID Reader")
        CommandLineParser parser = new DefaultParser()
        CommandLine line = null
        try {
            line = parser.parse(options, args)
        } catch (Exception e) {
            logger.error("Parse of command line failed ", e)
            line = null
        }
        if (line == null || line.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter()
            formatter.printHelp("RFID_Reader", options)
            logger.info("Exiting after printing command line options")
            System.exit(0)
        }
        if (line.hasOption("l")) {
            logger.info("listing the Com Ports")
            ReaderClass.showSerialPortList()
            System.exit(0)
        }
        if (!line.hasOption("r")) {
            logger.trace("attempting to read a property file")
            InputStream propertyInput = null
            try {
                propertyInput = new FileInputStream(PROPERTY_FILE_NAME)
            } catch (FileNotFoundException fileError) {
                logger.debug("no property file found")
            }
            if (propertyInput != null) {
                try {
                    properties = new Properties()
                    properties.load(propertyInput)
                } catch (Exception e) {

                }
            }
            println("You must specify USB device and port number")
            System.exit(2)
        }
        String[] reads = line.getOptionValues("r")
        int[] portVals = new int[5]
        def testNumber = { String stringVal ->
            try {
                return new Integer(stringVal)
            } catch (NumberFormatException e) {
                logger.error("port must be numeric", e)
                hadError = true
                return -1
            }

        }
        switch (reads.size()) {
            case 8: portVals[4] = testNumber(reads[7])
            case 6: portVals[3] = testNumber(reads[5])
            case 4: portVals[2] = testNumber(reads[3])
            case 2: portVals[1] = testNumber(reads[1])
        }
        if (line.hasOption("c")) {
            logger.debug("Creating a properties file and exiting")
            properties = new Properties()
            properties.setProperty("usb1", reads[0])
            properties.setProperty("port1", reads[1])
            switch (reads.size()) {
                case 8: properties.setProperty("usb4", reads[6])
                    properties.setProperty("port4", reads[7])
                case 6: properties.setProperty("usb3", reads[4])
                    properties.setProperty("port3", reads[5])
                case 4: properties.setProperty("usb2", reads[2])
                    properties.setProperty("port2", reads[3])
            }
            try {
                FileOutputStream propertyStream = new FileOutputStream(PROPERTY_FILE_NAME)
                properties.store(propertyStream, "Values for starting the RFID Forwarder")
            } catch (Exception e) {
                logger.error("Property file store failed with error {}", e.getMessage(), e)
            }
            logger.info("Exiting after creating the property file")
            System.exit(0)
        }
        if (hadError) {
            logger.error("exiting as an error was encountered")
        }
        ReaderClass reader1 = new ReaderClass(reads[0], portVals[1])
        reader1.start()
        ReaderClass reader2 = null
        ReaderClass reader3 = null
        ReaderClass reader4 = null
        if (reads.size() > 2) {
            reader2 = new ReaderClass(reads[2], portVals[2])
            reader2.start()
        }
        if (reads.size() > 4) {
            reader3 = new ReaderClass(reads[4], portVals[3])
            reader3.start()
        }
        if (reads.size() > 6) {
            reader4 = new ReaderClass(reads[6], portVals[4])
            reader4.start()
        }
        boolean continueLooping = true
        while (continueLooping) {
            println("Type 'Stop' to terminate")
            String inLine
            sleep(100)
            inLine = java.lang.System.in.newReader().readLine()
            if (inLine == null || inLine.equals("")) {
                println("type STOP to end")
            } else {
                continueLooping = false
            }
        }
        reader1.readerStop()
        if (reader2 != null) {
            reader2.readerStop()
        }
        if (reader3 != null) {
            reader3.readerStop()
        }
        if (reader4 != null) {
            reader4.readerStop()
        }
        System.exit(0)
    }
}
