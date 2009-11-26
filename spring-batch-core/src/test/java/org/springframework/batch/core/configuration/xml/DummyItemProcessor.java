package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.item.ItemProcessor;

/**
 * @author Dave Syer
 * @since 2.1
 */
public class DummyItemProcessor implements ItemProcessor<Object,Object> {

	public Object process(Object item) throws Exception {
		return item;
	}

}
