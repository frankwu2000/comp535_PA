package socs.network.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

import socs.network.message.SOSPFPacket;

public class Server extends Thread {
	private ServerSocket serverSocket;
	private Router self;
	
	public Server(int port, Router myself) throws IOException
	{
		self = myself;
		serverSocket = new ServerSocket(port);
		System.out.printf("Server socket opened on %s\n", serverSocket.toString());
	}
	
	public void run()
	{
		while (true)
		{
			try
			{
				Socket server = serverSocket.accept();

				int linkPort = 0;
				
				ObjectInputStream in = new ObjectInputStream(server.getInputStream());
				ObjectOutputStream out = new ObjectOutputStream(server.getOutputStream());
				
				SOSPFPacket packet = new SOSPFPacket();
				packet = (SOSPFPacket) in.readObject();
				
				if (packet.sospfType == 0)
				{
					for (int i = 0; i < self.ports.length; ++i)
					{
						if (self.ports[i] == null)
						{
							RouterDescription source = new RouterDescription();
							source.processIPAddress = packet.srcProcessIP;
							source.processPortNumber = packet.srcProcessPort;
							source.simulatedIPAddress = packet.srcIP;
							self.ports[i] = new Link(self.rd,source);
							linkPort = i;
							break;
						}
					}
					System.out.println("received HELLO from " + packet.srcIP + ";");
					self.ports[linkPort].router2.status = RouterStatus.INIT;
					System.out.println("set " + packet.srcIP + " state to INIT");
				}
				
				packet.srcProcessIP = self.rd.processIPAddress;
				packet.srcProcessPort = self.rd.processPortNumber;
				packet.dstIP = packet.srcIP;
				packet.srcIP = self.rd.simulatedIPAddress;
				packet.sospfType = 0;
				packet.neighborID = packet.routerID;
				packet.routerID = self.rd.simulatedIPAddress;
				
				out.writeObject(packet);
				
				packet = (SOSPFPacket) in.readObject();
				if (packet.sospfType == 0)
				{
					System.out.println("received HELLO from " + packet.srcIP + ";");
					self.ports[linkPort].router2.status = RouterStatus.TWO_WAY;
					System.out.println("set " + packet.srcIP + " state to TWO_WAY");
				}
				
				self.ports[linkPort] = null;
			}
			catch(IOException e)
			{
				e.printStackTrace();
				break;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}
