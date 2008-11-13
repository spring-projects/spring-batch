package org.springframework.batch.sample;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import static org.junit.Assert.*;

import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class MultiResourceJobFunctionalTests extends FixedLengthImportJobFunctionalTests {

	/**
	 * Context: 5 items overall, min. 2 items per output file, commitInterval=3,
	 * => two files created, with 3 items in the first and two in second.
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
	public void setJob(@Qualifier("multiResourceJob") Job job) {
		super.setJob(job);
	}

}
