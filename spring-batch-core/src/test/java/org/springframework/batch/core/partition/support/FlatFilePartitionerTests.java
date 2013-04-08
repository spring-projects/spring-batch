/**
 * 
 */
package org.springframework.batch.core.partition.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.AbstractResource;
import org.springframework.util.Assert;

public class FlatFilePartitionerTests {

	private static final String TMP_PATH = "tmp"; 

	private FlatFilePartitioner partitioner;
	
	private static final long LINE_COUNT = 15233L;
	private static final int BUFFER_SIZE = 4096;
	private File testFile;
	private File emptyFile;
	private File oneLineFile;
	private File twoLineFile;

	@Before
	public void setUp() throws IOException {
		partitioner = new FlatFilePartitioner();

		testFile = File.createTempFile("big", ".csv");
		testFile.deleteOnExit();
		FileWriter fw1 = null, fw2 = null;
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(testFile), BUFFER_SIZE);
			for(long i=0; i < LINE_COUNT; i++) {
				bw.write("04.01.2012; ;36;29; 3;24;18;46;--;39;1;3505278;645804; 21.634.026,75;         1; 1.081.701,30;         2;   432.680,50;         9;    60.094,50;       463;     3.037,10;     1.106;       195,60;    24.023;        45,00;    31.343;        27,60;   436.701;        10,80\n");
			}
			emptyFile = File.createTempFile("empty", ".csv");
			emptyFile.deleteOnExit();
			
			oneLineFile = File.createTempFile("line", ".csv");
			oneLineFile.deleteOnExit();
			fw1 = new FileWriter(oneLineFile); 
			fw1.write("hello");
			
