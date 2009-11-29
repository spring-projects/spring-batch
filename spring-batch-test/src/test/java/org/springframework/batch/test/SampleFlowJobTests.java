package org.springframework.batch.test;

import org.junit.runner.RunWith;
import org.springframework.batch.core.job.flow.FlowJob;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This class will specifically test the capabilities of
 * {@link JobRepositoryTestUtils} to test {@link FlowJob}s.
 * 
 * @author Dan Garrette
 * @since 2.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/jobs/sampleFlowJob.xml")
public class SampleFlowJobTests extends AbstractSampleJobTests {
	
}
