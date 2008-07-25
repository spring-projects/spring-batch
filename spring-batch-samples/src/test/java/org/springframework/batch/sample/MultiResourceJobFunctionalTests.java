package org.springframework.batch.sample;

import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class MultiResourceJobFunctionalTests extends FixedLengthImportJobFunctionalTests {


	@Autowired
	public void setJob(@Qualifier("multiResourceJob") Job job) {
		super.setJob(job);
	}

}
