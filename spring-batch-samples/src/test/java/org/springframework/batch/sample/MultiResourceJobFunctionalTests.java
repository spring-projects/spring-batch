package org.springframework.batch.sample;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.sql.DataSource;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.sample.domain.trade.Trade;
import org.springframework.batch.sample.domain.trade.internal.TradeFieldSetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class MultiResourceJobFunctionalTests extends AbstractValidatingBatchLauncherTests {

	// expected line length in input file (sum of pattern lengths + 2, because
	// the counter is appended twice)
	private static final int LINE_LENGTH = 29;

	// auto-injected attributes
	private SimpleJdbcTemplate simpleJdbcTemplate;
	private Resource fileLocator;
	protected FlatFileItemReader<Trade> itemReader;
	private LineTokenizer lineTokenizer;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	@Autowired
	public void setLineTokenizer(LineTokenizer lineTokenizer) {
		this.lineTokenizer = lineTokenizer;
	}

	@Before
	public void onSetUp() throws Exception {
		simpleJdbcTemplate.update("delete from TRADE");
		fileLocator = new ClassPathResource(
				"data/multiResourceJob/input/20070122.teststream.ImportTradeDataStep.txt");
		itemReader = new FlatFileItemReader<Trade>();

		FieldSetMapper<Trade> mapper = new TradeFieldSetMapper();
		DefaultLineMapper<Trade> lineMapper = new DefaultLineMapper<Trade>();
		lineMapper.setLineTokenizer(lineTokenizer);
		lineMapper.setFieldSetMapper(mapper);
		itemReader.setLineMapper(lineMapper);

		itemReader.setResource(fileLocator);
		itemReader.open(new ExecutionContext());
	}

	/*
	 * fixed-length file is expected on input
	 */
	protected void validatePreConditions() throws Exception {
		BufferedReader reader;

		reader = new BufferedReader(new FileReader(fileLocator.getFile()));
		String line;
		while ((line = reader.readLine()) != null) {
			assertEquals(LINE_LENGTH, line.length());
		}
	}

	/**
	 * Context: 5 items overall, min. 2 items per output file, commitInterval=3, =>
	 * two files created, with 3 items in the first and two in second.
	 */
	@Override
	protected void validatePostConditions() throws Exception {
		File file1 = new File("target/test-outputs/multiResourceOutput.txt.1");
		File file2 = new File("target/test-outputs/multiResourceOutput.txt.2");
		assertTrue(file1.exists());
		assertTrue(file2.exists());

		BufferedReader reader1 = new BufferedReader(new FileReader(file1));
		for (int i = 1; i <= 3; i++) {
			assertEquals(itemReader.read().toString(), reader1.readLine());
		}
		assertNull(reader1.readLine());

		BufferedReader reader2 = new BufferedReader(new FileReader(file2));
		for (int i = 1; i <= 2; i++) {
			assertEquals(itemReader.read().toString(), reader2.readLine());
		}
		assertNull(reader2.readLine());

	}

	@Autowired
	public void setJob(@Qualifier("multiResourceJob")
	Job job) {
		super.setJob(job);
	}
}
