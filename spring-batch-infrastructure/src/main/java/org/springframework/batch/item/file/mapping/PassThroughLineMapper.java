package org.springframework.batch.item.file.mapping;

public class PassThroughLineMapper implements LineMapper<String>{

	public String mapLine(String line, int lineNumber) throws Exception {
		return line;
	}

}
