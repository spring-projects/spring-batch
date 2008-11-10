package org.springframework.batch.item.file;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.core.io.FileSystemResource;

/**
 * Tests for {@link MultiResourceItemWriter}.
 */
public class MultiResourceItemWriterTests {

	private MultiResourceItemWriter<String> tested = new MultiResourceItemWriter<String>();

	private File file;
	
	private ResourceSuffixCreator suffixCreator = new SimpleResourceSuffixCreator();

	private ResourceAwareItemWriterItemStream<String> delegate = new FlatFileItemWriter<String>() {
		{
			setLineAggregator(new PassThroughLineAggregator<String>());
		}
	};

	private ExecutionContext executionContext = new ExecutionContext();

	@Before
	public void setUp() throws Exception {
		file = File.createTempFile(MultiResourceItemWriterTests.class.getSimpleName(), null);
		tested.setResource(new FileSystemResource(file));
		tested.setDelegate(delegate);
		tested.setResourceSuffixCreator(suffixCreator);
		tested.setItemCountLimitPerResource(2);

		tested.open(executionContext);
	}

	@Test
	public void testBasicMultiResourceWriteScenario() throws Exception {

		tested.write(Arrays.asList("1", "2", "3"));

		File part1 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(1));
		assertTrue(part1.exists());
		assertEquals("123", readFile(part1));

		tested.write(Arrays.asList("4"));
		File part2 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(2));
		assertTrue(part2.exists());
		assertEquals("4", readFile(part2));

		tested.write(Arrays.asList("5"));
		assertEquals("45", readFile(part2));

		tested.write(Arrays.asList("6", "7", "8", "9"));
		File part3 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(3));
		assertTrue(part3.exists());
		assertEquals("6789", readFile(part3));
	}

	@Test
	public void testRestart() throws Exception {
		tested.write(Arrays.asList("1", "2", "3"));

		File part1 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(1));
		assertTrue(part1.exists());
		assertEquals("123", readFile(part1));

		tested.write(Arrays.asList("4"));
		File part2 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(2));
		assertTrue(part2.exists());
		assertEquals("4", readFile(part2));

		tested.update(executionContext);
		tested.close(executionContext);
		tested.open(executionContext);
		
		tested.write(Arrays.asList("5"));
		assertEquals("45", readFile(part2));

		tested.write(Arrays.asList("6", "7", "8", "9"));
		File part3 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(3));
		assertTrue(part3.exists());
		assertEquals("6789", readFile(part3));
	}

	private String readFile(File f) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(f));
		StringBuilder result = new StringBuilder();
		while (true) {
			String line = reader.readLine();
			if (line == null) {
				break;
			}
			result.append(line);
		}
		return result.toString();
	}
}
