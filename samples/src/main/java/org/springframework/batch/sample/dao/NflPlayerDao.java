package org.springframework.batch.sample.dao;

import org.springframework.batch.sample.domain.NflPlayer;

public interface NflPlayerDao {

	void savePlayer(NflPlayer nflPlayer);
}
