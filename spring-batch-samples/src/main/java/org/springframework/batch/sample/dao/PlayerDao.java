package org.springframework.batch.sample.dao;

import org.springframework.batch.sample.domain.Player;

public interface PlayerDao {

	void savePlayer(Player player);
}
