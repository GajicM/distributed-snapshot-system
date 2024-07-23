package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.LYSnapshotResult;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.SnapshotCollectorWorker;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.LYTellMessage;

import java.util.Map;

public class LYTellHandler implements MessageHandler {

	private Message clientMessage;
	private SnapshotCollector snapshotCollector;
	
	public LYTellHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
		this.clientMessage = clientMessage;
		this.snapshotCollector = snapshotCollector;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.LY_TELL) {
			LYTellMessage lyTellMessage = (LYTellMessage)clientMessage;

			AppConfig.timestampedErrorPrint("TELL MESSAGE RECEIVED"+lyTellMessage.getResultsMap()+" regiosn:"+lyTellMessage.getRegionList());
			for(Map.Entry<Integer, LYSnapshotResult> entry: lyTellMessage.getResultsMap().entrySet()){
				snapshotCollector.addLYSnapshotInfo(
						entry.getKey(),
						entry.getValue());
			}
			for(Integer i:lyTellMessage.getRegionList()){
				snapshotCollector.addRegion(i);
			}
//			if(AppConfig.myServentInfo.getId()==lyTellMessage.getVersionVector().getInitiatorId()){
//				((SnapshotCollectorWorker)snapshotCollector).waiting.set(false);
//			}

		} else {
			AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
		}

	}

}
