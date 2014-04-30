package org.springframework.batch.item.file;

import org.springframework.batch.item.AbstractItemStreamItemReaderTests;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * Tests for {@link FlatFileItemReader}.
 */
public class MultiThreadedFlatFileItemReaderCommonTests extends AbstractItemStreamItemReaderTests{

	private static final String FOOS = "0 \n 1 \n 2 \n 3 \n 4 \n 5 \n"; 
	
	@Override
	protected ItemReader<Foo> getItemReader() throws Exception {
		MultiThreadedFlatFileItemReader<Foo> tested = new MultiThreadedFlatFileItemReader<Foo>();
		Resource resource = new ByteArrayResource(FOOS.getBytes());
		tested.setResource(resource);
		tested.setStartAt(4);
		tested.setLineMapper(new LineMapper<Foo>() {
            @Override
			public Foo mapLine(String line, int lineNumber) {
				Foo foo = new Foo();
				foo.setValue(Integer.valueOf(line.trim()));
				return foo;
			}
		});
		
		tested.setSaveState(true);
		tested.afterPropertiesSet();
		return tested;
	}

	@Override
	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		MultiThreadedFlatFileItemReader<Foo> reader = (MultiThreadedFlatFileItemReader<Foo>) tested;
		reader.close();
		
		reader.setResource(new ByteArrayResource("".getBytes()));
		reader.afterPropertiesSet();
		
		reader.open(new ExecutionContext());
		
	}

	
	
}
