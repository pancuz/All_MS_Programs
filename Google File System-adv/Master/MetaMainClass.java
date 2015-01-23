import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.Comparator;
import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.Collections;

public class MetaMainClass {

	protected static ArrayList<String> listOfServer = new ArrayList<String>();
	private static ArrayList<String> listOfClient = new ArrayList<String>();
	private static String metaServer=null;
	private static volatile HashMap<String,ListOfArray> chunkLocationHash = new HashMap<String,ListOfArray>();
	public static ServerClass [] serverObjArray=new ServerClass[25];
	public static volatile HashMap<String,Long>mapServerHeartBeat=new HashMap<String,Long>();
	public static String hostname;
	public static int gCount=0;
	public static ListOfArray [] listObj = new ListOfArray[25];
	protected static TreeMap <String,String> chunkMap = new TreeMap<String,String>(); // To store received list of chunks 
	private static HashMap <String,String> hashForClient = new HashMap<String,String>();
	private static HashMap <String,String> hashForServer = new HashMap<String,String>(); // To select unique server by storing temporary server
	private static HashMap <String,String> hashUniqueServer = new HashMap<String,String>(); // To select unique server by storing temporary server
	private static HashMap <String,Integer> hashFileNameAndChunkNumber = new HashMap<String,Integer>(); // To name chunk for each file based on number
	private static volatile HashMap <String,String> hashForChunkAndPrimaryServer = new HashMap<String,String>();
	private static HashMap <String,Integer> hashStoreNumberOfChunk = new HashMap<String,Integer>(); // to store number of chunk
	protected static HashMap <String,Double> hashStoreFreeSpace = new HashMap<String,Double>(); // to store free space
	private static Map <String,Double> sortedHash = new HashMap<String,Double>();
	private static int NUMBER_OF_REPLICAS = 3; // How many replicas to create for each chunk

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

