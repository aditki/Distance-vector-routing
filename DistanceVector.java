/*
 * Vamshedhar Reddy Chintala (800988045)
 * Sai Aditya Varma Mudunuri (800986990)
 *
 * Distance vector class for routing table entry
 * It contains shortest distance to destination and next hop on the path to the destination
 */
public class DistanceVector {
	public double distance;
	public String next;
	
	// Initialize with default values
	public DistanceVector() {
		this.distance = Double.POSITIVE_INFINITY;
		this.next = null;
	}
	
	// Initialize with given values
	public DistanceVector(double distance, String next) {
		this.distance = distance;
		this.next = next;
	}
}
