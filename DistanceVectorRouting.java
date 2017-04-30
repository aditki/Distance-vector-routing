/*
 * Vamshedhar Reddy Chintala (800988045)
 * Sai Aditya Varma Mudunuri (800986990)
 */

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

public class DistanceVectorRouting {
	
	/*
	 * currentNode 	-	Current Router Node 
	 * iteration 	-	Count of number of iterations
	 * TIMEOUT 		-	time out after which router re broadcast its routing table
	 */
	public NetworkNode currentNode;
	public int iteration;
	public static long TIMEOUT = 10000;

	/*
	 * Initialize the router
	 */
	public DistanceVectorRouting(NetworkNode currentNode) {
		this.currentNode = currentNode;
		this.iteration = 1;
	}
	
	public static void main(String[] args) throws Exception {


		// Validate arguments 
		if (args.length != 1) {
            System.out.println("Invalid Format!");
            System.out.println("Expected Format: java Sender <filename or filepath>");
            return;
        }


		String filename = args[0];
		
		// Get node name from file
		String currentNodeName = filename.split(".txt")[0];
		
		// Create network node object
		NetworkNode currentNode = new NetworkNode(currentNodeName);
		
		// Initialize the routing algorithm
		DistanceVectorRouting DVR = new DistanceVectorRouting(currentNode);

		// Start timer to broadcast signal
		long startTime = System.currentTimeMillis();
		
		try {

			/*
			 * Generate router port number from node name
			 * take 8000 as base and add ASCII value of the current node name to get port number at which the router runs
			 */
			int currentPort = 8000 + (int) currentNodeName.charAt(0);

			// Initialize Socket
			DatagramSocket clientSocket = new DatagramSocket(currentPort);
			
			InetAddress IPAddress = InetAddress.getByName("localhost");
			

			while(true){

				// Read data when the routing table has changed
				if(DVR.currentNode.routingTableChanged){

					try{
						// Read routing information data from the file every time before broadcasting
						BufferedReader reader = new BufferedReader(new FileReader(filename));
						
						String line = reader.readLine();

						// HashMap to store neighbors information from the file
						HashMap<String, Double> neighbors = new HashMap<String, Double>();

						while((line = reader.readLine()) != null) {
							
							StringTokenizer st = new StringTokenizer(line);
							
							if(st.countTokens() != 2){
								System.err.println("Invalid data!");
								reader.close();
								return;
							}
							
							String neighbourNodeName = st.nextToken().trim();
							Double weight = Double.parseDouble(st.nextToken().trim());

							neighbors.put(neighbourNodeName, weight);
						}
						reader.close();
						
						// Set of all old neighbors and new neighbors
						// To know if any nodes where added or deleted
						Set<String> newNeighbors = new HashSet<String>();
						newNeighbors.addAll(neighbors.keySet());
						newNeighbors.addAll(DVR.currentNode.neighbors.keySet());

						// For all the new neighbors update the routing table
						for(String neighbourNodeName : newNeighbors){
							Double weight = neighbors.get(neighbourNodeName);
							
							// If am old neighbors information does not exist in new neighbors list, then it was deleted
							// Set its weight to INFINITY
							if(weight == null){
								weight = Double.POSITIVE_INFINITY;
							}
							
							DVR.currentNode.addNeighbourNode(neighbourNodeName, weight, neighbourNodeName, DVR.iteration == 1);
						}
						
					} catch(FileNotFoundException e){
						System.err.println("File Not Found!");
						return;
					} catch(NumberFormatException e){
						System.err.println("Invalid Data!");
						return;
					} catch(IOException e){
						System.err.println("Invalid Data!");
						return;
					}

					// Prints the information thats transmitted to the neighbors
					System.out.println("Sending data to neighbors: Iteration " + DVR.iteration++);

					System.out.println(DVR.currentNode);

					System.out.println();
					
					// Transmit the information to all its neighbors
					for(String neighbour : DVR.currentNode.neighbors.keySet()){

						// do not broadcast to links which are down 
						if(DVR.currentNode.neighbors.get(neighbour) == Double.POSITIVE_INFINITY){
							continue;
						}

						String routingTable = currentNode.getRoutingTableString(neighbour);
					
						byte[] data = routingTable.getBytes();
						// calculating receivers port number
						int receiverPort = 8000 + (int) neighbour.charAt(0);
						
						// Broadcast data
						DatagramPacket dataPacket = new DatagramPacket(data, data.length, IPAddress, receiverPort);
						clientSocket.send(dataPacket);
					}
					
					// Set routing table changed to false
					DVR.currentNode.reset();
				}

				try{

					// Calculate remaining time to re-broadcast the routing table
					long TIMER = TIMEOUT - (System.currentTimeMillis() - startTime);

					// if timeout raise timeout exception
					if (TIMER < 0) {
						throw new SocketTimeoutException();
					}

					// Wait for packets broadcasted to router
					byte[] receivedPacket = new byte[1024];
					DatagramPacket receiveDatagramPacket = new DatagramPacket(receivedPacket, receivedPacket.length);

					clientSocket.setSoTimeout((int) TIMER);
					
					// Receive packets broadcasted to router
					clientSocket.receive(receiveDatagramPacket);
					byte[] receiveData = receiveDatagramPacket.getData();
					
					// Create a dummy received node with name and routing table
					// Note here routing table doesn't have nextHop info
					NetworkNode receivedNode = new NetworkNode(receiveData);
					
					// for all entries of routing table received
					for(String node : receivedNode.RoutingTable.keySet()){

						DistanceVector tableEntry = DVR.currentNode.RoutingTable.get(node);

						Double actualDistance = Double.POSITIVE_INFINITY;
						String nextHop = null;

						if(tableEntry != null){
							actualDistance = tableEntry.distance;
							nextHop = tableEntry.next;
						}
						
						// calculate and update distance using Bellman Ford algorithm
						Double calculatedDistance = receivedNode.RoutingTable.get(node).distance + DVR.currentNode.neighbors.get(receivedNode.name);


						if(actualDistance > calculatedDistance){
							// update if new distance is less than calculated distance
							DVR.currentNode.updateRoutingTableEntry(node, calculatedDistance, receivedNode.name);
						} else if(nextHop != null && nextHop.equals(receivedNode.name) && !actualDistance.equals(calculatedDistance)){
							// Update if weight till next hop has changed
							DVR.currentNode.updateRoutingTableEntry(node, calculatedDistance, receivedNode.name);
						}
					}
				} catch(SocketTimeoutException e){
					// On time out set routing table changed and restart timer
					DVR.currentNode.routingTableChanged = true;
					startTime = System.currentTimeMillis();
				}
				
				
			}
			
			
			
		} catch (SocketException e) {
			e.printStackTrace();
		}  catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
	}

}
