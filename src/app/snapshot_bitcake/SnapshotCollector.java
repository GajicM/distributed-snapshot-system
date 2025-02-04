package app.snapshot_bitcake;

import app.Cancellable;

import java.util.List;

/**
 * Describes a snapshot collector. Made not-so-flexibly for readability.
 * 
 * @author bmilojkovic
 *
 */
public interface SnapshotCollector extends Runnable, Cancellable {

	BitcakeManager getBitcakeManager();

	void addLYSnapshotInfo(int id, LYSnapshotResult lySnapshotResult);

	void startCollecting();
	void addRegion(int region);

	void addNeighborFromDifferentRegion(int id);
	List<Integer> getNeighborsFromDifferentRegion();
}