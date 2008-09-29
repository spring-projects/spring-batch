package org.springframework.batch.item.file;

import java.util.Comparator;

import org.springframework.batch.item.CommonItemStreamItemReaderTests;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.mapping.FieldSet;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.junit.runner.RunWith;
import org.junit.internal.runners.JUnit4ClassRunner;

@RunWith(JUnit4ClassRunner.class)
public class MultiResourceItemReaderFlatFileTests extends
		CommonItemStreamItemReaderTests {

	protected ItemReader<Foo> getItemReader() throws Exception {

		MultiResourceItemReader<Foo> multiReader = new MultiResourceItemReader<Foo>();
		FlatFileItemReader<Foo> fileReader = new FlatFileItemReader<Foo>();

		fileReader.setFieldSetMapper(new FieldSetMapper<Foo>() {
			public Foo process(FieldSet fs) {
				Foo foo = new Foo();
				foo.setValue(fs.readInt(0));
				return foo;
			}
		});
		fileReader.setSaveState(true);

		multiReader.setDelegate(fileReader);

		Resource r1 = new ByteArrayResource("1\n2\n".getBytes());
		Resource r2 = new ByteArrayResource("".getBytes());
		Resource r3 = new ByteArrayResource("3\n".getBytes());
		Resource r4 = new ByteArrayResource("4\n5\n".getBytes());

		multiReader.setResources(new Resource[] { r1, r2, r3, r4 });
		multiReader.setSaveState(true);
		multiReader.setComparator(new Comparator<Resource>() {
			public int compare(Resource arg0, Resource arg1) {
				return 0; // preserve original ordering
			}
			
		});

		return multiReader;
	}

	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		MultiResourceItemReader<Foo> multiReader = (MultiResourceItemReader<Foo>) tested;
		multiReader.close(new ExecutionContext());
		multiReader.setResources(new Resource[] { new ByteArrayResource(""
				.getBytes()) });
		multiReader.open(new ExecutionContext());
	}

}
