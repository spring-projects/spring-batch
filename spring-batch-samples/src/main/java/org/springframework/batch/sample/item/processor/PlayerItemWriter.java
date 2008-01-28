package org.springframework.batch.sample.item.processor;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.sample.dao.PlayerDao;
import org.springframework.batch.sample.domain.Player;

public class PlayerItemWriter implements ItemWriter {

	PlayerDao playerDao;
	
	public void write(Object data) throws Exception {		
		playerDao.savePlayer((Player)data);
	}

	public void setPlayerDao(PlayerDao playerDao) {
		this.playerDao = playerDao;
	}
}
