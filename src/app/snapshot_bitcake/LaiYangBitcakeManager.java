package app.snapshot_bitcake;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import app.AppConfig;
import servent.message.Message;
import servent.message.snapshot.LYMarkerMessage;
import servent.message.snapshot.LYTellMessage;
import servent.message.util.MessageUtil;

public class LaiYangBitcakeManager implements BitcakeManager {

	private final AtomicInteger currentAmount = new AtomicInteger(1000);
	
	public void takeSomeBitcakes(int amount) {
		currentAmount.getAndAdd(-amount);
	}
	
	public void addSomeBitcakes(int amount) {
	//	AppConfig.timestampedErrorPrint("Adding bitcakes: " + amount+ " now have"+currentAmount.get());
		currentAmount.getAndAdd(amount);
	}
	
	public int getCurrentBitcakeAmount() {
		return currentAmount.get();
	}

	@Override
	public void setVersionInHistory(SnapshotVersionId version) {
	//	giveHistory=new ConcurrentHashMap<>();
	//	getHistory=new ConcurrentHashMap<>();

	//	AppConfig.timestampedErrorPrint("---Resetting version: " + version+" where \ngetHistory: "+getHistory+"\ngiveHistory: "+giveHistory+"\n---------");
		Map<Integer,Integer> giveHistoryInMap = new ConcurrentHashMap<>();
		Map<Integer,Integer> getHistoryInMap = new ConcurrentHashMap<>();
		for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
			giveHistoryInMap.put(neighbor, 0);
			getHistoryInMap.put(neighbor, 0);
		}
		if(!giveHistory.containsKey(version)){
			giveHistory.put(version, giveHistoryInMap);
		}
		if(!getHistory.containsKey(version)){
			getHistory.put(version, getHistoryInMap);
		}


	}

	private Map<SnapshotVersionId, Map<Integer, Integer>> giveHistory = new ConcurrentHashMap<>();
	private Map<SnapshotVersionId,Map<Integer, Integer>> getHistory = new ConcurrentHashMap<>();
	
	public LaiYangBitcakeManager() {
		setVersionInHistory(new SnapshotVersionId(0,-1));
	}

	
	/*
	 * This value is protected by AppConfig.colorLock.
	 * Access it only if you have the blessing.
	 */
	public int recordedAmount = 0;
	
	public void markerEvent(int collectorId, SnapshotCollector snapshotCollector,Integer snapshotVersionId,int parentId) {

		synchronized (AppConfig.versionLock) {
			((SnapshotCollectorWorker)snapshotCollector).waitingForNewSnapshot.set(false);
			AppConfig.timestampedErrorPrint("Marker event for: " + collectorId + " initiated by " + AppConfig.myServentInfo.getId()+ " recordedamount: "+recordedAmount +"  getHistory: "+getHistory);
			AppConfig.timestampedErrorPrint("regionList: "+((SnapshotCollectorWorker) snapshotCollector).regionList +"\n colectedvalues "+((SnapshotCollectorWorker) snapshotCollector).collectedLYValues);
			Map giveHistoryForSnapshot=Map.of(new SnapshotVersionId(snapshotVersionId-1,snapshotVersionId-1==0?-1:collectorId), new HashMap<>(giveHistory.get(new SnapshotVersionId(snapshotVersionId-1))));
			Map getHistoryForSnapshot= Map.of(new SnapshotVersionId(snapshotVersionId-1,snapshotVersionId-1==0?-1:collectorId),new HashMap<>(getHistory.get(new SnapshotVersionId(snapshotVersionId-1))));
			AppConfig.timestampedErrorPrint("give history "+giveHistory+ "\n");
			AppConfig.timestampedErrorPrint("get history "+getHistory+ "\n");
			AppConfig.version.setVersionId(snapshotVersionId);
			AppConfig.version.setInitiatorId(collectorId);
			//moguce je da postoji novija verzija u get history-ju, ali ne i u give history-u, kako bih video pravi amount koji je vezan samo za snapshotVerziju moram da oduzmem ove vrednosti
			recordedAmount = getCurrentBitcakeAmount();
			if(getHistory.containsKey(new SnapshotVersionId(snapshotVersionId))){
				Collection<Integer> ints=getHistory.get(new SnapshotVersionId(snapshotVersionId))	.values();
				for(Integer recordedGet:ints){
					recordedAmount-=recordedGet;
				}
				AppConfig.timestampedErrorPrint("I dalje u ifu recordedamount = " + recordedAmount);
			}
			setVersionInHistory(AppConfig.version);


			LYSnapshotResult snapshotResult = new LYSnapshotResult(
					AppConfig.myServentInfo.getId(), recordedAmount, giveHistoryForSnapshot,getHistoryForSnapshot);
				AppConfig.timestampedErrorPrint("\nSnapshot result: " + snapshotResult+"\n");
			snapshotCollector.addLYSnapshotInfo(
					AppConfig.myServentInfo.getId(),
					snapshotResult);
			if (collectorId == AppConfig.myServentInfo.getId()) {
				Thread predator=new Thread(new ChildResponseAwaiterThread((SnapshotCollectorWorker) snapshotCollector,collectorId,snapshotVersionId));
				predator.start();
				AppConfig.parentId.set(parentId);
			}
			else
			{
				Thread predator=new Thread(new ChildResponseAwaiterThread((SnapshotCollectorWorker) snapshotCollector,collectorId,snapshotVersionId));
				predator.start();
			}
			
			for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
				if(neighbor==AppConfig.parentId.get())
					continue;
				Message clMarker = new LYMarkerMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighbor), collectorId,AppConfig.version);
				MessageUtil.sendMessage(clMarker);
				try {
					/*
					 * This sleep is here to artificially produce some white node -> red node messages.
					 * Not actually recommended, as we are sleeping while we have colorLock.
					 */
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private class MapValueUpdater implements BiFunction<Integer, Integer, Integer> {
		
		private int valueToAdd;
		
		public MapValueUpdater(int valueToAdd) {
			this.valueToAdd = valueToAdd;
		}
		
		@Override
		public Integer apply(Integer key, Integer oldValue) {
			return oldValue + valueToAdd;
		}
	}
	
	public void recordGiveTransaction(int version,int neighbor, int amount) {
		for(SnapshotVersionId snapshotVersionId : giveHistory.keySet()) {
			if(snapshotVersionId.getVersionId() == version) {
				giveHistory.get(snapshotVersionId).compute(neighbor, new MapValueUpdater(amount));

			}
		}

	}
	
	public void recordGetTransaction(int version,int neighbor, int amount) {
		if(!getHistory.containsKey(new SnapshotVersionId(version))){
			setVersionInHistory(new SnapshotVersionId(version));
		}
		for(SnapshotVersionId snapshotVersionId : getHistory.keySet()) {
			if(snapshotVersionId.getVersionId() == version) {
				getHistory.get(snapshotVersionId).compute(neighbor, new MapValueUpdater(amount));

			}
		}

	}
}
