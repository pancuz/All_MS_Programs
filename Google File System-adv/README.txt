#Three folders – each for Client, Master, Server
# List of files in each folder
Ø	Client 
	o	ClientMainClass.java
	o	OperationRequestType.java
	o	ReceivingServerMessage.java
	o	TCPClient.java
	o	TCPServerThread.java
Ø	Master
	o	ListOfArray.java
	o	MetaMainClass.java
	o	MonitorHeartBeat.java
	o	ReceivingServerMessage.java
	o	ServerClass.java
	o	TCPClient.java
	o	TCPServerThread.java
Ø	Server
	o	HeartBeatThread.java
	o	ReceivingServerMessage.java
	o	ServerMainClass.java
	o	TCPClient.java
	o	TCPServerThread.java

# Sample input file must be present in the class path directory.
# Total storage space should be specified while running the server class file e.d java ServerMainClass 320 //320 is 320kb
#Metaserver runs on dc31.utdallas.edu
# Client file should be run on the client server
# Master server should be run on the Master server
# Server files should be run on file server

To Run java file: -
1.	Copy all java files and sample input file into a common directory
2.	Run javac *.java – this will compile all java files to class files
3.	Run ClientMainClass,  MetaMainClass, ServerMainClass are the class file with main, run this file on all servers
4.	Start Master server first using java MetaMainClass
5. 	Start FIle-Servers using java ServerMainClass 320
6	Start Client Main Class using java ClientMainClass input.txt
7.	File IO starts with ClientMainClass processing request.
