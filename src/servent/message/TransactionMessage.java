package servent.message;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.SnapshotVersionId;

/**
 * Represents a bitcake transaction. We are sending some bitcakes to another node.
 * 
 * @author bmilojkovic
 *
 */
public class TransactionMessage extends BasicMessage {

	private static final long serialVersionUID = -333251402058492901L;

	private transient BitcakeManager bitcakeManager;
	
	public TransactionMessage(ServentInfo sender, ServentInfo receiver, int amount, BitcakeManager bitcakeManager, SnapshotVersionId versionVector) {
		super(MessageType.TRANSACTION, sender, receiver, String.valueOf(amount), versionVector);
		this.bitcakeManager = bitcakeManager;
	}
	
	/**
	 * We want to take away our amount exactly as we are sending, so our snapshots don't mess up.
	 * This method is invoked by the sender just before sending, and with a lock that guarantees
	 * that we are white when we are doing this in Chandy-Lamport.
	 */
	@Override
	public void sendEffect() {
		int amount = Integer.parseInt(getMessageText());
		if(AppConfig.version.getVersionId()>getVersionVector().getVersionId()){
			setVersionVector(AppConfig.version);//TODO MOZDA?
		}
		bitcakeManager.takeSomeBitcakes(amount);
		if (bitcakeManager instanceof LaiYangBitcakeManager ) {
			LaiYangBitcakeManager lyFinancialManager = (LaiYangBitcakeManager)bitcakeManager;
			
			lyFinancialManager.recordGiveTransaction(getVersionVector().getVersionId(),getReceiverInfo().getId(), amount);
		}
	}
}
