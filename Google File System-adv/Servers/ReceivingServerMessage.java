import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.io.DataOutputStream;


public class ReceivingServerMessage implements Runnable {
	
	Socket sockId;
	String comMessage = "compute";
	String termMessage = "terminate";
	String ackMessage = "ACK";
	String S1,S2;
	

	public ReceivingServerMessage(Socket server) {
		// TODO Auto-generated constructor stub
		sockId = server;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		String rcvdM;
		//System.out.println("in Received MSG: ");
		InputStreamReader in;
		try {
			in = new InputStreamReader(sockId.getInputStream());
			BufferedReader reader = new BufferedReader(in);
			rcvdM = reader.readLine();
			ServerMainClass.ProcessData(rcvdM);
			String []  tmp1 = rcvdM.split("\\|");
			if(tmp1[0].equals("ia") || tmp1[0].equals("commit")){
				DataOutputStream outToClient = new DataOutputStream(sockId.getOutputStream());			
				outToClient.writeBytes("Ack\n");
				outToClient.close();
			}
			else{
			//	System.out.println("In else");
			}

			} catch (Exception e) {
				e.printStackTrace();
		}
		

	}

}
