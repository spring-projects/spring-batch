package org.springframework.batch.sample.football;

import org.springframework.batch.item.support.AbstractItemWriter;

public class PlayerItemWriter extends AbstractItemWriter<Player> {

	private PlayerDao playerDao;
	
	public void write(Player player) throws Exception {		
		playerDao.savePlayer(player);
	}

	public void setPlayerDao(PlayerDao playerDao) {
		this.playerDao = playerDao;
	}
	
}
