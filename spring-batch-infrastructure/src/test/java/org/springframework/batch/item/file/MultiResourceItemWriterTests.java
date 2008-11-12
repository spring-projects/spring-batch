package org.springframework.batch.item.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;

/**
 * Tests for {@link MultiResourceItemWriter} delegating to
 * {@link FlatFileItemWriter}.
 */
public class MultiResourceItemWriterTests extends AbstractMultiResourceItemWriterTests {

	@Override
	@Before
	public void setUp() throws Exception {
		delegate = new FlatFileItemWriter<String>() {
			{
				setLineAggregator(new PassThroughLineAggregator<String>());
			}
		};
		super.setUp();
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
}
