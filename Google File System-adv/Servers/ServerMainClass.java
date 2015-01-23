import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.StringTokenizer;
import java.io.DataOutputStream;
import java.util.HashMap;


public class ServerMainClass {

	private static ArrayList<String> listOfServer = new ArrayList<String>();
	private static ArrayList<String> listOfClient = new ArrayList<String>();
	private static String metaServer=null;
	private static String clientName=null;
	protected static volatile String hostname=null;
	protected static String chunkList="";	
	protected static Integer no_of_chunks_hosted=0; //How many chunks are stored on this server
	protected static volatile Integer TOTAL_SPACE; // what is total available space on this server
	protected static volatile Integer free_space = TOTAL_SPACE; // How much space is available for storing = will keep reducing as chunks are created
	protected static Double percentLeft = 100.0; // If no create is performed, available percent is 100% that will be sent to Master server
	private static HashMap<String,String> hashForBuffer = new HashMap<String,String>();
	private static String buffer="";
	private static void getMyIP(){
		
		StringBuffer output = new StringBuffer();
        	try{
			System.out.println("=== system command ===");
        		Process p = Runtime.getRuntime().exec("/bin/hostname -i");

        		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        		String line = "";
        		while ((line = reader.readLine())!= null) {
                		output.append(line);
        		}

        		System.out.println(output.toString());
        		hostname=output.toString();
        		}catch(IOException e){
                	System.out.println(e);
        	}

		
	}
	protected static void getMyFiles(){
	
		StringBuffer output = new StringBuffer();
		try{
			String cmd = "/bin/ls "+hostname;
			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";
			int count=0;
			while ((line = reader.readLine())!= null) {
				output.append(line);
				output.append(" ");
				count++;
			}
			String allFiles = output.toString();
			Double temp = TOTAL_SPACE.doubleValue();
			percentLeft = ((temp - count*8.0)*100)/temp;
			no_of_chunks_hosted=count;
			chunkList = allFiles;
		}catch(IOException e){
			System.out.println(e);
		}

	}
	
	private static void performSCP(String fileName,String destServer){
	
		StringBuffer output = new StringBuffer();
		fileName="./"+hostname+"/"+fileName;
		System.out.println("=== Performing SCP for file, to server: "+ fileName+"\t"+destServer);
		try{
			
			String destDir = "/home/004/p/px/pxk131330/Project-3/DistributedFS/Servers/"+destServer+"/";
			String cmd = "sh transfer.sh "+destServer+" "+fileName+" "+destDir;
			System.out.println("SCP Command is: "+cmd);
			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";
			while ((line = reader.readLine())!= null) {
				output.append(line);
			}
			System.out.println("=== output ===");
			System.out.println(output.toString());
		}catch(IOException e){
			System.out.println(e);
		}
	}
	
