package org.springframework.batch.core.launch.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple implementation of the {@link JobLauncher} interface. The Spring Core
 * {@link TaskExecutor} interface is used to launch a {@link Job}. This means
 * that the type of executor set is very important. If a
 * {@link SyncTaskExecutor} is used, then the job will be processed
 * <strong>within the same thread that called the launcher.</strong> Care should
 * be taken to ensure any users of this class understand fully whether or not
 * the implementation of TaskExecutor used will start tasks synchronously or
 * asynchronously. The default setting uses a synchronous task executor.
 *
 * There are two required dependencies of this Launcher, a
 * {@link JobRepository} and {@link RetryTemplate}. The JobRepository is used to
 * obtain a valid JobExecution. The Repository must be used because the provided
 * {@link Job} could be a restart of an existing {@link JobInstance}, and only the
 * Repository can reliably recreate it. The RetryTemplate is used to automatically
 * restart and retry the failed job.
 *
 * @see JobRepository
 * @see TaskExecutor
 */
public class FaultTolerantJobLauncher implements JobLauncher, InitializingBean {

    protected static final Log logger = LogFactory.getLog(FaultTolerantJobLauncher.class);

    private JobRepository jobRepository;

    private RetryTemplate retryTemplate;

    private TaskExecutor taskExecutor;

    /**
     * Run the provided job with the given {@link JobParameters}. The
     * {@link JobParameters} will be used to determine if this is an execution
     * of an existing job instance, or if a new one should be created.
     *
     * @param job the job to be run.
     * @param jobParameters the {@link JobParameters} for this particular
     * execution.
     * @return JobExecutionAlreadyRunningException if the JobInstance already
     * exists and has an execution already running.
     * @throws JobRestartException if the execution would be a re-start, but a
     * re-start is either not allowed or not needed.
     * @throws JobInstanceAlreadyCompleteException if this instance has already
     * completed successfully
     * @throws JobParametersInvalidException
     */
    @Override
    public JobExecution run(final Job job, final JobParameters jobParameters)
            throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException,
            JobParametersInvalidException {

        Assert.notNull(job, "The Job must not be null.");
        Assert.notNull(jobParameters, "The JobParameters must not be null.");

        final AtomicReference<JobExecution> executionReference = new AtomicReference<>();
        JobExecution lastExecution = jobRepository.getLastJobExecution(job.getName(), jobParameters);
        if (lastExecution != null) {
            if (!job.isRestartable()) {
                throw new JobRestartException("JobInstance already exists and is not restartable");
            }
			/*
			 * validate here if it has stepExecutions that are UNKNOWN, STARTING, STARTED and STOPPING
			 * retrieve the previous execution and check
			 */
            for (StepExecution execution : lastExecution.getStepExecutions()) {
                BatchStatus status = execution.getStatus();
                if (status.isRunning() || status == BatchStatus.STOPPING) {
                    throw new JobExecutionAlreadyRunningException("A job execution for this job is already running: "
                            + lastExecution);
                } else if (status == BatchStatus.UNKNOWN) {
                    throw new JobRestartException(
                            "Cannot restart step [" + execution.getStepName() + "] from UNKNOWN status. "
                                    + "The last execution ended with a failure that could not be rolled back, "
                                    + "so it may be dangerous to proceed. Manual intervention is probably necessary.");
                }
            }
        }

        // Check the validity of the parameters before doing creating anything
        // in the repository...
        job.getJobParametersValidator().validate(jobParameters);

        taskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    retryTemplate.execute(new FaultTolerantJobRetryCallback(executionReference, job, jobParameters));
                } catch (TaskRejectedException e) {
                    executionReference.get().upgradeStatus(BatchStatus.FAILED);
                    if (executionReference.get().getExitStatus().equals(ExitStatus.UNKNOWN)) {
                        executionReference.get().setExitStatus(ExitStatus.FAILED.addExitDescription(e));
                    }
                    jobRepository.update(executionReference.get());
                }
            }
        });

        return executionReference.get();
    }

    /**
     * Set the JobRepsitory.
     *
     * @param jobRepository
     */
    public void setJobRepository(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    /**
     * Set the retryTemplate
     *
     * @param retryTemplate
     */
    public void setRetryTemplate(RetryTemplate retryTemplate) {
        this.retryTemplate = retryTemplate;
    }

    /**
     * Set the TaskExecutor. (Optional)
     *
     * @param taskExecutor
     */
    public void setTaskExecutor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    /**
     * Ensure the required dependencies of a {@link JobRepository} have been
     * set.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.state(jobRepository != null, "A JobRepository has not been set.");
        Assert.state(retryTemplate != null, "A RetryTemplate has not been set.");
        if (taskExecutor == null) {
            logger.info("No TaskExecutor has been set, defaulting to synchronous executor.");
            taskExecutor = new SyncTaskExecutor();
        }
    }

    private class FaultTolerantJobRetryCallback implements RetryCallback<Object, RuntimeException> {

        private final AtomicReference<JobExecution> executionReference;
        private final Job job;
        private final JobParameters jobParameters;

        FaultTolerantJobRetryCallback(AtomicReference<JobExecution> executionReference, Job job, JobParameters jobParameters){
            this.executionReference = executionReference;
            this.job = job;
            this.jobParameters = jobParameters;
        }

        @Override
        public Object doWithRetry(RetryContext retryContext) {
            if(!job.isRestartable()){
                //will be set only once and in case that job can not be restarted we don't retry
                retryContext.setExhaustedOnly();
            }

            if(retryContext.getRetryCount() > 0){
                logger.info("Job: [" + job + "] retrying/restarting with the following parameters: [" + jobParameters
                        + "]");
            }

            try {
                /*
                 * There is a very small probability that a non-restartable job can be
                 * restarted, but only if another process or thread manages to launch
                 * <i>and</i> fail a job execution for this instance between the last
                 * assertion and the next method returning successfully.
                 */
                executionReference.set(jobRepository.createJobExecution(job.getName(), jobParameters));
                logger.info("Job: [" + job + "] launched with the following parameters: [" + jobParameters
                        + "]");
                job.execute(executionReference.get());
                logger.info("Job: [" + job + "] completed with the following parameters: [" + jobParameters
                        + "] and the following status: [" + executionReference.get().getStatus() + "]");
            }
            catch (JobInstanceAlreadyCompleteException | JobExecutionAlreadyRunningException e){
                retryContext.setExhaustedOnly();    //don't repeat if instance already complete or running
                rethrow(e);
            }
            catch (Throwable t) {
                logger.info("Job: [" + job
                        + "] failed unexpectedly and fatally with the following parameters: [" + jobParameters
                        + "]", t);
                rethrow(t);
            }

            if(job.isRestartable() && executionReference.get().getStatus() == BatchStatus.FAILED){
                //if job failed and can be restarted, use retry template to restart the job
                throw new TaskRejectedException("RetryTemplate failed too many times");
            }

            return null;
        }

        private void rethrow(Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            else if (t instanceof Error) {
                throw (Error) t;
            }
            throw new IllegalStateException(t);
        }
    }
}
