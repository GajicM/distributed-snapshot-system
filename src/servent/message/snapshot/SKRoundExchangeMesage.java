package servent.message.snapshot;

import app.ServentInfo;
import app.snapshot_bitcake.LYSnapshotResult;
import app.snapshot_bitcake.SnapshotVersionId;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.util.Map;
import java.util.Set;

public class SKRoundExchangeMesage extends BasicMessage {
    private Set<Integer> regionList;
    private Map<Integer, LYSnapshotResult> resultsMap;
    public SKRoundExchangeMesage(ServentInfo originalSenderInfo, ServentInfo receiverInfo, SnapshotVersionId versionVector, Map<Integer, LYSnapshotResult> resultsMap, Set<Integer> regionList) {
        super(MessageType.SK_ROUND_EXCHANGE, originalSenderInfo, receiverInfo,versionVector);
        this.regionList=regionList;
        this.resultsMap=resultsMap;
    }

    public Set<Integer> getRegionList() {
        return regionList;
    }

    public Map<Integer, LYSnapshotResult> getResultsMap() {
        return resultsMap;
    }
}
