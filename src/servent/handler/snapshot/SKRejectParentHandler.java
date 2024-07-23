package servent.handler.snapshot;

import app.snapshot_bitcake.SnapshotCollectorWorker;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;

public class SKRejectParentHandler implements MessageHandler { //if i get this message, i am rejected as a parent, so i may be a leaf
    private Message clientMessage;
    private SnapshotCollectorWorker snapshotCollectorWorker;
    public SKRejectParentHandler(Message clientMessage, SnapshotCollectorWorker snapshotCollectorWorker) {
        this.clientMessage=clientMessage;
        this.snapshotCollectorWorker=snapshotCollectorWorker;
    }

    @Override
    public void run() {

        snapshotCollectorWorker.addRegion(clientMessage.getVersionVector().getInitiatorId());
        snapshotCollectorWorker.addNeighborFromDifferentRegion(clientMessage.getOriginalSenderInfo().getId());
    }
}
