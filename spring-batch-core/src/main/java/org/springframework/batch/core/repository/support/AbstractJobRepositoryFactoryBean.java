package org.springframework.batch.core.repository.support;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.Assert;

/**
 * A {@link FactoryBean} that automates the creation of a
 * {@link SimpleJobRepository}. Declares abstract methods for providing DAO
 * object implementations.
 * 
 * @see JobRepositoryFactoryBean
 * @see MapJobRepositoryFactoryBean
 * 
 * @author Ben Hale
 * @author Lucas Ward
 * @author Robert Kasanicky
 */
public abstract class AbstractJobRepositoryFactoryBean implements FactoryBean, InitializingBean {

	/**
	 * Default value for isolation level in create* method.
	 */
	private static final String DEFAULT_ISOLATION_LEVEL = "ISOLATION_SERIALIZABLE";

	private ProxyFactory proxyFactory;

	private String isolationLevelForCreate = DEFAULT_ISOLATION_LEVEL;

	private PlatformTransactionManager transactionManager;

	/**
	 * @return fully configured {@link JobInstanceDao} implementation.
	 */
	protected abstract JobInstanceDao createJobInstanceDao() throws Exception;

	/**
	 * @return fully configured {@link JobExecutionDao} implementation.
	 */
	protected abstract JobExecutionDao createJobExecutionDao() throws Exception;

	/**
	 * @return fully configured {@link StepExecutionDao} implementation.
	 */
	protected abstract StepExecutionDao createStepExecutionDao() throws Exception;

	public Object getObject() throws Exception {
		return proxyFactory.getProxy();
	}

	/**
	 * The type of object to be returned from {@link #getObject()}.
	 * 
	 * @return JobRepository.class
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	public Class<JobRepository> getObjectType() {
		return JobRepository.class;
	}

	public boolean isSingleton() {
		return true;
	}

	public void afterPropertiesSet() throws Exception {

		Assert.notNull(transactionManager, "TransactionManager must not be null.");

		initializeProxy();
	}

	protected void initializeProxy() throws Exception {
		proxyFactory = new ProxyFactory();
		TransactionInterceptor advice = new TransactionInterceptor(transactionManager, PropertiesConverter
				.stringToProperties("create*=PROPAGATION_REQUIRES_NEW," + isolationLevelForCreate
						+ "\n*=PROPAGATION_REQUIRED"));
		DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(advice);
		NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
		pointcut.addMethodName("*");
		advisor.setPointcut(pointcut);
		proxyFactory.addAdvisor(advisor);
		proxyFactory.setProxyTargetClass(false);
		proxyFactory.addInterface(JobRepository.class);
		proxyFactory.setTarget(getTarget());
	}

	private Object getTarget() throws Exception {
		return new SimpleJobRepository(createJobInstanceDao(), createJobExecutionDao(), createStepExecutionDao());
	}

	/**
	 * Public setter for the {@link PlatformTransactionManager}.
	 * @param transactionManager the transactionManager to set
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * Public setter for the isolation level to be used for the transaction when
	 * job execution entities are initially created. The default is
	 * ISOLATION_SERIALIZABLE, which prevents accidental concurrent execution of
	 * the same job (ISOLATION_REPEATABLE_READ would work as well).
	 * 
	 * @param isolationLevelForCreate the isolation level name to set
	 * 
	 * @see SimpleJobRepository#createJobExecution(org.springframework.batch.core.Job,
	 * org.springframework.batch.core.JobParameters)
	 */
	public void setIsolationLevelForCreate(String isolationLevelForCreate) {
		this.isolationLevelForCreate = isolationLevelForCreate;
	}

}
