package org.springframework.batch.sample.item.writer;

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.execution.scope.SimpleStepContext;
import org.springframework.batch.execution.scope.StepSynchronizationManager;
import org.springframework.batch.sample.item.writer.StagingItemWriter;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.util.ClassUtils;

public class StagingItemProcessorTests extends AbstractTransactionalDataSourceSpringContextTests {

	private StagingItemWriter writer;

	public void setProcessor(StagingItemWriter processor) {
		this.writer = processor;
	}

	protected String[] getConfigLocations() {
		return new String[] { ClassUtils.addResourcePathToPackagePath(StagingItemWriter.class,
				"staging-test-context.xml") };
	}

	protected void prepareTestInstance() throws Exception {
		SimpleStepContext stepScopeContext = new SimpleStepContext(new StepExecution(new StepInstance(new Long(11)),
				new JobExecution(new JobInstance(new Long(12), new JobParameters(), new Job("job")))));
		StepSynchronizationManager.register(stepScopeContext);
		super.prepareTestInstance();
	}

	public void testProcessInsertsNewItem() throws Exception {
		int before = getJdbcTemplate().queryForInt("SELECT COUNT(*) from BATCH_STAGING");
		writer.write("FOO");
		int after = getJdbcTemplate().queryForInt("SELECT COUNT(*) from BATCH_STAGING");
		assertEquals(before + 1, after);
	}

}
