package app.snapshot_bitcake;

import app.AppConfig;
import servent.message.Message;
import servent.message.snapshot.LYTellMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ChildResponseAwaiterThread implements Runnable {
    private SnapshotCollectorWorker snapshotCollector;
    private int collectorId;
    private int versionId;
    public ChildResponseAwaiterThread(SnapshotCollectorWorker snapshotCollector, int collectorId,int versionId) {
        this.snapshotCollector = snapshotCollector;
        this.collectorId = collectorId;
        this.versionId = versionId;
    }

    @Override
    public void run() {
            //waituje ovde dok se ne zavrse svi odgovori
        //pamti u snapshotCollector
        //salje parentu odgovor

        boolean stillWorking=true;
        loop:
        while (stillWorking) {
            for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
                if(neighbor==AppConfig.parentId.get())
                    continue;
   //             AppConfig.timestampedErrorPrint("parent" + AppConfig.parentId.get());
  //             AppConfig.timestampedErrorPrint("neigh"+neighbor+"\ncolVals="+((SnapshotCollectorWorker) snapshotCollector).collectedLYValues+" \nlockedOut= "+snapshotCollector.neighborsFromDifferentRegion);
                if (!(snapshotCollector).collectedLYValues.containsKey(neighbor) &&
                        !snapshotCollector.neighborsFromDifferentRegion.contains(neighbor)
                && AppConfig.parentId.get()!=neighbor) {
                    try {
                        Thread.sleep(500);
                        continue loop;
                    } catch (InterruptedException e) {
                        AppConfig.timestampedErrorPrint("EH");
                    }
                }
            }
            stillWorking = false;
        }  //all values are collected
        AppConfig.timestampedErrorPrint("IZASAO IZ CUDNOG WHILEA" + snapshotCollector.collectedLYValues);

        if((AppConfig.myServentInfo.getId() != AppConfig.parentId.get())){

            Message reportToParent=new LYTellMessage(
                    AppConfig.myServentInfo,
                    AppConfig.getInfoById(AppConfig.parentId.get()),
                   snapshotCollector.collectedLYValues,
                    AppConfig.version,
                    snapshotCollector.regionList);
            MessageUtil.sendMessage(reportToParent);
            AppConfig.timestampedErrorPrint("REPORTUJEM PARENTU\n collected vals"+snapshotCollector.collectedLYValues+ "\nneibors from regions "+snapshotCollector.neighborsFromDifferentRegion+"\n regions "+snapshotCollector.regionList);
            AppConfig.timestampedStandardPrint("My children are"+snapshotCollector.collectedLYValues.keySet() + "and my parent is "+AppConfig.parentId);
            snapshotCollector.waitingForNewSnapshot.set(true);
        }else {
            snapshotCollector.waiting.set(false);
        }


        }

}
