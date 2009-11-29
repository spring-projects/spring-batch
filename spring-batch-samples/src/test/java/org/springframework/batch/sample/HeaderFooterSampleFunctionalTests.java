package org.springframework.batch.sample;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/headerFooterSample.xml", "/job-runner-context.xml" })
public class HeaderFooterSampleFunctionalTests {

	@Autowired
	@Qualifier("inputResource")
	private Resource input;
	
	@Autowired
	@Qualifier("outputResource")
	private Resource output;
	
	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Test
	public void testJob() throws Exception {
		jobLauncherTestUtils.launchJob();

		BufferedReader inputReader = new BufferedReader(new FileReader(input.getFile()));
		BufferedReader outputReader = new BufferedReader(new FileReader(output.getFile()));

		// skip initial comment from input file
		inputReader.readLine();

		String line;

		int lineCount = 0;
		while ((line = inputReader.readLine()) != null) {
			lineCount++;
			assertTrue("input line should correspond to output line", outputReader.readLine().contains(line));
		}

		// footer contains the item count
		int itemCount = lineCount - 1; // minus 1 due to header line
		assertTrue(outputReader.readLine().contains(String.valueOf(itemCount)));
	}

}
