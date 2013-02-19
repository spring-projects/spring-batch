package org.springframework.batch.sample.iosample;

import org.junit.runner.RunWith;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/jobs/iosample/repository.xml")
public class RepositoryFunctionalTests extends AbstractIoSampleTests {

	@Override
	protected void pointReaderToOutput(ItemReader<CustomerCredit> reader) {
		JobParameters jobParameters = new JobParametersBuilder(super.getUniqueJobParameters()).addDouble("credit", 0.)
				.toJobParameters();
		StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(jobParameters);
		StepSynchronizationManager.close();
		StepSynchronizationManager.register(stepExecution);
	}

	@Override
	protected JobParameters getUniqueJobParameters() {
		return new JobParametersBuilder(super.getUniqueJobParameters()).addString("credit", "10000").toJobParameters();
	}
}
