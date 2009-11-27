package org.springframework.batch.item.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.FileSystemResource;

/**
 * Tests for {@link MultiResourceItemWriter}.
 * 
 * @see MultiResourceItemWriterFlatFileTests
 * @see MultiResourceItemReaderXmlTests
 */
public class AbstractMultiResourceItemWriterTests {

	protected MultiResourceItemWriter<String> tested = new MultiResourceItemWriter<String>();

	protected File file;

	protected ResourceSuffixCreator suffixCreator = new SimpleResourceSuffixCreator();

	protected ExecutionContext executionContext = new ExecutionContext();

	protected void setUp(ResourceAwareItemWriterItemStream<String> delegate) throws Exception {
		file = File.createTempFile(MultiResourceItemWriterFlatFileTests.class.getSimpleName(), null);
		tested.setResource(new FileSystemResource(file));
		tested.setDelegate(delegate);
		tested.setResourceSuffixCreator(suffixCreator);
		tested.setItemCountLimitPerResource(2);
		tested.setSaveState(true);
		tested.open(executionContext);
	}

	protected String readFile(File f) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(f));
		StringBuilder result = new StringBuilder();
		try {
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				result.append(line);
			}
		}
		finally {
			reader.close();
		}
		return result.toString();
	}
}
