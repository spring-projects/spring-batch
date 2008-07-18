package org.springframework.batch.item.file;

import org.springframework.batch.item.CommonItemStreamItemReaderTests;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.mapping.FieldSet;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

public class FlatFileItemReaderCommonTests extends CommonItemStreamItemReaderTests {

	private static final String FOOS = "1 \n 2 \n 3 \n 4 \n 5 \n"; 
	
	protected ItemReader<Foo> getItemReader() throws Exception {
		FlatFileItemReader<Foo> tested = new FlatFileItemReader<Foo>();
		Resource resource = new ByteArrayResource(FOOS.getBytes());
		tested.setResource(resource);
		tested.setFieldSetMapper(new FieldSetMapper<Foo>() {
			public Foo mapLine(FieldSet fs, int lineNum) {
				Foo foo = new Foo();
				foo.setValue(fs.readInt(0));
				return foo;
			}
		});
		
		tested.setSaveState(true);
		tested.afterPropertiesSet();
		return tested;
	}

	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		FlatFileItemReader<Foo> reader = (FlatFileItemReader<Foo>) tested;
		reader.close(new ExecutionContext());
		
		reader.setResource(new ByteArrayResource("".getBytes()));
		reader.afterPropertiesSet();
		
		reader.open(new ExecutionContext());
	}

}
