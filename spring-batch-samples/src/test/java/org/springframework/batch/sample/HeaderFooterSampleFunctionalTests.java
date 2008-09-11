package org.springframework.batch.sample;

import java.io.BufferedReader;
import java.io.FileReader;

import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class HeaderFooterSampleFunctionalTests extends AbstractValidatingBatchLauncherTests {

	@Override
	protected void validatePostConditions() throws Exception {
		Resource input = (Resource) applicationContext.getBean("inputResource", Resource.class);
		Resource output = (Resource) applicationContext.getBean("outputResource", Resource.class);

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
