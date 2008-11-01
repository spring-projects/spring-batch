package org.springframework.batch.sample.dao;

import org.springframework.batch.sample.domain.Player;

/**
 * Interface for writing {@link Player} objects to arbitrary output.
 */
public interface PlayerDao {

	void savePlayer(Player player);
}