	private static void performRead(String fileName,Integer sizeToRead, Integer offset, String clientName){
		File file = new File(hostname+"/"+fileName);
		String filePath = file.getAbsolutePath();
        //System.out.println("File Path: "+filePath);
		
		try{
			RandomAccessFile random = new RandomAccessFile(file,"r");
			random.seek(offset);
			byte [] arr = new byte[sizeToRead];
			random.readFully(arr);
			//System.out.println("== received offset: " + offset);
			String readMsg = new String(arr);
			System.out.println("== = Read Message :\t"+ readMsg); 
			//System.out.println("=== READ ===\n"+readMsg);
			random.close();
			sendMessageToHost(clientName, readMsg);
		}catch(IOException e){
			System.out.println(e);
		}

	}
	private static void performAppend(String fileName, String dataToAppend, String clientName, String secServer){

		try{
			File file = new File(hostname+"/"+fileName);
			if(!file.exists()){
 				System.out.println(" X X X  ERROR : File Does Not Exist  X X X "+ fileName);

			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);
			BufferedWriter bf = new BufferedWriter(fw);
			
			byte [] lsize = dataToAppend.getBytes();
			//System.out.println("=== \n Current file size and data: "+file.length() +" : " +lsize.length+"\n\n");
			if((file.length() + lsize.length) > 8192){
				// New chunk shoule be created - notify client that append has failed
				// Pad the file with null character
				System.out.println(" = Padding required : file size and to write data size = " + file.length() +" : " +lsize.length);
				for(int co=(int)(file.length()); co<8192; co++){
					bf.write("\0");
				}
				String [] fName = fileName.split("_");
				//System.out.println("-> fName: " + fName[0]);
				//Tell Client to request for another chunk and retry
				String messageBackToClient="w|"+fName[0]+"|"+"10.176.66.81"+"|"+clientName+"|"+fileName+"|"+dataToAppend;
				//System.out.println("===> \nMessage Back to client: " + messageBackToClient+"\n\n\n");
				sendMessageToHost(clientName,messageBackToClient);
				String [] secList = secServer.split("\\-");

				String c2=null;
				String c3=null;
				if(secList[0].equals(hostname)){
					c2=secList[1];
					c3=secList[2];
				}
				if(secList[1].equals(hostname)){
					c2=secList[0];
					c3=secList[2];
				}
				if(secList[2].equals(hostname)){
					c2=secList[0];
					c3=secList[1];
					
				}
				String msgForPadding = "pad|"+fileName;
				sendMessageToHost(c2,msgForPadding);
				sendMessageToHost(c3,msgForPadding);

			}
			else {
			// Append to the file 
			//

				int cnt=0;
				String response1="";
				String response2="";
				String response3="";
				String response4="";
				
				//System.out.println("Secondary : "+secServer);
				//StringTokenizer st=new StringTokenizer(secServer,"-");
				String [] secList = secServer.split("\\-");

				String c2=null;
				String c3=null;
				if(secList[0].equals(hostname)){
					c2=secList[1];
					c3=secList[2];
				}
				if(secList[1].equals(hostname)){
					c2=secList[0];
					c3=secList[2];
				}
				if(secList[2].equals(hostname)){
					c2=secList[0];
					c3=secList[1];
					
				}
				
				Socket clientSocket1 = new Socket(c2, 8025);		 
				Socket clientSocket2 = new Socket(c3, 8025);		
				
				DataOutputStream outToServer1 = new DataOutputStream(clientSocket1.getOutputStream());
				DataOutputStream outToServer2 = new DataOutputStream(clientSocket2.getOutputStream());
				
				BufferedReader inFromServer1 = new BufferedReader(new InputStreamReader(clientSocket1.getInputStream()));
				BufferedReader inFromServer2 = new BufferedReader(new InputStreamReader(clientSocket2.getInputStream()));
				
				//System.out.println("Before outToServer\t " + dataToAppend);
			
				outToServer1.writeBytes("ia|"+fileName+"|"+hostname+fileName+"|"+dataToAppend+"\n");
				outToServer2.writeBytes("ia|"+fileName+"|"+hostname+fileName+"|"+dataToAppend+"\n");
				
			 	//System.out.println("outToServer is done - needs response");
				long millis=System.currentTimeMillis() / 1000L;
				if((response1 = inFromServer1.readLine())!=null && (response2 = inFromServer2.readLine())!=null) /* || (System.currentTimeMillis() / 1000L)>(millis+2000))*/
				{
					/*if((System.currentTimeMillis() / 1000L)>(millis+2000))
					{
					System.out.println(" XXX inside timeout - sending to client \n");
					sendMessageToHost(clientName,"error|"+"Replica append timeout");
					}
					else
					{*/	
					System.out.println("===Received response1 and response2\t" + response1 + " ||| " + response2);
					
					clientSocket1.close();
					clientSocket2.close();
					outToServer1.close(); 
					outToServer2.close();
 					inFromServer1.close(); 
					inFromServer2.close();

					Socket clientSocket3 = new Socket(c2, 8025);		 
					Socket clientSocket4 = new Socket(c3, 8025);

					
					 DataOutputStream outToServer3 = new DataOutputStream(clientSocket3.getOutputStream());
					 DataOutputStream outToServer4 = new DataOutputStream(clientSocket4.getOutputStream());
				
					BufferedReader inFromServer3 = new BufferedReader(new InputStreamReader(clientSocket3.getInputStream()));
					BufferedReader inFromServer4 = new BufferedReader(new InputStreamReader(clientSocket4.getInputStream()));
				
					outToServer3.writeBytes("commit|"+fileName+"|"+hostname+fileName+"\n");
					outToServer4.writeBytes("commit|"+fileName+"|"+hostname+fileName+"\n");

				if((response3 = inFromServer3.readLine())!=null && (response4 = inFromServer4.readLine())!=null)
					{
				// Append to the file 
				 
					fw.write(dataToAppend);
					System.out.println("Appended");
	
					clientSocket3.close();
					clientSocket4.close();
					outToServer3.close(); 
					outToServer4.close();
 					inFromServer3.close(); 
					inFromServer4.close();
					
					}

					//}//end of inner else

				}
 

			
			} // end of else
			bf.close();
			fw.close();

		}catch(IOException e){
			System.out.println("Exception thrown:"+e);
		}
		

	


		
	}
	
