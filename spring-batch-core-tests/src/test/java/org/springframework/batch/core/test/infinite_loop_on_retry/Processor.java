package org.springframework.batch.core.test.infinite_loop_on_retry;

import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

public class Processor implements ItemProcessor<Item, Item> {

	private static final org.slf4j.Logger log = LoggerFactory
			.getLogger(Processor.class);

	int cpt = 0;

	@Override
	public Item process(Item item) throws Exception {
		log.debug("process " + item);

		cpt++;
		if (cpt == 6 || cpt == 7) {
			log.debug("error on process for " + item);
			throw new Exception("Error during process");
		}

		item.incNbProcessed();
		return item;

	}

}
