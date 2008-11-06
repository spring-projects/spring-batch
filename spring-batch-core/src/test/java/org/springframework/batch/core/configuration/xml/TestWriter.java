package org.springframework.batch.core.configuration.xml;

import java.util.List;

import org.springframework.batch.item.ItemWriter;

public class TestWriter implements ItemWriter<String> {

	public void write(List<? extends String> items) throws Exception {
	}

}
