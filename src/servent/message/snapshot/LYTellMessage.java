package servent.message.snapshot;

import java.util.List;
import java.util.Map;
import java.util.Set;

import app.ServentInfo;
import app.snapshot_bitcake.LYSnapshotResult;
import app.snapshot_bitcake.SnapshotVersionId;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

public class LYTellMessage extends BasicMessage {

	private static final long serialVersionUID = 3116394054726162318L;

//	private LYSnapshotResult lySnapshotResult;
	private Set<Integer> regionList;
	private Map<Integer,LYSnapshotResult> resultsMap;

	public LYTellMessage(ServentInfo sender, ServentInfo receiver, Map<Integer,LYSnapshotResult> resultsMap, SnapshotVersionId versionVector, Set<Integer> regionList) {
		super(MessageType.LY_TELL, sender, receiver,versionVector);
		this.resultsMap = resultsMap;
		this.regionList=regionList;
	}


	public Set<Integer> getRegionList() {
		return regionList;
	}

	public Map<Integer, LYSnapshotResult> getResultsMap() {
		return resultsMap;
	}
}
