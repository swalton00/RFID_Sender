# RFID_Sender
Read one or more RFID readers and send the tags to any listening TCP/IP users (like JMRI)

Build with the build-in Gradle wrapper. The "shadowJar" task will create a single (all-encompassing) jar file as "build/libs/RFID_Sender-all.jar" This is an executable Java jar
with all required elements in it. Execute it with "java -jar RFID_Sender-all.jar -h" to see the options (and some log messages).

If you run this with Java 11, you will see some dire WARNING messages about illegal reflective access. This is a consequence of using Groovy for some of the code. You can
successfully ignore the messages, or use Java 1.8.

To run with one or more RFID readers, you will need the COM port name and a TCP/IP listener port number. Run the the above command with -l (instead of -h) to see a list
of all available COM ports. Run multiple times, with and without the RFID reader pluged in to determine the correct one for the reader. For a TCP/IP port, you will need to 
choose a port (or ports, one for each reader) number which is not currently in use on your system. I have been successful using 1417 and 1418 for TCP/IP ports ("netstat -na"
will show what ports are in use on you machine). If you have two readers at COM5 and COM6, and wish to use ports 1417 and 1418, use the following to run it:
    java -jar RFID_Sender-all.jar -r COM5 1417 -r COM6 1418
THis command will run (and log to the console with every read) until you type "stop" and press enter, at which point it will terminate

