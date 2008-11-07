/**
 * 
 */
package org.springframework.batch.core.annotation;

import java.lang.reflect.Method;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;


/**
 * @author Lucas Ward
 * 
 */
public class StepComponentBeanPostProcessor implements BeanPostProcessor, Ordered {

	private AbstractStep step;
	private String basePackage;

	public StepComponentBeanPostProcessor(AbstractStep step, String basePackage) {
		this.step = step;
		this.basePackage = basePackage;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		// if it implements the StepExecutionListener interface, we'll register
		// it directly as such

		if (bean instanceof StepExecutionListener) {
			step.registerStepExecutionListener((StepExecutionListener) bean);
		}

		Class<?> clazz = bean.getClass();
		if(bean instanceof FactoryBean){
			clazz = ((FactoryBean)bean).getObjectType();
		}

		// If the class isn't of the correct package, isn't annotated with the
		// BatchComponent annotation, we won't put it in.
		if (!clazz.getName().startsWith(basePackage)
				|| AnnotationUtils.findAnnotation(clazz, BatchComponent.class) == null) {
			return bean;
		}

		ProxyFactory proxyFactory = new ProxyFactory(
				new Class[] { StepExecutionListener.class });

		StepExecutionListenerMixin listenerMixin = new StepExecutionListenerMixin(bean);
		
		Method[] methods = clazz.getMethods();
		for (Method method : methods) {
			if(AnnotationUtils.findAnnotation(method, BeforeStep.class) != null){
				listenerMixin.setBeforeStepMethod(method);
			}
			
			if(AnnotationUtils.findAnnotation(method, AfterStep.class) != null){
				listenerMixin.setAfterStepMethod(method);
			}
		}
		
		proxyFactory.addAdvisor(new DefaultIntroductionAdvisor(listenerMixin, StepExecutionListener.class));
		StepExecutionListener listener = (StepExecutionListener)proxyFactory.getProxy();
		step.registerStepExecutionListener(listener);

		return null;
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {

		return bean;
	}
	
	private class StepExecutionListenerMixin extends DelegatingIntroductionInterceptor implements StepExecutionListener{

		Object bean;
		Method afterStepMethod;
		Method beforeStepMethod;
		
		public StepExecutionListenerMixin(Object bean) {
			this.bean = bean;
		}
		
		public void setBeforeStepMethod(Method beforeStepMethod) {
			this.beforeStepMethod = beforeStepMethod;
		}
		
		public void setAfterStepMethod(Method afterStepMethod) {
			this.afterStepMethod = afterStepMethod;
		}
		
		public ExitStatus afterStep(StepExecution stepExecution) {
			if(afterStepMethod != null){
				invokeMethod(afterStepMethod, bean);
			}
			return null;
		}

		public void beforeStep(StepExecution stepExecution) {
			if(beforeStepMethod != null){
				invokeMethod(beforeStepMethod, bean);
			}
		}
	}
	
	private Object invokeMethod(Method method, Object bean, Object... args){
		
		try {
			return method.invoke(bean, args);
		} catch (Exception e) {
			if(e instanceof RuntimeException){
				throw (RuntimeException)e;
			}
			throw new IllegalArgumentException("Failed to invoke method: [" + method + "]");
		} 
	}

	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}
	
}
