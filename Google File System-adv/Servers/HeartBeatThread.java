import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;


public class HeartBeatThread implements Runnable{

	int port;
	public HeartBeatThread(int port){
		port=this.port;
	}
	
	public void run(){
		String META_SERVER="10.176.66.81";
		String chunkListToMaster="";
		while(true){
			//System.out.println("=== Sending hearbeat to Meta-server: "+System.currentTimeMillis());

			try{
				Socket client = new Socket(META_SERVER, 8025);                                                                                                                          //System.out.println("Just connected to " + client.getRemoteSocketAddress());
				PrintWriter out = new PrintWriter(client.getOutputStream(), true);
				//send message to master server
				//heartbeat|hostname|chunklist|no_of_chunks|freespace
				ServerMainClass.getMyFiles();//Update list of file which is there usinf ls command
				out.println("heartbeat|"+ServerMainClass.hostname+"|"+ServerMainClass.chunkList+"|"+ServerMainClass.no_of_chunks_hosted+"|"+ServerMainClass.percentLeft);
				//System.out.println("Sleeping for 5 seconds before sending next heartbeat\n"+ ServerMainClass.percentLeft);
				Thread.sleep(5000);

				
			} catch(IOException e){
				e.printStackTrace();
			}
		
			catch(InterruptedException i){
				System.out.println(i);
			}
		}
		
	}
}