        System.out.println("\nMy local ip address is: " + output.toString());
        hostname=output.toString();
        }catch(IOException e){
                System.out.println(e);
        }

		
	}
	
	/*
	private static void sortHashMapOnFreeSpace(HashMap<String,Double> mapToSort){
		//Sorting based on the value of the percentage available = server with less space will be on top of hash
		Comparator<String> comparer = new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			}};
		Map<String, Double> sorted = new TreeMap<String, Double>(comparer);
		sorted.putAll(mapToSort);
		sortedHash = sorted;
		System.out.println("\n SORTED: \t"+sortedHash);


	} */



	 private static void sortHashMapOnFreeSpace(HashMap<String,Double> unsortMap){

		List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(unsortMap.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
			public int compare(final Map.Entry<String, Double> o1, final Map.Entry<String, Double> o2) {
				return ( ((o1)).getValue()).compareTo(((o2)).getValue());
			}
		});

		Map <String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for (Iterator<Map.Entry<String, Double>>  it = list.iterator(); it.hasNext();) {
			Map.Entry<String, Double>  entry = (Map.Entry<String, Double> ) it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		sortedHash = sortedMap;
		//System.out.println("\n SORTED: \t"+sortedHash);
	 }


	private static ServerClass getServerForChunk(int count){
	
		//return bottom most  elements of hash if conditions are met
		String serverName="";
		Iterator<Map.Entry<String, Double>> it = sortedHash.entrySet().iterator();
		Map.Entry<String, Double> e1;
		ServerClass tempObj=null;
		ArrayList <String> arr = new ArrayList<String>();
		while (it.hasNext()) {
			e1 = it.next();
			serverName = e1.getKey().toString();
			arr.add(serverName);
		}
		if(arr.size() < 3){

			System.out.println("At least three servers are not available for Replicas\n PLEASE VERIFY IF AT LEAST THREE SERVERS ARE THERE !!!");
			System.exit(0);
		}

		

//		for(int ind = 0;ind<arr.size(); ind++){
//			System.out.println("=== array element ===\t" +  arr.get(ind));
//		}
		
		abc : for(int i = arr.size()-1; i>=0; i--){ // start from bottom of array list as one with more space will be at end
			serverName = arr.get(i);
			int j = 0;
//			System.out.println("In serverClass array: "+ serverName);
			//System.out.println("length of array object " +serverObjArray.length);
			for(j=0;j<listOfServer.size();j++){ 
				
				if(serverObjArray[j].status==true && serverObjArray[j].serverId.equals(serverName) && !hashForServer.containsKey(serverName)){
//					System.out.println("In IF all true: " + j+"\t"+"\t"+i);
					if(count !=2){
						hashForServer.put(serverName,"1");
//						System.out.println("Inside count!="+count+"\t"+hashForServer);
					}
					else{
//						System.out.println("In else: "+serverName);
						hashForServer.clear();
					}
					//System.out.println(" = Returning : " +  serverObjArray[j].serverId);
					tempObj =  serverObjArray[j];
					break abc;
				}

			}
			
		} // end of outer for loop=i
		//System.out.println(" = Returning : " +  tempObj.serverId);
		return tempObj;
	}
	
	/*
	private static ServerClass getServerForChunk(int count){
		/* This method will return a random server whose status is true
		 * Extra code added to return unique server each time for NUMBER_OF_REPLICAS times
		 * */
	/*	int no_of_attempt = 10;
		Random random = new Random();
		int rand = random.nextInt((listOfServer.size()-1) - 0 + 1) + 0;
		//System.out.println("=Generated Random: "+rand);
		while(serverObjArray[rand].status==false || hashForServer.containsKey(serverObjArray[rand].serverId)){
			random = new Random();
			rand = random.nextInt((listOfServer.size()-1) - 0 + 1) + 0;
			no_of_attempt--;
			System.out.println(" --- Inside while : " + serverObjArray[rand].serverId);
			if(no_of_attempt == 0){
			// This will also avoid while loop to get into infinite loop 
				System.out.println(" === Not Able to find unique server even after 10 attempts ===\n");
				System.out.println("\n=== I suggest you to verify that number of FILE servers being used is at least three ===\n");
				System.exit(0);

			}
		}
		if(count == 2){
		 /* We have got upto three unique server for replication and so empty the hashmap 
		  * for next three unique server entry
		  
			System.out.println("count is 2: "+ serverObjArray[rand].serverId);
			hashForServer.clear();

		 }
		 else{
			hashForServer.put(serverObjArray[rand].serverId,"1");
			System.out.println("Hash for server: "+ hashForServer);
		 }
		 System.out.println(" --- returning : " + serverObjArray[rand]);
		return(serverObjArray[rand]);
		
		
	} */
	
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

	private static String getUniqueServer(String chunkName,int count){
		// This method will return a unique servername for SCP, saticfying all conditions
		String serverName="";
		Iterator<Map.Entry<String, Double>> it = sortedHash.entrySet().iterator();
		Map.Entry<String, Double> e1;
		ServerClass tempObj=null;
		ArrayList <String> arr = new ArrayList<String>();
		while (it.hasNext()) {
			e1 = it.next();
			serverName = e1.getKey().toString();
			arr.add(serverName);
		}
		if(arr.size() < 3){
			System.out.println("At least three servers are not available for Replicas\n PLEASE VERIFY IF AT LEAST THREE SERVERS ARE THERE !!!");
			System.exit(0);
		}
		
		for(int p=0; p<chunkLocationHash.get(chunkName).listOfChunks.size();p++){
			String tmp = chunkLocationHash.get(chunkName).listOfChunks.get(p);
			System.out.println("\n=== tmp is: "+ tmp);
			arr.remove(tmp);	
			
			
		}

		abc : for(int i = arr.size()-1; i>=0; i--){ 
			serverName = arr.get(i);
			int j=0;
			for(j=0;j<listOfServer.size();j++){
				if(serverObjArray[j].status==true && serverObjArray[j].serverId.equals(serverName) && !hashUniqueServer.containsKey(serverName)){
					for(int k=0;k<chunkLocationHash.get(chunkName).listOfChunks.size();k++){
						if(!chunkLocationHash.get(chunkName).listOfChunks.get(k).equals(serverName)){
							if(count !=2){
								hashUniqueServer.put(serverName,"1");
							}
							else{
								hashUniqueServer.clear();
							}
							tempObj =  serverObjArray[j];
							break abc;
						}
					}
				}
			}
		}
		//System.out.println(" = From getUnique Server Returning : " +  tempObj.serverId);
		return tempObj.serverId;
	
	}

	protected static void serverFailed(String failedServerName){
		String message = "";
		sortHashMapOnFreeSpace(hashStoreFreeSpace); // perform a sort based on free space - to ensure load balancing
		String serverToSCPFrom = "";
		//check if at least one chunk was there on this failed server
		if(chunkMap.containsKey(failedServerName)){
			String tmp1 = chunkMap.get(failedServerName);
			String [] allChunks = tmp1.split("\\s+");
			for(int k=0;k<allChunks.length;k++){
				System.out.println("\nX "+allChunks[k]);
				if(allChunks[k].length()>1){
					System.out.println("\nXXX "+allChunks[k]+"\n");
					//select chunk and now find a destination to create replica
					//Each chunk must be copied to three unique servers-so one copy for failed server
					//A server which already has current chunk should not get this chunk
					String destServer = getUniqueServer(allChunks[k],2);
					int myc;
					xyz : for(myc=0;myc<chunkLocationHash.get(allChunks[k]).listOfChunks.size();myc++){
						if(chunkLocationHash.get(allChunks[k]).listOfChunks.get(myc).equals(failedServerName)){
							chunkLocationHash.get(allChunks[k]).listOfChunks.set(myc,destServer);
							break xyz;
						}
		
					}
					
					System.out.println("\n\n=== After failure updated chunk list === Replace \n" + chunkLocationHash.get(allChunks[k]).listOfChunks.get(myc));
					outer : for(int p=0; p<chunkLocationHash.get(allChunks[k]).listOfChunks.size();p++){
						if(!chunkLocationHash.get(allChunks[k]).listOfChunks.get(p).equals(failedServerName)){
							
							serverToSCPFrom = chunkLocationHash.get(allChunks[k]).listOfChunks.get(p);
							for(int j=0;j<listOfServer.size();j++){
								if(serverObjArray[j].status==true && serverObjArray[j].serverId.equals(serverToSCPFrom)){
									if(!serverToSCPFrom.equals(destServer)){
										break outer;
									}
								}
							}

						}
					}
					//for(int j=0;j<NUMBER_OF_REPLICAS;j++){
						//String destServer = getUniqueServer(allChunks[k],2);
					// Update hash of chunk count here
					        chunkMap.remove(failedServerName);
						hashStoreNumberOfChunk.remove(failedServerName);
						hashStoreFreeSpace.remove(failedServerName);
						message = "scp|"+allChunks[k]+"|"+destServer;
						System.out.println("Send Message for SCP: "+ message +"\tSCP from: "+serverToSCPFrom);
						sendMessageToHost(serverToSCPFrom,message);

					//}
				}
			}


		}
	}
	
	public static synchronized void sendMessageToHost(String serverName,String message) {
	 
	// System.out.println(" === To Server and message: "+serverName+"\t"+message);
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
		// Message was received from M-server - get all information and store in variable for processing
		String toSendMessage=null;
		String[] S3 = message.split("\\|");
		String msgType = S3[0];
		
		//String serverName=S3[2];
		//Integer offset;
		String msgHeartbeat="heartbeat";
		String msgRead= "r";
		String msgAppend = "a";
		String msgCreate = "w";
	
		for(int countForObj=0;countForObj<25;countForObj++){
			listObj[countForObj]=new ListOfArray();
		}
		//System.out.println("=== Received message ===\t"+msgType+"\t"+chunkName+"\t"+offset);
		
		if(msgRead.equals(msgType)){
		//Incoming message is -" r filename chunknumber sizetoread offet hostname
			String fileName = S3[1];
			String whoIsClient = S3[5];
			String offset = S3[4];
			Integer chunkIndex = Integer.parseInt(S3[2]);
			String chunkName=fileName+"_"+chunkIndex;
			//System.out.println("TempName for chunk is: "+chunkName);
			
			/* Implement way to find one server to perform read operation */

			// Goes here - if we need to send all chunk locations - all servers or any one server 
			//Displaying all servers where a chunk index is available

			//System.out.println("In read - chunk name is: " + chunkName);
			//for(int c =0; c< chunkLocationHash.get(chunkName).listOfChunks.size() ; c++){
				//System.out.println("chunk at server >  "+ chunkName+" : " + chunkLocationHash.get(chunkName).listOfChunks.get(c));


			//}

			String chunkServer0 =  chunkLocationHash.get(chunkName).listOfChunks.get(0); // Temp, at least one server(First)
			String chunkServer1 =  chunkLocationHash.get(chunkName).listOfChunks.get(1);
			String chunkServer2 =  chunkLocationHash.get(chunkName).listOfChunks.get(2);
			String chunkServer="";
			//chunkLocationHash.get(chunkName).listOfChunks.get(chunkIndex);
			//toSendMessage=msgType+"|"+chunkName+"|"+chunkServer+"|"+whoIsClient+"|"+offset+"|"+S3[3];
			for(int k=0;k<listOfServer.size();k++){
				if(serverObjArray[k].serverId.equals(chunkServer0)&&(serverObjArray[k].status==true)){
				//System.out.println("\n\n\n Server is not available : " + serverObjArray[k].serverId );
					chunkServer = chunkServer+chunkServer0+"-";
				}
			}
			for(int k=0;k<listOfServer.size();k++){
				if(serverObjArray[k].serverId.equals(chunkServer1)&&(serverObjArray[k].status==true)){
					chunkServer = chunkServer+chunkServer1+"-";
				}
		
			}
			for(int k=0;k<listOfServer.size();k++){

				if(serverObjArray[k].serverId.equals(chunkServer2)&&(serverObjArray[k].status==true)){
					chunkServer = chunkServer+chunkServer2+"-";
				}

			}
			if(chunkServer!=null){
				String [] allS = chunkServer.split("\\-");
				chunkServer = allS[0];
				toSendMessage=msgType+"|"+chunkName+"|"+chunkServer+"|"+whoIsClient+"|"+offset+"|"+S3[3];
				sendMessageToHost(whoIsClient, toSendMessage);
			}
			else{
				toSendMessage="error|"+chunkServer;
				sendMessageToHost(whoIsClient, toSendMessage);
			}
		}
		else if(msgAppend.equals(msgType)){
			String fileName = S3[1];
			String whoIsClient = S3[2];
			
			String chunkName = fileName+"_"+(hashFileNameAndChunkNumber.get(fileName));
			String [] allServers= new String[3];
			String chunkServer="";
			//Since append should be performed on all three server, get list of all three servers and do an append
			for(int j=0; j<chunkLocationHash.get(chunkName).listOfChunks.size(); j++){
				chunkServer =  chunkLocationHash.get(chunkName).listOfChunks.get(j);
				System.out.println("chunk name and server in append: " + chunkName + " : " + chunkServer);
				allServers[j]=chunkServer;

			//String chunkName = fileName+"_"+(chunkLocationHash.get(fileName).listOfChunks.size()-1);
			//String chunkServer = chunkLocationHash.get(fileName).listOfChunks.get(chunkLocationHash.get(fileName).listOfChunks.size()-1);
			//String chunkServer = 
			//System.out.println("=== Appended on server  ===" + chunkServer);
			}
				String toSendList=allServers[0]+"-"+allServers[1]+"-"+allServers[2];
				String messageToClientToWrite = "a|"+chunkName+"|"+chunkServer+"|"+whoIsClient+"|"+toSendList+"|"+S3[3];
			 	for(int k=0;k<listOfServer.size();k++){
					if(serverObjArray[k].serverId.equals(allServers[0])&&(serverObjArray[k].status==false) || serverObjArray[k].serverId.equals(allServers[1])&&(serverObjArray[k].status==false)|| serverObjArray[k].serverId.equals(allServers[2])&&(serverObjArray[k].status==false)){
				   	// System.out.println("\n\n\n Server is not available : " + serverObjArray[k].serverId );
				    	messageToClientToWrite="error|"+chunkServer;
					    }
			 	}
		
			 	sendMessageToHost(whoIsClient,messageToClientToWrite);
				//sendMessageToHost(S3[3], message);
			//}
			//} 
		}
		else if(msgCreate.equals(msgType)){
		   String fileName = S3[1];
		   String whoIsClient = S3[3];
		   String chunkName=null;
		   String [] allServers= new String[3];
		   String secServer="";
		   String serverForChunk="";
		   //chunkName=fileName+"_"+(chunkLocationHash.get(fileName).listOfChunks.size()-1);
			//To create replicas for each of the chunk, call following codes for NUMBER_OF_REPLICAS

		  //System.out.println("=== Before SOrt hashforFreeSpace:\n" + hashStoreFreeSpace);
		  sortHashMapOnFreeSpace(hashStoreFreeSpace);
		  for(int i=0;i<NUMBER_OF_REPLICAS;i++){
			ServerClass myServerObj = getServerForChunk(i); // Randomly select one of the server objects
			 serverForChunk = myServerObj.serverId; // Server id is the server ip
			//String chunkName=null;
			//System.out.println("To create chunk name is: " + chunkName);
			
			/*
			 * If create or append request is by another client for a file, reject that request
			 * To implement this, we can use a hash where filename can be key and value will be client's name
			 * This hash can be updated when a new file is created
			 * 
			 */
			//if((hashForClient.containsKey(fileName))&&(!hashForClient.get(fileName).equals(whoIsClient))){
			//	String messageToClientToWrite = "error1|"+"Can not take request";
			//	sendMessageToHost(whoIsClient,messageToClientToWrite);
				//break;

			//}else{
			hashForClient.put(fileName,whoIsClient);
			if(hashFileNameAndChunkNumber.containsKey(fileName)){ // This is not first chunk of the file
				//(chunkLocationHash.get(fileName)).listOfChunks.add(serverForChunk);
				if(chunkName == null){
					//chunkName=fileName+"_"+(chunkLocationHash.get(fileName).listOfChunks.size()-1);
					hashFileNameAndChunkNumber.put(fileName,(hashFileNameAndChunkNumber.get(fileName) + 1));
					chunkName = fileName+"_"+(hashFileNameAndChunkNumber.get(fileName));
					//System.out.println("\n\n\nin if - null chunkname " + hashFileNameAndChunkNumber );
				}
				// update array list with other replica server names for current chunk name
				listObj[gCount].listOfChunks.add(serverForChunk);
				// add updated object with arraylist into chunk hash
				chunkLocationHash.put(chunkName,listObj[gCount]);
			}
			else{
			// This is first chunk for this file name, add 1 for the file name into hash and generate chunk name using it
				// For example, file-1's all chunks location will be in first index -> arralist[0]...

				//listObj[gCount].listOfChunks.add(serverForChunk);
				//chunkLocationHash.put(fileName,listObj[gCount]);

				if(chunkName == null){
					//chunkName=fileName+"_"+(chunkLocationHash.get(fileName).listOfChunks.size()-1);
					hashFileNameAndChunkNumber.put(fileName,0);
					chunkName = fileName+"_"+(hashFileNameAndChunkNumber.get(fileName));

				}
				//Add server name to array list for that object	
				listObj[gCount].listOfChunks.add(serverForChunk);
				//Add the address of the object to the hash as value for chunkname key
				chunkLocationHash.put(chunkName,listObj[gCount]);
				//gCount++;
				//System.out.println("-first added to hash: " + chunkLocationHash);

			}
			allServers[i]=serverForChunk;
			//System.out.println("in create: " + chunkLocationHash);
			//chunkLocationHash.put(chunkName, serverForChunk);
			toSendMessage=msgType+"|"+chunkName;
			// a|filename|serverWhichHasChunkFile|DataToAppendToTheFile|ClientNameWhoRequested
			//String messageToClientToWrite = "a|"+chunkName+"|"+serverForChunk+"|"+whoIsClient+"|dummy"+"|"+S3[5]; 
			//System.out.println("To create chunk name is: " + chunkName);
			//System.out.println(System.currentTimeMillis()+": In create: " + toSendMessage);
			sendMessageToHost(serverForChunk, toSendMessage);// ask server to create the chunk
			//sendMessageToHost(whoIsClient,messageToClientToWrite);// send chunk info to client to write data to that chunk file
		  // } // end of else-outer
		  }// End of for loop - NUMBER_OF_REPLICAS 
		  	secServer = allServers[0]+"-"+allServers[1]+"-"+allServers[2];
			String messageToClientToWrite = "a|"+chunkName+"|"+serverForChunk+"|"+whoIsClient+"|"+secServer+"|"+S3[5];
		  sendMessageToHost(whoIsClient,messageToClientToWrite);
		  gCount++;
		}
		else if(msgHeartbeat.equals(msgType)){

			//register the server into ArrayList
			if(!listOfServer.contains(S3[1])){
				listOfServer.add(S3[1]);
				String name = S3[1];
				hashStoreFreeSpace.put(name,100.0);
				ServerClass serverObj = new ServerClass();
				serverObj.serverId=name;
				serverObj.status=true;
				serverObj.numberOfChunk=0;
				serverObjArray[listOfServer.size()-1]=serverObj;
				Long time = System.currentTimeMillis();
				mapServerHeartBeat.put(name,time);
			}
			//update heartbeat received time
			mapServerHeartBeat.put(S3[1],System.currentTimeMillis());
			Integer no_of_chunk = Integer.parseInt(S3[3]);
			hashStoreNumberOfChunk.put(S3[1],no_of_chunk); // update hash with number of chunks received = overwrite
			Double free_space = Double.parseDouble(S3[4]);
			//System.out.println("Received space: "+free_space+" from "+ S3[1]);
			hashStoreFreeSpace.put(S3[1],free_space); // update hash with available space = overwrite
			//String [] chunkmsg = S3[2].split("\\-");

			//Put received list of chunks into chunkmap with key as server and string of chunk lists as value
			if(S3.length>2){ 
				// This means list of files have been received after at leat one creation	
				chunkMap.put(S3[1],S3[2]);
			}
			//System.out.println("In process HeartBeat: " + chunkMap);
			//for(int ck=0;ck<chunkmsg.length;ck++){
				//System.out.println("-> " + chunkmsg[ck]);
				/*if(!chunkMap.containsKey(chunkmsg[ck])){
					if(!(chunkmsg[ck].equals("null"))){
						chunkMap.put(chunkmsg[ck],S3[1]);
					}
				}else{
					Strint temp = chunkMap.get(chunkmsg[ck]);
					temp = 

				}*/

			//}
			//System.out.println(System.currentTimeMillis()+": Heartbeat received: "+S3[1]+ " : " + mapServerHeartBeat.get(S3[1]));

			System.out.println("\n === UPDATED Chunk List ===");
			for (Map.Entry<String, String> entry : chunkMap.entrySet()) {
				String key = entry.getKey().toString();
				System.out.println(key+" ==> \t"+ entry.getValue());
			}
			System.out.println("\n === PERCENTAGE SPACE LEFT ===");
			for (Map.Entry<String, Double> entry : hashStoreFreeSpace.entrySet()) {
				String key = entry.getKey().toString();
				System.out.println(key+" ==> \t"+ entry.getValue());
			}
			System.out.println("\n === NUMBER OF CHUNKS ON EACH SERVER ===");
			for (Map.Entry<String, Integer> entry : hashStoreNumberOfChunk.entrySet()) {
				String key = entry.getKey().toString();
				System.out.println(key+" ==> \t"+ entry.getValue());
			}
			//System.out.println("\n === PERCENTAGE OF FREE SPACE AT EACH FILE SERVER ===\n"+ hashStoreFreeSpace);
			//System.out.println("\n === NUMBER OF CHUNKS ON EACH SERVER ===\n"+ hashStoreNumberOfChunk);
			System.out.println("\n");

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
				listOfOperation.add(sc.nextLine());
		        
			}
		}catch(IOException e){
			System.out.println(e);
		}
		sc.close();
		//file.close();
		return listOfOperation;        
	}
	
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		//String filePath = args[0];
		//readFile(filePath);
		getMyIP();
		/*
		listOfServer.add("10.176.66.54");
		listOfServer.add("10.176.66.55");
		listOfServer.add("10.176.66.56");
		listOfServer.add("10.176.66.57");
		*/
		//Start the server to listen to incoming messages or Acknowledgments of requests
		StartServerThread(8025);
		//Start Client Operations
		System.out.println("This is Master Server - so operation for Master");
		
		/*Read about the operation - create, read or append a file - from input file */
		ArrayList<String> tempListOfServer = new ArrayList<String>();
		//listOfServer=readFile(filePath);
		

		//String MetaServer ="192.168.1.138";
		
		//ServerClass [] serverObjArray=new ServerClass[25];
		/*
		for(int i=0;i<listOfServer.size();i++){
			String name = listOfServer.get(i);
			hashStoreFreeSpace.put(name,100.0); // Initially all servers have 100% available space
			System.out.println("Name: " +name+" = "+ listOfServer.get(i) );
			ServerClass serverObj = new ServerClass();
			//serverObj=new ServerClass();
			serverObj.serverId=name;
			serverObj.numberOfChunk=0;
			serverObj.status=false; // initial status of server is false until at least one heartbeat is received
			serverObj.started=false;
			serverObjArray[i]=serverObj;
			//System.out.println("At object: "+ serverObjArray[i].serverId +"|"+serverObjArray[i].numberOfChunk+"|"+serverObjArray[i].status) ;
			//MetaMainClass (name+"a") = new MetaMainClass();
			Long time = System.currentTimeMillis();
			mapServerHeartBeat.put(name,time);
		}*/
		MonitorHeartBeat mhbt = new MonitorHeartBeat();
		Thread t = new Thread(mhbt);
		t.start();
		
		System.out.println(" === Started everything on MASTER SERVER ===");
		
		
		
	}

}
