package org.springframework.batch.sample.football;


/**
 * Interface for writing {@link Player} objects to arbitrary output.
 */
public interface PlayerDao {

	void savePlayer(Player player);
}
