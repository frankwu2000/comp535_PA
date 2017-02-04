package socs.network.node;

import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public class Router {

  protected LinkStateDatabase lsd;

  RouterDescription rd = new RouterDescription();

  //assuming that all routers are with 4 ports
  Link[] ports = new Link[4];

  public Router(Configuration config) {
	  rd.simulatedIPAddress = config.getString("socs.network.router.ip");
	  lsd = new LinkStateDatabase(rd);
	  try
	  {
		  Thread t = new Server(8000, this);
		  t.start();
	  }
	  catch(IOException e)
	  {
		  e.printStackTrace();
	  }
  }

  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address  -> ip address -> ... -> destination ip
   *
   * @param destinationIP the ip adderss of the destination simulated router
   */
  private void processDetect(String destinationIP) {
	  
  }

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber the port number which the link attaches at
   */
  private void processDisconnect(short portNumber) {

  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to identify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  private void processAttach(String processIP, short processPort,
                             String simulatedIP, short weight) {	  
	  	for (int i = 0; i < ports.length; ++i)
	  	{
	  		if (ports[i] == null)
	  		{
	  			RouterDescription r2 = new RouterDescription();
	  			r2.processIPAddress = processIP;
	  			r2.processPortNumber = processPort;
	  			r2.simulatedIPAddress = simulatedIP;
	  			ports[i] = new Link(rd,r2);
	  			break;
	  		}
	  	}
  }

  /**
   * broadcast Hello to neighbors
   */
  private void processStart() {
	  	for (int i = 0; i < ports.length; ++i)
	  	{
	  		if (ports[i] != null)
	  		{
	  			try
	  			{
	  				Socket clientSocket = new Socket(ports[i].router2.processIPAddress,ports[i].router2.processPortNumber);
					
					ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
					ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
					
					SOSPFPacket packet = new SOSPFPacket();
					packet.srcProcessIP = rd.processIPAddress;
					packet.srcProcessPort = rd.processPortNumber;
					packet.srcIP = rd.simulatedIPAddress;
					packet.dstIP = ports[i].router2.simulatedIPAddress;
					packet.sospfType = 0;
					packet.routerID = rd.simulatedIPAddress;
					packet.neighborID = ports[i].router2.simulatedIPAddress;
					
					out.writeObject(packet);
					
					try {
						packet = (SOSPFPacket) in.readObject();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
					
					if (packet.sospfType == 0)
					{
						System.out.println("received HELLO from " + packet.srcIP + ";");
						ports[i].router2.status = RouterStatus.TWO_WAY;
						System.out.println("set " + packet.srcIP + " state to TWO_WAY");
					}
					
					packet.srcProcessIP = rd.processIPAddress;
					packet.srcProcessPort = rd.processPortNumber;
					packet.srcIP = rd.simulatedIPAddress;
					packet.dstIP = ports[i].router2.simulatedIPAddress;
					packet.sospfType = 0;
					packet.routerID = rd.simulatedIPAddress;
					packet.neighborID = ports[i].router2.simulatedIPAddress;
					
					out.writeObject(packet);
					
		  			clientSocket.close();
				}
	  			catch (IOException e)
	  			{
					e.printStackTrace();
				}
	  		}
	  	}
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to identify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * This command does trigger the link database synchronization
   */
  private void processConnect(String processIP, short processPort,
                              String simulatedIP, short weight) {
	  
  }

  /**
   * output the neighbors of the routers
   */
  private void processNeighbors() {
	  for (int i = 0; i < ports.length; i++)
	  {
		  if (ports[i] != null)
		  {
			  System.out.printf("IP Address of neighbor %d: %s\n", i+1, ports[i].router2.simulatedIPAddress);
		  }
	  }
  }

  /**
   * disconnect with all neighbors and quit the program
   */
  private void processQuit() {

  }

  public void terminal() {
    try {
      InputStreamReader isReader = new InputStreamReader(System.in);
      BufferedReader br = new BufferedReader(isReader);
      System.out.print(">> ");
      String command = br.readLine();
      while (true) {
        if (command.startsWith("detect ")) {
          String[] cmdLine = command.split(" ");
          processDetect(cmdLine[1]);
        } else if (command.startsWith("disconnect ")) {
          String[] cmdLine = command.split(" ");
          processDisconnect(Short.parseShort(cmdLine[1]));
        } else if (command.startsWith("quit")) {
          processQuit();
        } else if (command.startsWith("attach ")) {
          String[] cmdLine = command.split(" ");
          processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("start")) {
          processStart();
        } else if (command.equals("connect ")) {
          String[] cmdLine = command.split(" ");
          processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("neighbors")) {
          //output neighbors
          processNeighbors();
        } else {
          //invalid command
          break;
        }
        System.out.print(">> ");
        command = br.readLine();
      }
      isReader.close();
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
