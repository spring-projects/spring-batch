package org.springframework.batch.sample.item.processor;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.sample.dao.PlayerDao;
import org.springframework.batch.sample.domain.Player;

public class PlayerItemProcessor implements ItemProcessor {

	PlayerDao playerDao;
	
	public void process(Object data) throws Exception {		
		playerDao.savePlayer((Player)data);
	}

	public void setPlayerDao(PlayerDao playerDao) {
		this.playerDao = playerDao;
	}
}
