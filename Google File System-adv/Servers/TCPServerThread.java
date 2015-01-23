import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;


public class TCPServerThread implements Runnable {

	int port;
	protected ServerSocket serverSocket;
	
	public TCPServerThread(int port) {
		// TODO Auto-generated constructor stub
		this.port = port;
	}

	
	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(port);
			serverSocket.setSoTimeout(590000);
			//System.out.println(" => " + getRemoteSocketAddress());
			while(true)
			{
				try
				{
					Socket server = serverSocket.accept();
					//System.out.println(" => " + server.getRemoteSocketAddress());
					new Thread(new ReceivingServerMessage(server)).start();
					//DataOutputStream outToClient = new DataOutputStream(server.getOutputStream());
					//outToClient.writeBytes("Ack\n");
					//new Thread(new ReceivingServerMessage(server)).start();

				} catch(SocketTimeoutException s) {
					System.out.println("Socket timed out on server-1!");
					break;
				} catch(IOException e) {
					e.printStackTrace();
					break;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
