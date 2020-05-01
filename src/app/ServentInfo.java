package app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is an immutable class that holds all the information for a servent.
 *
 * @author bmilojkovic
 */
public class ServentInfo implements Serializable {

	private static final long serialVersionUID = 5304170042791281555L;
	private final int id;
	private final String ipAddress;
	private final int listenerPort;
	private final List<Integer> neighbors;
	
	//TODO
	private final List<Integer> initiators;	
	private  int parentId = -1;
	private  int myCollectorId = -1;
	
	Set<Integer> otherCollectors = new HashSet<Integer>(); 
	
	public ServentInfo(String ipAddress, int id, int listenerPort, List<Integer> neighbors, List<Integer> initiators) {
		this.ipAddress = ipAddress;
		this.listenerPort = listenerPort;
		this.id = id;
		this.neighbors = neighbors;
		this.initiators = initiators;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public int getListenerPort() {
		return listenerPort;
	}

	public int getId() {
		return id;
	}
	
	public List<Integer> getNeighbors() {
		return neighbors;
	}
	
	@Override
	public String toString() {
		return "[" + id + "|" + ipAddress + "|" + listenerPort + "]";
	}

	public List<Integer> getInitiators() {
		return initiators;
	}
	
	public  int getParentId() {
		return parentId;
	}

	public void setParentId(int parentId) {
		this.parentId = parentId;
	}

	public  int getMyCollectorId() {
		return myCollectorId;
	}

	public void setMyCollectorId(int myCollectorId) {
		this.myCollectorId = myCollectorId;
	}
	
	
	public Set<Integer> getOtherCollectors() {
		return otherCollectors;
	}

	public void setOtherCollectors(Set<Integer> otherCollectors) {
		this.otherCollectors = otherCollectors;
	}

	public void clearEverything() {
		parentId = -1;
		myCollectorId = -1;	
	}
	
	
}
