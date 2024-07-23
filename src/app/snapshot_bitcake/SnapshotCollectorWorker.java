package app.snapshot_bitcake;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import app.AppConfig;
import servent.message.Message;
import servent.message.snapshot.LYTellMessage;
import servent.message.snapshot.SKRoundExchangeMesage;
import servent.message.util.MessageUtil;

/**
 * Main snapshot collector class. Has support for Naive, Chandy-Lamport
 * and Lai-Yang snapshot algorithms.
 * 
 * @author bmilojkovic
 *
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

	private volatile boolean working = true;
	
	private AtomicBoolean collecting = new AtomicBoolean(false);

	protected Map<Integer, LYSnapshotResult> collectedLYValues = new HashMap<>();
	
	private BitcakeManager bitcakeManager;
	private SnapshotVersionId version = new SnapshotVersionId(0,AppConfig.myServentInfo.getId());
	protected Set<Integer> regionList; //discoveredRegions
	protected List<Integer> neighborsFromDifferentRegion;
	public final AtomicBoolean waiting=new AtomicBoolean(true);


	public Map<Integer,LYSnapshotResult> roundExchangeValues=new HashMap<>();
	public Set<Integer> differentRegionList=new HashSet<>();
	public boolean updated=false;


	public SnapshotCollectorWorker() {
		bitcakeManager = new LaiYangBitcakeManager();
		regionList=new HashSet<>();
		neighborsFromDifferentRegion=new ArrayList<>();

	}
	
	@Override
	public BitcakeManager getBitcakeManager() {
		return bitcakeManager;
	}
	AtomicBoolean waitingForNewSnapshot=new AtomicBoolean(true);
	@Override
	public void run() {
		while(working) {
			
			/*
			 * Not collecting yet - just sleep until we start actual work, or finish
			 */
			while (collecting.get() == false) {
				if(waitingForNewSnapshot.get()){
					dontSaveAnything();
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}
			
			/*
			 * Collecting is done in three stages:
			 * 1. Send messages asking for values
			 * 2. Wait for all the responses
			 * 3. Print result
			 */
			
			//1 send asks

			((LaiYangBitcakeManager)bitcakeManager).markerEvent(AppConfig.myServentInfo.getId(), this, AppConfig.version.getVersionId()+1,AppConfig.myServentInfo.getId());

			//2 wait for responses or finish
		//	boolean waiting = true;
			while (waiting.get()) {
				if (collectedLYValues.size() == AppConfig.getServentCount()) { //TODO promeniti tako da bude velicina dece? kako ce da zna koliko dece ima? zasto ne samo da li su stigli od suseda?
					waiting.set(false);
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}

			AppConfig.timestampedStandardPrint("found regions before exchange"+ regionList);
			AppConfig.timestampedErrorPrint("\n my before rounds values are for "+ collectedLYValues.keySet());
            regionList.remove(-1); //za slucaj da postoji
			regionList.remove(AppConfig.myServentInfo.getId()); //izbaci sebe iz regiona jer sta ce ti?
			AppConfig.timestampedErrorPrint("initiatiors list from config "+AppConfig.myServentInfo.getInitiatorsList() );
			boolean canBreak2=false;
			int rounds=1;
			while(AppConfig.getServentCount()!= collectedLYValues.size() || !canBreak2){
				if(collectedLYValues.size()==AppConfig.getServentCount()){
					canBreak2=true;
				}
				AppConfig.timestampedStandardPrint("Current known values are for "+collectedLYValues.keySet());
				List<Integer> sentTo=new ArrayList<>();
					AppConfig.timestampedStandardPrint("ROUND "+rounds+++" GO ");
					for(Integer region:regionList){
						if(region==AppConfig.myServentInfo.getId())
							continue;
						AppConfig.timestampedStandardPrint("Im sending message to region: " + region );
						Message message=new SKRoundExchangeMesage(AppConfig.myServentInfo,AppConfig.getInfoById(region),AppConfig.version,Map.copyOf(collectedLYValues),regionList);
						MessageUtil.sendMessage(message);
						sentTo.add(region);

					}
					while(true){ //cekas da dobijes odgovore od inicijatora susednih regiona i to iskljucivo SKRoundExchange
						boolean canBreak=true;
						for(Integer i1:sentTo){

							if(!roundExchangeValues.containsKey(i1)){
								canBreak=false;
							}else continue;
						}
						if(canBreak)
							break;
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							AppConfig.timestampedErrorPrint("ovde greska");
						}
					}

					collectedLYValues.putAll(roundExchangeValues);
					synchronized (AppConfig.versionLock){
						roundExchangeValues.clear();
					}


			}





			int sum;
			sum = 0;
			for (Entry<Integer, LYSnapshotResult> nodeResult : collectedLYValues.entrySet()) {
				sum += nodeResult.getValue().getRecordedAmount();
				AppConfig.timestampedErrorPrint(nodeResult.getValue().toString());
				AppConfig.timestampedStandardPrint(
						"Recorded bitcake amount for " + nodeResult.getKey() + " = " + nodeResult.getValue().getRecordedAmount());
			}
			AppConfig.timestampedErrorPrint("found regions"+ regionList);
//TODO treba da se spoji i dalje
			for(int i = 0; i < AppConfig.getServentCount(); i++) {
				for (int j = 0; j < AppConfig.getServentCount(); j++) {
					if (i != j) {
						if (AppConfig.getInfoById(i).getNeighbors().contains(j) &&
							AppConfig.getInfoById(j).getNeighbors().contains(i)) {
							int ijAmount = -1;
							int jiAmount = -1;
							Map<SnapshotVersionId,Map<Integer,Integer>> iGiveHistory= collectedLYValues.get(i).getGiveHistory();
							for(SnapshotVersionId key : iGiveHistory.keySet()) {
								if(key.equals(version)) {
									ijAmount = iGiveHistory.get(key).get(j);
								}
							}
							Map<SnapshotVersionId,Map<Integer,Integer>> jGetHistory= collectedLYValues.get(j).getGetHistory();
							for(SnapshotVersionId key : jGetHistory.keySet()) {
								if(key.equals(version)) {
									jiAmount = jGetHistory.get(key).get(i);
								}
							}
							String outputString1 = String.format(
									"bitcake amount:ij= %d from servent %d and ji %d servent %d in snapshot version %d.",
									ijAmount , i, jiAmount, j, version.getVersionId());
							AppConfig.timestampedErrorPrint(outputString1);
							if (ijAmount != jiAmount) {
								String outputString = String.format(
										"Unreceived bitcake amount: %d from servent %d to servent %d in snapshot version %d.",
										ijAmount - jiAmount, i, j, version.getVersionId());
								AppConfig.timestampedStandardPrint(outputString);
								sum += ijAmount - jiAmount;
							}
						}
					}
				}
			}
			
			AppConfig.timestampedStandardPrint("System bitcake count: " + sum);
			AppConfig.timestampedErrorPrint("Snapshot version " + version + " finished.");
			version= new SnapshotVersionId(version.getVersionId()+1 , version.getInitiatorId());
			bitcakeManager.setVersionInHistory(version);
			collectedLYValues.clear(); //reset for next invocation
			collecting.set(false);
			regionList=new HashSet<>(); //discoveredRegions
			neighborsFromDifferentRegion=new ArrayList<>();
			waiting.set(true);


		}

	}

	private void dontSaveAnything() {
		AppConfig.parentId.set(-1);
		regionList=new HashSet<>();
		collectedLYValues=new HashMap<>();
		neighborsFromDifferentRegion=new ArrayList<>();
	}

	@Override
	public void addLYSnapshotInfo(int id, LYSnapshotResult lySnapshotResult) {
		collectedLYValues.put(id, lySnapshotResult);
	}
	
	@Override
	public void startCollecting() {
		boolean oldValue = this.collecting.getAndSet(true);
		AppConfig.timestampedErrorPrint("STARTED COLLECTING");
		if (oldValue == true) {
			AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
		}
	}

	@Override
	public void addRegion(int region) {
		if(!regionList.contains(region))
			regionList.add(region);
	}

	@Override
	public void addNeighborFromDifferentRegion(int id) {
		if(!neighborsFromDifferentRegion.contains(id))
			neighborsFromDifferentRegion.add(id);
	}

	@Override
	public List<Integer> getNeighborsFromDifferentRegion() {
		return neighborsFromDifferentRegion;
	}

	public void setVersion(SnapshotVersionId version) {
		this.version = version;
	}
	
	@Override
	public void stop() {
		working = false;
	}

}
