package app;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
	private final List<Integer> initiatorsList;
	private final Map<Integer,Integer> initiatorVersionMap;
	
	public ServentInfo(String ipAddress, int id, int listenerPort, List<Integer> neighbors) {
		this.ipAddress = ipAddress;
		this.listenerPort = listenerPort;
		this.id = id;
		this.neighbors = neighbors;
		this.initiatorVersionMap= new ConcurrentHashMap<>();
		this.initiatorsList = new CopyOnWriteArrayList<>();

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
	public Map<Integer,Integer> getInitiatorVersionMap(){
		return initiatorVersionMap;
	}

	public List<Integer> getInitiatorsList() {
		return initiatorsList;
	}

	public boolean addInitiator(int initiatorId) {
		if(initiatorsList.contains(initiatorId)) {
			return false;
		}
		initiatorsList.add(initiatorId);
		//pocetna verzija je 0 treba mi provera negde sqamo
        initiatorVersionMap.putIfAbsent(initiatorId, 0);
		return true;
	}
	
	@Override
	public String toString() {
		return "[" + id + "|" + ipAddress + "|" + listenerPort + "]";
	}
}
