package org.springframework.batch.sample;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/nonSequentialJob.xml" })
public class NonSequentialJobFunctionalTests extends NonSequentialJobFunctionalTestsBase {
}
