package org.springframework.batch.item.file;

import org.springframework.batch.item.CommonItemStreamItemReaderTests;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.mapping.LineMapper;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * Tests for {@link ResourceLineReader}.
 */
public class ResourceLineReaderTests extends CommonItemStreamItemReaderTests{

	private static final String FOOS = "1 \n 2 \n 3 \n 4 \n 5 \n"; 
	
	@Override
	protected ItemReader<Foo> getItemReader() throws Exception {
		ResourceLineReader<Foo> tested = new ResourceLineReader<Foo>();
		Resource resource = new ByteArrayResource(FOOS.getBytes());
		tested.setResource(resource);
		tested.setLineMapper(new LineMapper<Foo>() {
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
		ResourceLineReader<Foo> reader = (ResourceLineReader<Foo>) tested;
		reader.close(new ExecutionContext());
		
		reader.setResource(new ByteArrayResource("".getBytes()));
		reader.afterPropertiesSet();
		
		reader.open(new ExecutionContext());
		
	}

	
	
}
