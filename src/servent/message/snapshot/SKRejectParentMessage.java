package servent.message.snapshot;

import app.ServentInfo;
import app.snapshot_bitcake.SnapshotVersionId;
import com.sun.nio.sctp.MessageInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.util.List;

public class SKRejectParentMessage extends BasicMessage {//send this message to attempted parent if i already have one ( i dont need 2)

    public SKRejectParentMessage( ServentInfo originalSenderInfo, ServentInfo receiverInfo, SnapshotVersionId versionVector) {
        super(MessageType.SK_RejectParent, originalSenderInfo, receiverInfo, versionVector);
    }

}
