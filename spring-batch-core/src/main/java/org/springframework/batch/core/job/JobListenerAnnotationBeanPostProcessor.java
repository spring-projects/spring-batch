/**
 * 
 */
package org.springframework.batch.core.job;

import java.lang.reflect.Method;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;


/**
 * {@link BeanPostProcessor} that registers any declared beans to the provided job
 * that have any methods annotated with {@link BeforeJob} or {@link AfterJob}
 * 
 * @author Lucas Ward
 * @since 2.0
 * @see SimpleJob
 */
public class JobListenerAnnotationBeanPostProcessor implements BeanPostProcessor {

	private AbstractJob job;
	
	public JobListenerAnnotationBeanPostProcessor(AbstractJob job) {
		this.job = job;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		return null;
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		
		for(Method method : bean.getClass().getMethods()){
			BeforeJob beforeJob = AnnotationUtils.findAnnotation(method, BeforeJob.class);
			if(beforeJob != null){
				JobExecutionListener listener = new BeforeJobProxy(method, bean, supportsJobExecutionPassing(method));
				job.registerJobExecutionListener(listener);
			}
			AfterJob afterJob = AnnotationUtils.findAnnotation(method, AfterJob.class);
			if(afterJob != null){
				JobExecutionListener listener = new AfterJobProxy(method, bean, supportsJobExecutionPassing(method));
				job.registerJobExecutionListener(listener);
			}
		}
		
		return null;
	}
	
	private class BeforeJobProxy extends JobExecutionListenerSupport{
		
		Method method;
		Object bean;
		boolean passThroughExecution;
		
		public BeforeJobProxy(Method method, Object bean, boolean passThroughExecution) {
			this.method = method;
			this.bean = bean;
			this.passThroughExecution = passThroughExecution;
		}
		
		@Override
		public void beforeJob(JobExecution jobExecution) {
			try{	
				if(passThroughExecution){
					method.invoke(bean, jobExecution);
				}
				else{
					method.invoke(bean);
				}
			}
			catch(Exception ex){
				throw new IllegalStateException("Unable to invoke annotated method: [" + method, ex);
			}
		}
	}
	
	private class AfterJobProxy extends JobExecutionListenerSupport{
		
		Method method;
		Object bean;
		boolean passThroughExecution;
		
		public AfterJobProxy(Method method, Object bean, boolean passThroughExecution) {
			this.method = method;
			this.bean = bean;
			this.passThroughExecution = passThroughExecution;
		}
		
		@Override
		public void afterJob(JobExecution jobExecution) {
			try{	
				if(passThroughExecution){
					method.invoke(bean, jobExecution);
				}
				else{
					method.invoke(bean);
				}
			}
			catch(Exception ex){
				throw new IllegalStateException("Unable to invoke annotated method: [" + method, ex);
			}
		}
	}
	
	private boolean supportsJobExecutionPassing(Method method){
		Class<?>[] parameters = method.getParameterTypes();
		if(parameters.length == 0 || parameters.length > 1){
			return false;
		}
		
		Class<?> parameter = parameters[0];
		if(parameter == JobExecution.class){
			return true;
		}
		
		return false;
	}
}
