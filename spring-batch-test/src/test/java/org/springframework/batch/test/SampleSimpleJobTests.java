package org.springframework.batch.test;

import org.junit.runner.RunWith;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This class will specifically test the capabilities of
 * {@link JobRepositoryTestUtils} to test {@link SimpleJob}s.
 * 
 * @author Dan Garrette
 * @since 2.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/jobs/sampleSimpleJob.xml")
public class SampleSimpleJobTests extends AbstractSampleJobTests {

}