			twoLineFile = File.createTempFile("line", ".csv");
			twoLineFile.deleteOnExit();
			fw2 = new FileWriter(twoLineFile); 
			fw2.write("hello\nagain");
		}
		finally {
			if( bw != null ) {
				bw.close();
			}
			if( fw1 != null ) {
				fw1.close();
			}
			if( fw2 != null ) {
				fw2.close();
			}
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void testPartitionPrecondition() {
		partitioner.partition(0);
	}	
	
	private void setResource(long lines, long lineLength, boolean lastTerminated) {
		StringBuilder source = new StringBuilder();
		for(int i=0; i<lines; i++) {
			for(int j=0; j<lineLength; j++) {
				source.append(j % 10);
			}
			if( i < lines-1 || lastTerminated ) {
				source.append('\n');
			}
		}
		InMemoryResource resource = new InMemoryResource(source.toString());
		partitioner.setResource(resource);
	}

	private void assertPartition(long startAt, long lines, Map<String, ExecutionContext> partition, String suffix) {
		ExecutionContext ex = partition.get(FlatFilePartitioner.DEFAULT_PARTITION_PREFIX + suffix);
		assertEquals(startAt, ex.get(FlatFilePartitioner.DEFAULT_START_AT_KEY));
		assertEquals(lines, ex.get(FlatFilePartitioner.DEFAULT_ITEMS_COUNT_KEY));
		assertEquals("file:"+TMP_PATH, ex.get(FlatFilePartitioner.DEFAULT_RESOURCE_KEY));
	}

	@Test
	public void testPartition1() {
		final long testLines = 100;
		setResource(testLines, 100, false);
		
		Map<String, ExecutionContext> partition = partitioner.partition(1);
		assertEquals(1, partition.size());
		
		assertPartition(0L, testLines, partition, "0");
	}
	
	@Test
	public void testPartition2() {
		final long testLines = 100;
		setResource(testLines, 10, true);

		Map<String, ExecutionContext> partition = partitioner.partition(2);
		assertEquals(2, partition.size());

		assertPartition(0L, testLines/2+1, partition, "0");
		assertPartition((testLines/2+1)*11, testLines/2-1, partition, "1");
	}
	
	@Test
	public void testPartition20() {
		setResource(5, 10, false);

		Map<String, ExecutionContext> partition = partitioner.partition(20);
		assertEquals(5, partition.size());

		assertPartition(0L, 1L, partition, "00");
		assertPartition(11L, 1L, partition, "01");
		assertPartition(22L, 1L, partition, "02");
		assertPartition(33L, 1L, partition, "03");
		assertPartition(44L, 1L, partition, "04");
	}
	
	@Test
	public void testPartitionEmpty() {
		setResource(0, 0, false);
		Map<String, ExecutionContext> partition = partitioner.partition(3);
		assertEquals(0, partition.size());		
	}

	@Test
	public void testPartitionFewBytes() {
		setResource(1, 5, false);
		Map<String, ExecutionContext> partition = partitioner.partition(10);
		assertEquals(1, partition.size());

		assertPartition(0L, 1L, partition, "00");
	}

	@Test
	public void testPartitionMoreBytesUneven() {
		final int gridSize = 50;
		final long lines = 99;
		final int strLen = 100;
		final long maxPartitionItems = lines / gridSize + 1;
		final long minPartitionItems = lines / gridSize;
		setResource(lines, strLen, true);	// totally 9999 bytes
		Map<String, ExecutionContext> partition = partitioner.partition(gridSize);
		assertEquals(gridSize, partition.size());
		
		long itemsTotal = 0;
		long prevStartAt = 0;
		long prevItems = 0;
		for( int i=0; i<gridSize; i++) {
			ExecutionContext ex = partition.get(String.format("%s%02d", FlatFilePartitioner.DEFAULT_PARTITION_PREFIX, i));
			long items = (Long) ex.get(FlatFilePartitioner.DEFAULT_ITEMS_COUNT_KEY);
			long startAt = (Long) ex.get(FlatFilePartitioner.DEFAULT_START_AT_KEY);
			itemsTotal += items;
			
			assertTrue( String.format("Partition %d has unbalanced number of items: %d, where %d to %d is expected", i, items, minPartitionItems, maxPartitionItems), 
					items >= minPartitionItems && items <= maxPartitionItems );
			
			assertEquals( prevStartAt + prevItems * (strLen + 1) , startAt );
			prevStartAt = startAt;
			prevItems = items;
		}
		assertEquals(lines, itemsTotal);
	}

	@Test
	public void testCountLines() throws IOException {
		InputStream in = FileUtils.openInputStream(testFile);
		long lineCount = FlatFilePartitioner.countLines(in);
		assertEquals(LINE_COUNT, lineCount);
	}

	@Test
	public void testEmptyFile() throws IOException {
		InputStream in = FileUtils.openInputStream(emptyFile);
		long lineCount = FlatFilePartitioner.countLines(in);
		assertEquals(0L, lineCount);
	}

	@Test
	public void testOneLineFile() throws IOException {
		InputStream in = FileUtils.openInputStream(oneLineFile);
		long lineCount = FlatFilePartitioner.countLines(in);
		assertEquals(1L, lineCount);
	}

	@Test
	public void testTwoLineFile() throws IOException {
		InputStream in = FileUtils.openInputStream(twoLineFile);
		long lineCount = FlatFilePartitioner.countLines(in);
		assertEquals(2L, lineCount);
	}

	/**
	 * An in memory implementation of Spring's {@link org.springframework.core.io.Resource} interface.
	 * <p>Used to feed the {@link FlatFilePartitionerTests} with String data rather than creating physical files for each test.</p>
	 *
	 * @author Luke Taylor
	 * @author Sergey Shcherbakov
	 */
	private static class InMemoryResource extends AbstractResource {
	    private final byte[] source;
	    private final String description;
	    
	    public InMemoryResource(String source) {
	        this(source.getBytes());
	    }

	    public InMemoryResource(byte[] source) {
	        this(source, null);
	    }

	    public InMemoryResource(byte[] source, String description) {
	        Assert.notNull(source);
	        this.source = source;
	        this.description = description;
	    }
	    
	    public String getDescription() {
	        return description;
	    }

	    public InputStream getInputStream() throws IOException {
	        return new ByteArrayInputStream(source);
	    }

	    public int hashCode() {
	        return 1;
	    }

	    public boolean equals(Object res) {
	        if (!(res instanceof InMemoryResource)) {
	            return false;
	        }

	        return Arrays.equals(source, ((InMemoryResource)res).source);
	    }

		@Override
		public File getFile() throws IOException {
			return new File(TMP_PATH);
		}
		@Override
		public boolean exists() {
			return true;
		}
		@Override
		public boolean isReadable() {
			return true;
		}
	}
}