	private static void performCreate(String fileName){
			

		/* For any create request, check if a directory exists with the FileServer name(hostname) 
		 * Create file in that directory path - hostname/filename
		 * With directory name as FileServer name, we can store same file in different directory to show replication
		 */

		//File DIR_TO_CREATE = new File("ServerName");
		//fileName = hostname+"/"+fileName;
		File file=new File(hostname+"/"+fileName);
		try{
			
			if(file.createNewFile()){
				System.out.println("=== File created !!! "+ fileName);
				//chunkList=chunkList+fileName+"-";
				//no_of_chunks_hosted++;
				//free_space = free_space - 8; // everytime a chunk is created, reduce available space by size of chunk
				//Double x=free_space.doubleValue();
				//Double x1=TOTAL_SPACE.doubleValue();
				//percentLeft = (x/x1)*100.0;//percent of free space left 
				//System.out.println("\n\nCalculated percentLeft: " + percentLeft);
			}
			/*else{
				System.out.println(" XXX File failed to create XXX");
			}*/
		}catch(IOException e){
			System.out.println(e);
		}

		/*try{
			RandomAccessFile f = new RandomAccessFile(file, "rw");
			f.setLength(8 * 1024 * 1024);
		}catch (Exception e) {
			System.err.println(e);
		}*/
	}
	
