/*
 * Vamshedhar Reddy Chintala (800988045)
 * Sai Aditya Varma Mudunuri (800986990)
 */

import java.util.HashMap;


/*
 * Network Node class to define nodes and information it stores
 * Stores name, neighbors information and routing table
 */
public class NetworkNode {
	public String name;
	public HashMap<String, Double> neighbors;
	public HashMap<String, DistanceVector> RoutingTable;
	public Boolean routingTableChanged;
	
	// Initialize Node
	public NetworkNode(String name) {
		this.name = name;
		this.routingTableChanged = true;
		this.neighbors = new HashMap<String, Double>();
		this.RoutingTable = new HashMap<String, DistanceVector>();
	}
	
	// Initialize dummy Node from routing table data received at the node
	public NetworkNode(byte[] receiveData){

		String receivedData = new String(receiveData);

		String[] data = receivedData.split(",");

		this.routingTableChanged = true;
		
		this.name = data[0].trim();
		
		this.RoutingTable = new HashMap<String, DistanceVector>();
		
		DistanceVector vector = new DistanceVector();
		vector.distance = 0.0;
		this.RoutingTable.put(this.name, vector);
		
		for(int i = 1; i < data.length; i++){
			String[] tableEntry = data[i].split(":");
			Double distance = Double.parseDouble(tableEntry[1].trim());
			DistanceVector DV = new DistanceVector();
			DV.distance = distance;
			
			this.RoutingTable.put(tableEntry[0].trim(), DV);
		}
	}
	
	// Set routing table changed to false
	public void reset(){
		this.routingTableChanged = false;
	}
	
	// Add new neighbour. Adds it to neighbors map and routing table
	public void addNeighbourNode(String forNode, double distance, String nextHop, Boolean create){
		this.routingTableChanged = true;

		// if new neighbour set previous distance to Infinity
		double previousDistance = Double.POSITIVE_INFINITY;

		if(this.neighbors.get(forNode) != null){
			previousDistance = this.neighbors.get(forNode);
		}

		this.neighbors.put(forNode, distance);

		// Update routing table only on create or first read of the routing table
		if(create || previousDistance != distance){
			this.updateRoutingTableEntry(forNode, distance, nextHop);
		}
	}
	

	// Updates routing table with new value
	public void updateRoutingTableEntry(String forNode, double distance, String nextHop){
		this.routingTableChanged = true;
		
		DistanceVector vector = this.RoutingTable.get(forNode);

		// if entry exists in routing table update it. else create it
		if(vector != null){
			vector.distance = distance;
			vector.next = nextHop;
		} else{
			this.RoutingTable.put(forNode, new DistanceVector(distance, nextHop));
		}
		
	}
	
	// Convert routing table info to String of required format to broadcast to neighbors
	public String getRoutingTableString(String avoid){
		
		String data = "";
		
		for(String node : this.RoutingTable.keySet()){
			
			if(node.equals(this.name)){
				continue;
			}
			
			DistanceVector DV = this.RoutingTable.get(node);
			if(!DV.next.equals(avoid)){
				data += node + ":" + DV.distance + ",";
			}
		}

		if(data.equals("")){
			return this.name;
		}
		
		return this.name + "," + data.substring(0, data.length() - 1);
	}

	// Print routing info while broadcasting
	@Override
	public String toString() {
		
		String output = "";
		for(String node : RoutingTable.keySet()){
			if(node.equals(this.name)){
				continue;
			}
			
			DistanceVector DV = this.RoutingTable.get(node);
			output += "shortest path " + this.name + "-" + node + ": the next hop is " + DV.next + " and the cost is " + DV.distance + "\n";
			// output += this.name + "-" + node + ": " + DV.next + " and " + DV.distance + "\n";

		}
		
		return output;
	}
	
	
}
