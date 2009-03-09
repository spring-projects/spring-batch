package org.springframework.batch.core.configuration.xml;

import java.util.List;

import org.springframework.batch.item.ItemWriter;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class DummyItemWriter implements ItemWriter<Object> {

	public void write(List<? extends Object> items) throws Exception {
	}

}
