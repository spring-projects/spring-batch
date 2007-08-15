package org.springframework.batch.sample.module.process;

import org.springframework.batch.io.file.support.FlatFileOutputSource;
import org.springframework.batch.item.ItemProcessor;

public class DefaultFlatFileProcessor implements ItemProcessor{

	private FlatFileOutputSource flatFileOutputSource;

	public void process(Object data) throws Exception {
		flatFileOutputSource.write(""+data);
	}

	public void setFlatFileOutputSource(FlatFileOutputSource flatFileOutputSource) {
		this.flatFileOutputSource = flatFileOutputSource;
	}
	
}
