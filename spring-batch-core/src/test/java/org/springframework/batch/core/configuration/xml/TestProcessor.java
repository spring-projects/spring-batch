package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.item.ItemProcessor;

public class TestProcessor extends AbstractTestComponent implements ItemProcessor<String, String>{

	public String process(String item) throws Exception {
		executed = true;
		return item;
	}

}
