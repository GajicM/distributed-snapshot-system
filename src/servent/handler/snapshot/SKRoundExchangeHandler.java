package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.LYSnapshotResult;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.SnapshotCollectorWorker;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.LYTellMessage;
import servent.message.snapshot.SKRoundExchangeMesage;

import java.util.Map;

public class SKRoundExchangeHandler implements MessageHandler {

    private Message clientMessage;
    private SnapshotCollector snapshotCollector;

    public SKRoundExchangeHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.SK_ROUND_EXCHANGE) {
            SKRoundExchangeMesage skRoundExchangeMesage = (SKRoundExchangeMesage) clientMessage;

            AppConfig.timestampedErrorPrint("ROUND EXCHANGE MESSAGE RECEIVED"+skRoundExchangeMesage.getResultsMap()+" regiosn:"+skRoundExchangeMesage.getRegionList());

          synchronized (AppConfig.versionLock){
              for(Map.Entry<Integer, LYSnapshotResult> entry: skRoundExchangeMesage.getResultsMap().entrySet()){
                  ((SnapshotCollectorWorker)snapshotCollector).roundExchangeValues.put(entry.getKey(),entry.getValue());
              }
          }

//            for(Integer i:skRoundExchangeMesage.getRegionList()){
//                snapshotCollector.addRegion(i);
//            }
            ((SnapshotCollectorWorker)snapshotCollector).updated=true;
        } else {
            AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
        }

    }

}
