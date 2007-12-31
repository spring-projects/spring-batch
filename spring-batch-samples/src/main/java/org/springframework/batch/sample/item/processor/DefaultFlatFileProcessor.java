package org.springframework.batch.sample.item.processor;

import org.springframework.batch.io.file.FlatFileItemWriter;
import org.springframework.batch.item.ItemProcessor;

public class DefaultFlatFileProcessor implements ItemProcessor{

	private FlatFileItemWriter flatFileItemWriter;

	public void process(Object data) throws Exception {
		flatFileItemWriter.write(""+data);
	}

	public void setFlatFileOutputSource(FlatFileItemWriter flatFileItemWriter) {
		this.flatFileItemWriter = flatFileItemWriter;
	}
	
}
