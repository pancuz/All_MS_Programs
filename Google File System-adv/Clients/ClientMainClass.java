import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.RandomAccessFile;
import java.util.Random;
import java.util.HashMap;

public class ClientMainClass {

	private static ArrayList<String> listOfServer = new ArrayList<String>();
	private static ArrayList<String> listOfClient = new ArrayList<String>();
	private static String metaServer=null;
	private static int chunkSize=0;
	private static String hostname=null;
	private static int sizeOfChunk=8192;
	private static HashMap<String,Integer> hashForFullChunk = new HashMap<String,Integer>();
	
	
	private static void StartServerThread(int port) {
		try{
			System.out.println("=== Server Listening is started ===");
			TCPServerThread tst = new TCPServerThread(port);
			Thread t = new Thread(tst);
			t.start();
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	private static int generateRandom(int min, int max){

		Random  random=new Random();
		int d1 = random.nextInt(max - min + 1) + min;
		return d1;

	}
	
	private static void getMyIP(){
		
		StringBuffer output = new StringBuffer();
        try{
		//System.out.println("=== system command ===");
        Process p = Runtime.getRuntime().exec("hostname -i");

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = "";
        while ((line = reader.readLine())!= null) {
                output.append(line);
        }

       // System.out.println(output.toString());
        hostname=output.toString();
        }catch(IOException e){
                System.out.println(e);
        }

		
	}

	public static synchronized void sendMessageToHost(String serverName,String message) {
		
		//System.out.println("SendMsg TO: "+message+" "+serverName);
		try{
			Socket client = new Socket(serverName, 8025);                                                                                                                          //System.out.println("Just connected to " + client.getRemoteSocketAddress());
			PrintWriter out = new PrintWriter(client.getOutputStream(), true);
			out.println(message);
		} catch(IOException e){
			e.printStackTrace();
		}
//		writeTofile("output.txt",recordLogForFile);
	}

	private static Double calculateChunkIndex(Long bytes){
		/*Divide bytes by chunk size to determine chunk index */
		return ((Math.floor((bytes/(1024*1024*chunkSize)))));
	}

	public static synchronized void ProcessData(String message) {
		// Message was received from M-server - get all information and store in variable for processing
		String[] S3 = message.split("\\|");
		if(S3.length > 1){
			String msgType = S3[0];
			if(msgType.equals("error")){
		 	System.out.println(" = = = ERROR = = = Chunk server is not available: "+ S3[1]);
			}else if(msgType.equals("error1")){
				System.out.println(" = = = Redundant Request === : " +  S3[1]);

			}
			else {	
			String chunkName = S3[1];
			String serverName=S3[2];
			String offset = S3[4];
			String sizeOrData = S3[5];
			String whoIsClient = S3[3];
		//	System.out.println("=== Received message ===\t"+msgType+"\t"+chunkName+"\t"+serverName+"\t"+offset);
			message = msgType+"|"+chunkName+"|"+serverName+"|"+whoIsClient+"|"+offset+"|"+sizeOrData;
		 	//System.out.println("\n\n\n # Sending:\n " + message + "\n\n\n\n\n");
			/* Call method sendMessage to send read or append request to serverName sent by M-server */
			if((msgType.equals("w") && hashForFullChunk.containsKey(S3[4]))) {
				// do nothing;
			}
			else{
				hashForFullChunk.put(S3[4],1);
				sendMessageToHost(serverName, message);
			}
			}
		}
		else{

		    // It is return from a read request - so display the message
			System.out.println("\n###\n###\n### = Read: " + message+"\n###\n###");
		}
	}
	

	
	private static ArrayList<String> readFile(String filePath){
		File file = new File(filePath);
		filePath = file.getAbsolutePath();
        //System.out.println("File Path: "+filePath);
        FileInputStream inputStream = null;
		Scanner sc = null;
		ArrayList<String> listOfOperation = new ArrayList<String>();
		try{
			inputStream = new FileInputStream(filePath);
			sc=new Scanner(inputStream);
			while (sc.hasNextLine()) {
		        	String line = sc.nextLine();
				listOfOperation.add(line);
			} // End of while
		}catch(IOException e){
			System.out.println(e);
		}
		return listOfOperation;        
	}
	
	private static OperationRequestType getOperation(String fileName){
		File file = new File(fileName);
		String filePath = file.getAbsolutePath();
        //System.out.println("File Path: "+filePath);
        FileInputStream inputStream = null;
		Scanner sc = null;
		OperationRequestType reqObj = new OperationRequestType();
		ArrayList<String> listOfOperation = new ArrayList<String>();
		try{
			inputStream = new FileInputStream(filePath);
			sc=new Scanner(inputStream);
			while (sc.hasNextLine()) {
		        String line = sc.nextLine();
		        String [] wordList = line.split("\\|");
		        reqObj.requestType=wordList[0];
		        reqObj.fileName = wordList[1];
		        reqObj.inputBytes=Long.parseLong(wordList[2],10);
			}
		}catch(IOException e){
			System.out.println(e);
		}
		        
		return reqObj;        
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String filePath = args[0];
		//readFile(filePath);
		//Long inputBytes = 288832L;
		
		//Start the server to listen to incoming messages or Acknowledgments of requests
		StartServerThread(8025);
		try{
			Thread.sleep(5000);
		}catch(InterruptedException i){
			System.out.println(i);
		}
		getMyIP(); //get local ip address of the client
		//Start Client Operations
		System.out.println("This is client - so operation for client");
		
		/*Read about the operation - create, read or append a file - from input file */
		ArrayList<String> listOfOperation = new ArrayList<String>();
		listOfOperation=readFile(filePath);
		
		if(listOfOperation.isEmpty()){
			System.out.println("No operation was specified");
		}
		
		String MetaServer ="10.176.66.81";
		OperationRequestType reqObj = new OperationRequestType();
		//reqObj = getOperation(args[1]);
		for(int i=0;i<listOfOperation.size();i++){
			String [] message = listOfOperation.get(i).split("\\|");
			//System.out.println(message+"\t"+MetaServer);
			try{
				Thread.sleep(2000);
			}catch(InterruptedException ie){
				 System.out.println(ie);
			}

			if(message[0].equals("w")){
				/* if it is create operation - send message to Meta server= crate filename*/

				String finalMessage= message[0]+"|"+message[1]+"|dummy"+"|"+hostname+"|"+"dummy"+"|"+message[2];
			//	System.out.println("Sending: " + finalMessage);
				try{
					Thread.sleep(10);
				}catch(InterruptedException ie){
					System.out.println(ie);
				}
				sendMessageToHost(MetaServer, finalMessage);
			}
			else if((message[0].equals("r"))) {
				/* if it is read request - calculate chunk index and then send message= read filename chunkindex */
				//Integer chunkIndex=calculateChunkIndex(Long.parseLong(message[2])).intValue();
				Integer chunkIndex = Integer.parseInt(message[2])/sizeOfChunk;
				Integer offset = Integer.parseInt(message[2])%sizeOfChunk;
				Integer sizeToRead = Integer.parseInt(message[3]);
				String finalMessage = message[0] +"|"+message[1]+"|"+chunkIndex+"|"+sizeToRead+"|"+offset+"|"+hostname;
				try{
					Thread.sleep(1000);
				}catch(InterruptedException ie){
					System.out.println(ie);
				}
				sendMessageToHost(MetaServer, finalMessage);
				
			}
			else if((message[0].equals("a"))){
				/* Message format for append: "a filename dataToWrite hostname" */
				 String finalMessage= message[0]+"|"+message[1]+"|"+hostname+"|"+message[2];
			//	 System.out.println("Sending: " + finalMessage);
				 sendMessageToHost(MetaServer, finalMessage);
				 //sendMessageToHost(MetaServer, finalMessage);
	
			}
					
		}
		
		
		
		
	}

}
