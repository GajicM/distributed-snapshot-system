package servent;

import app.AppConfig;
import app.Cancellable;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.SnapshotCollectorWorker;
import servent.handler.MessageHandler;
import servent.handler.NullHandler;
import servent.handler.TransactionHandler;
import servent.handler.snapshot.LYMarkerHandler;
import servent.handler.snapshot.LYTellHandler;
import servent.handler.snapshot.SKRejectParentHandler;
import servent.handler.snapshot.SKRoundExchangeHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.SKRejectParentMessage;
import servent.message.util.MessageUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleServentListener implements Runnable, Cancellable {

	private volatile boolean working = true;
	
	private SnapshotCollector snapshotCollector;
	
	public SimpleServentListener(SnapshotCollector snapshotCollector) {
		this.snapshotCollector = snapshotCollector;
	}

	/*
	 * Thread pool for executing the handlers. Each client will get it's own handler thread.
	 */
	private final ExecutorService threadPool = Executors.newWorkStealingPool();
	
	private List<Message> redMessages = new ArrayList<>();
	
	@Override
	public void run() {
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(AppConfig.myServentInfo.getListenerPort(), 100);
			/*
			 * If there is no connection after 1s, wake up and see if we should terminate.
			 */
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.getListenerPort());
			System.exit(0);
		}
		
		
		while (working) {
			try {
				Message clientMessage=null;
				/*
				* This blocks for up to 1s, after which SocketTimeoutException is thrown.
				 */
				Socket clientSocket = listenerSocket.accept();
				//GOT A MESSAGE! <3
				clientMessage = MessageUtil.readMessage(clientSocket);
				synchronized (AppConfig.versionLock) {
					if (clientMessage.getVersionVector().getVersionId()>AppConfig.version.getVersionId()) {
						if (clientMessage.getMessageType() != MessageType.LY_MARKER) {

						} else {
							LaiYangBitcakeManager lyFinancialManager =
									(LaiYangBitcakeManager)snapshotCollector.getBitcakeManager();
							lyFinancialManager.markerEvent(
									Integer.parseInt(clientMessage.getMessageText()), snapshotCollector,clientMessage.getVersionVector().getVersionId(),clientMessage.getOriginalSenderInfo().getId());
						}
					}else if(clientMessage.getVersionVector().getVersionId()==AppConfig.version.getVersionId() && clientMessage.getMessageType().equals(MessageType.LY_MARKER)){
						//razlicit inicijator iste verzije
						if(!snapshotCollector.getNeighborsFromDifferentRegion().contains(clientMessage.getOriginalSenderInfo().getId())) {
							snapshotCollector.addRegion(clientMessage.getVersionVector().getInitiatorId());
							snapshotCollector.addNeighborFromDifferentRegion(clientMessage.getOriginalSenderInfo().getId());
							Message m = new SKRejectParentMessage(AppConfig.myServentInfo, clientMessage.getOriginalSenderInfo(), AppConfig.version);
							MessageUtil.sendMessage(m);
						}
					}
				}
				
				MessageHandler messageHandler = switch (clientMessage.getMessageType()) {
                    case TRANSACTION -> new TransactionHandler(clientMessage, snapshotCollector.getBitcakeManager());
                    case LY_MARKER -> new LYMarkerHandler();
                    case LY_TELL -> new LYTellHandler(clientMessage, snapshotCollector);
                    case SK_RejectParent -> new SKRejectParentHandler(clientMessage, (SnapshotCollectorWorker) snapshotCollector);
					case SK_ROUND_EXCHANGE -> new SKRoundExchangeHandler(clientMessage,snapshotCollector);
                };
				
				/*
				 * Each message type has it's own handler.
				 * If we can get away with stateless handlers, we will,
				 * because that way is much simpler and less error prone.
				 */

                threadPool.submit(messageHandler);
			} catch (SocketTimeoutException timeoutEx) {
				//Uncomment the next line to see that we are waking up every second.
//				AppConfig.timedStandardPrint("Waiting...");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
		this.working = false;
	}

}
