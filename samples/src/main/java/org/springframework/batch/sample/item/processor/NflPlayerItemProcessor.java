package org.springframework.batch.sample.item.processor;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.sample.dao.NflPlayerDao;
import org.springframework.batch.sample.domain.NflPlayer;

public class NflPlayerItemProcessor implements ItemProcessor {

	NflPlayerDao nflPlayerDao;
	
	public void process(Object data) throws Exception {		
		nflPlayerDao.savePlayer((NflPlayer)data);
	}

	public void setNflPlayerDao(NflPlayerDao nflPlayerDao) {
		this.nflPlayerDao = nflPlayerDao;
	}
}
