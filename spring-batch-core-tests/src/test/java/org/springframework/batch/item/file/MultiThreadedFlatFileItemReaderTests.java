/**
 * 
 */
package org.springframework.batch.item.file;

import static junit.framework.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.support.FlatFilePartitioner;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests the {@link MultiThreadedFlatFileItemReader} by creating a temporary flat fixed length field file
 * and starting a job consisting of a single partition step that used {@link FlatFilePartitioner} to 
 * prepare {@link ExcecutionContext}s for each partition with information about at which position within
 * the file the reading partition thread should start reading items and how much of them should it read.
 * The {@link DummyWriter} assures then that number of read items is equals to the number of entries in
 * the temporary file.
 * 
 * @author Sergey Shcherbakov
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MultiThreadedFlatFileItemReaderTests {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;
    
    @Autowired
    private DummyWriter countingWriter;

	private static final long LINE_COUNT = 1000L;
	private static final int BUFFER_SIZE = 4096;
	private File testFile;
	
	/**
	 * Creates a temporary test file and populates it with dummy records 
	 * @throws IOException
	 */
	@Before
	public void setUp() throws IOException {
		testFile = File.createTempFile("big", ".csv");
		testFile.deleteOnExit();
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(testFile), BUFFER_SIZE);
			for(long i=0; i < LINE_COUNT; i++) {
				bw.write(String.format("%d,numbers\n",i));
			}
		}
		finally {
			if( bw != null ) {
				bw.close();
			}
		}
		countingWriter.reset(true);
	}

	/**
	 * Main test method that launches the job
	 * @throws Exception
	 */
	@Test
	public void testReading() throws Exception {
		jobLauncherTestUtils.launchJob(	new JobParametersBuilder()
				.addString("input.file", "file:" + testFile.getAbsolutePath())
				.toJobParameters());
		
		assertEquals(LINE_COUNT, countingWriter.getItemCount());
	}

	/**
	 * Asserts consistent recovery from a failure in the middle of the parallel processing. 
	 * @throws Exception
	 */
	@Test
	public void testJobRestart() throws Exception {
		final JobParameters jobParameters = new JobParametersBuilder()
				.addString("input.file", "file:" + testFile.getAbsolutePath())
				.toJobParameters();
		
		countingWriter.setFailAt(LINE_COUNT/2);
		JobExecution status = jobLauncherTestUtils.launchJob( jobParameters );
		assertEquals(BatchStatus.FAILED, status.getStatus());
		
		int alreadyWritten = 0;
		for(StepExecution se : status.getStepExecutions()) {
			if(se.getStepName().contains("partition")) {
				alreadyWritten += se.getWriteCount();
			}
		}
		
		countingWriter.reset(false);
		status = jobLauncherTestUtils.launchJob( jobParameters );
		assertEquals(BatchStatus.COMPLETED, status.getStatus());

		assertEquals(LINE_COUNT - alreadyWritten, countingWriter.getItemCount());
		assertEquals(LINE_COUNT, countingWriter.getSeenIdsCount());
	}
	
	/**
	 * Dummy item writer.
	 * Counts items coming through.
	 * Can throw an exception at a given count point.
	 * Checks that items get written only once. 
	 */
	public static class DummyWriter implements ItemWriter<Draw> {
		private long failAt;
		private AtomicLong itemCount;
		private Set<Integer> seenIds;

		public DummyWriter() {
			reset(true);
		}
		
		public void write(List<? extends Draw> items) throws Exception {
			long prevCount = itemCount.getAndAdd(items.size());
			if( prevCount > failAt ) {
				throw new JobExecutionException("Failed at " + prevCount);
			}
			for( Draw d : items ) {
				if(!seenIds.add(d.getId())) {
					throw new JobExecutionException(String.format("Seeing the %s again!", d));
				}
			}
		}
		
		public long getItemCount() {
			return itemCount.get();
		}
		public long getSeenIdsCount() {
			return seenIds.size();
		}		
		public void setFailAt(long failAt) {
			this.failAt = failAt;
		}
		public void reset(boolean clearHistory) {
			this.failAt = Long.MAX_VALUE;
			this.itemCount = new AtomicLong(0);
			if( clearHistory ) {
				seenIds = Collections.synchronizedSet(new HashSet<Integer>());
			}
		}
	}

	/**
	 *	Dummy item processor. Sleeps for a given delay. 
	 */
	public static class DelayingProcessor implements ItemProcessor<Draw, Draw> {
		@Value("${processor.delay}")
		private long delay;
		public Draw process(Draw item) throws Exception {
			Thread.sleep(delay);
			return item;
		}
	}

	public static class Draw implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private int id;
		private String numbers;
		
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public String getNumbers() {
			return numbers;
		}
		public void setNumbers(String numbers) {
			this.numbers = numbers;
		}
		
		@Override
		public String toString() {
			return "Draw [id=" + id + ", numbers=" + numbers + "]";
		}
	}
}