	private static void StartServerThread(int port) {
		try{
			System.out.println("=== Server is started ===");
			TCPServerThread tst = new TCPServerThread(port);
			Thread t = new Thread(tst);
			t.start();
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	private static void StartHeartBeat(int port){
		try{
			System.out.println("=== Heartbeat is started ===");
			HeartBeatThread hbt = new HeartBeatThread(port);
			Thread t = new Thread(hbt);
			t.start();
		} catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	
	public static synchronized void sendMessageToHost(String serverName,String message) {
		
		
		try{
			Socket client = new Socket(serverName, 8025);                                                                                                                          //System.out.println("Just connected to " + client.getRemoteSocketAddress());
			PrintWriter out = new PrintWriter(client.getOutputStream(), true);
			out.println(message);
		} catch(IOException e){
			e.printStackTrace();
		}
//		writeTofile("output.txt",recordLogForFile);
	}



	public static synchronized void ProcessData(String message) {
		// Message was received - get all information and store in variable for processing
		//System.out.println("In processData: "+message);
		String[] S3 = message.split("\\|");
		String msgType = S3[0];
		String chunkName = S3[1];
		//String serverName=S3[2];
		//String dataToApp = S3[2];
		Integer offset;
		String msgRead= "r";
		String msgAppend = "a";
		String msgCreate = "w";
		String msgSCP = "scp";
		String msgInternalAppend = "ia";
		String msgCommit="commit";
		String msgPad = "pad";
		
		//System.out.println("=== Received message ===\t"+msgType+"\t"+chunkName+"\t"+offset);
		
		if(msgRead.equals(msgType)){
			System.out.println(" = Read Request =");
			Integer sizeToRead = Integer.parseInt(S3[5]);
			offset=Integer.parseInt(S3[4]);
			clientName=S3[3];
			performRead(chunkName,sizeToRead,offset,clientName);
		}
		else if(msgAppend.equals(msgType)){
			System.out.println(" = Append request = ");
			//offset=Integer.parseInt(S3[2]);
			String dataToAppend = S3[5];
			clientName=S3[3];
			String secServer = S3[4];
			performAppend(chunkName,dataToAppend,clientName,secServer);
		}
		else if(msgCreate.equals(msgType)){
			System.out.println(" = Create Request = " );
			performCreate(chunkName);
		}
		else if(msgSCP.equals(msgType)){
			System.out.println(" === SCP To be performed ===");
			// chunkName:which file to SCP for replication
			// S3[2]:destination server 
			performSCP(chunkName,S3[2]);

		}

		else if(msgInternalAppend.equals(msgType)){
			String myBuffer = S3[3];
			hashForBuffer.put(S3[2],myBuffer);
			//System.out.println("=== on secondary XXX === inside IA ");
			//buffer=dataToApp;
			//DataOutputStream outToClient = new DataOutputStream(TCPServerThread.serverSocket.getOutputStream());
			//outToClient.writeBytes("Ack\n");
		}
		else if(msgCommit.equals(msgType)){

			//System.out.println("=== on secondary === received commit === before appending ");
			String toWrite = hashForBuffer.get(S3[2]);
			File file = new File(hostname+"/"+chunkName);
			if(!file.exists()){
			System.out.println(" X X X  ERROR : File Does Not Exist  X X X "+ chunkName);
				
			}
			try{
				FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);
				BufferedWriter bf = new BufferedWriter(fw);
				fw.append(toWrite);
				System.out.println("===Appended on secondary");
				fw.close();
				bf.close();
			}catch(IOException e){
				System.out.println(e);
			}
		}
		else if(msgPad.equals(msgType)){
			try{
				File file = new File(hostname+"/"+chunkName);
				if(!file.exists()){
 				 System.out.println(" X X X  ERROR : File Does Not Exist  X X X "+ chunkName);

				}
				FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);
				BufferedWriter bf = new BufferedWriter(fw);
			
				//byte [] lsize = dataToAppend.getBytes();
				for(int co=(int)(file.length()); co<8192; co++){
					bf.write("\0");
				}
				bf.close();
				fw.close();
			}catch(IOException e){
				System.out.println(e);
			}
			

		}



		/* Server should know request_type(read/append), chunk name and offset
		 * So, create a message with request_type, chunk_name and offest and send to the server
		 */
		//message = msgType+" "+chunkName+" "+offset;
		
		/* Call method sendMessage to send read or append request to serverName sent by M-server */
		//sendMessageToHost(serverName, message);
//		String comMessage = "compute";
//		String termMessage = "terminate";
		
	
	
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
		        String [] wordList = line.split("\\|");
		        if(wordList[0].equals("server")){
		        	//
		        }
		        else if(wordList[0].equals("meta")){
		        	/* Meta Server information to be stored */
		        }
		        else if(wordList[0].equals("operation")){
		        	listOfOperation.add(wordList[1]);
		        }
		        
			}
		}catch(IOException e){
			System.out.println(e);
		}
		return listOfOperation;        
	}
	
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//String filePath = args[0];
		//readFile(filePath);

		getMyIP();
		Integer size = Integer.parseInt(args[0]);
		TOTAL_SPACE = size;
		free_space = size;
		System.out.println(" = "+ hostname);
		File DIR_TO_CREATE = new File(hostname);
		if (!DIR_TO_CREATE.exists()) {
		   System.out.println("creating directory: " + hostname);
		   boolean result = false;
		     try{
		     	DIR_TO_CREATE.mkdir();
			result = true;
			} catch(SecurityException se){
				System.out.println(se);
			}        
			if(result) {    
				 System.out.println("DIR created");  
		         }
		
		} // End of directory creation



		//Start the server to listen to incoming messages or Acknowledgments of requests
		StartServerThread(8025);
		try{
			Thread.sleep(5000);
		}catch(InterruptedException i){
			System.out.println(i);
		}
		
		StartHeartBeat(8025); // Create a thread that will keep sending heartbeat to Meta-server
		
		System.out.println("This is server - so heartbeat OR serve to client");
		

		//String MetaServer ="192.168.1.138";
		
//		for(int i=0;i<listOfOperation.size();i++){
//			String message = listOfOperation.get(i);
//			System.out.println(message+"\t"+MetaServer);
//			sendMessageToHost(MetaServer, message);
//					
//		}
//		
		
		
		
	}

}
