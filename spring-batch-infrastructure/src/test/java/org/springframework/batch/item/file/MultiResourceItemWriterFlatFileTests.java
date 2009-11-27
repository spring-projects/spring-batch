package org.springframework.batch.item.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tests for {@link MultiResourceItemWriter} delegating to
 * {@link FlatFileItemWriter}.
 */
public class MultiResourceItemWriterFlatFileTests extends AbstractMultiResourceItemWriterTests {

	/**
	 * @author dsyer
	 *
	 */
	private final class WriterCallback implements TransactionCallback {
		private List<? extends String> list;

		public WriterCallback(List<? extends String> list) {
			super();
			this.list = list;
		}

		public Object doInTransaction(TransactionStatus status) {
			try {
				tested.write(list);
			}
			catch (Exception e) {
				throw new IllegalStateException("Unexpected");
			}
			return null;
		}
	}

	private FlatFileItemWriter<String> delegate;

	@Before
	public void setUp() throws Exception {
		delegate = new FlatFileItemWriter<String>();
		delegate.setLineAggregator(new PassThroughLineAggregator<String>());
	}

	@Test
	public void testBasicMultiResourceWriteScenario() throws Exception {

		super.setUp(delegate);

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
	public void testUpdateAfterDelegateClose() throws Exception {

		super.setUp(delegate);

		tested.update(executionContext);
		assertEquals(0, executionContext.getInt(tested.getKey("resource.item.count")));
		assertEquals(1, executionContext.getInt(tested.getKey("resource.index")));
		tested.write(Arrays.asList("1", "2", "3"));
		tested.update(executionContext);
		assertEquals(0, executionContext.getInt(tested.getKey("resource.item.count")));
		assertEquals(2, executionContext.getInt(tested.getKey("resource.index")));

	}

	@Test
	public void testMultiResourceWriteScenarioWithFooter() throws Exception {

		delegate.setFooterCallback(new FlatFileFooterCallback() {
			public void writeFooter(Writer writer) throws IOException {
				writer.write("f");
			}
		});
		super.setUp(delegate);

		tested.write(Arrays.asList("1", "2", "3"));

		File part1 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(1));
		assertTrue(part1.exists());

		tested.write(Arrays.asList("4"));
		File part2 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(2));
		assertTrue(part2.exists());
		
		tested.close();

		assertEquals("123f", readFile(part1));
		assertEquals("4f", readFile(part2));

	}

	@Test
	public void testTransactionalMultiResourceWriteScenarioWithFooter() throws Exception {

		delegate.setFooterCallback(new FlatFileFooterCallback() {
			public void writeFooter(Writer writer) throws IOException {
				writer.write("f");
			}
		});
		super.setUp(delegate);
		
		ResourcelessTransactionManager transactionManager = new ResourcelessTransactionManager();
		
		new TransactionTemplate(transactionManager).execute(new WriterCallback(Arrays.asList("1", "2", "3")));

		File part1 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(1));
		assertTrue(part1.exists());

		new TransactionTemplate(transactionManager).execute(new WriterCallback(Arrays.asList("4")));
		File part2 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(2));
		assertTrue(part2.exists());
		
		tested.close();

		assertEquals("123f", readFile(part1));
		assertEquals("4f", readFile(part2));

	}

	@Test
	public void testRestart() throws Exception {

		super.setUp(delegate);

		tested.write(Arrays.asList("1", "2", "3"));

		File part1 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(1));
		assertTrue(part1.exists());
		assertEquals("123", readFile(part1));

		tested.write(Arrays.asList("4"));
		File part2 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(2));
		assertTrue(part2.exists());
		assertEquals("4", readFile(part2));

		tested.update(executionContext);
		tested.close();
		tested.open(executionContext);

		tested.write(Arrays.asList("5"));
		assertEquals("45", readFile(part2));

		tested.write(Arrays.asList("6", "7", "8", "9"));
		File part3 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(3));
		assertTrue(part3.exists());
		assertEquals("6789", readFile(part3));
	}

	@Test
	public void testRestartWithFooter() throws Exception {

		delegate.setFooterCallback(new FlatFileFooterCallback() {
			public void writeFooter(Writer writer) throws IOException {
				writer.write("f");
			}
		});
		super.setUp(delegate);

		tested.write(Arrays.asList("1", "2", "3"));

		File part1 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(1));
		assertTrue(part1.exists());
		assertEquals("123f", readFile(part1));

		tested.write(Arrays.asList("4"));
		File part2 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(2));
		assertTrue(part2.exists());
		assertEquals("4", readFile(part2));

		tested.update(executionContext);
		tested.close();
		tested.open(executionContext);

		tested.write(Arrays.asList("5"));
		assertEquals("45f", readFile(part2));

		tested.write(Arrays.asList("6", "7", "8", "9"));
		File part3 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(3));
		assertTrue(part3.exists());
		assertEquals("6789f", readFile(part3));
	}

	@Test
	public void testTransactionalRestartWithFooter() throws Exception {

		delegate.setFooterCallback(new FlatFileFooterCallback() {
			public void writeFooter(Writer writer) throws IOException {
				writer.write("f");
			}
		});
		super.setUp(delegate);

		ResourcelessTransactionManager transactionManager = new ResourcelessTransactionManager();
		
		new TransactionTemplate(transactionManager).execute(new WriterCallback(Arrays.asList("1", "2", "3")));

		File part1 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(1));
		assertTrue(part1.exists());
		assertEquals("123f", readFile(part1));

		new TransactionTemplate(transactionManager).execute(new WriterCallback(Arrays.asList("4")));
		File part2 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(2));
		assertTrue(part2.exists());
		assertEquals("4", readFile(part2));

		tested.update(executionContext);
		tested.close();
		tested.open(executionContext);

		new TransactionTemplate(transactionManager).execute(new WriterCallback(Arrays.asList("5")));
		assertEquals("45f", readFile(part2));
	}

}
