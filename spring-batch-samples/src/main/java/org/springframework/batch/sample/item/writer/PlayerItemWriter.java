package org.springframework.batch.sample.item.writer;

import org.springframework.batch.item.support.AbstractItemWriter;
import org.springframework.batch.sample.dao.PlayerDao;
import org.springframework.batch.sample.domain.Player;

public class PlayerItemWriter extends AbstractItemWriter<Player> {

	private PlayerDao playerDao;
	
	public void write(Player player) throws Exception {		
		playerDao.savePlayer(player);
	}

	public void setPlayerDao(PlayerDao playerDao) {
		this.playerDao = playerDao;
	}
	
}
