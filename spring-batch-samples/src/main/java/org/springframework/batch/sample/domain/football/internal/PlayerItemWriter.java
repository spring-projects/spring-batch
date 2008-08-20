package org.springframework.batch.sample.domain.football.internal;

import java.util.List;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.sample.domain.football.Player;
import org.springframework.batch.sample.domain.football.PlayerDao;

public class PlayerItemWriter implements ItemWriter<Player> {

	private PlayerDao playerDao;

	public void write(List<? extends Player> players) throws Exception {
		for (Player player : players) {
			playerDao.savePlayer(player);
		}
	}

	public void setPlayerDao(PlayerDao playerDao) {
		this.playerDao = playerDao;
	}

}
