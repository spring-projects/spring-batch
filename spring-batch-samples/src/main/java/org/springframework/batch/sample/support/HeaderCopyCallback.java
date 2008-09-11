package org.springframework.batch.sample.support;

import java.io.IOException;
import java.io.Writer;


import org.springframework.batch.item.file.FileWriterCallback;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.LineCallbackHandler;
import org.springframework.util.Assert;

/**
 * Designed to be registered with both {@link FlatFileItemReader} and
 * {@link FlatFileItemWriter} and copy header line from input file to output
 * file.
 */
public class HeaderCopyCallback implements LineCallbackHandler, FileWriterCallback {

	private String header = "";
	
	public void handleLine(String line) {
		Assert.notNull(line);
		this.header = line;
	}

	public void write(Writer writer) throws IOException {
		writer.write("header from input: " + header);
		
	}

}
