/**
 * 
 */
package org.springframework.batch.core.annotation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import sun.reflect.generics.scope.Scope;

/**
 * @author Lucas Ward
 *
 */
public class BatchComponentIntegrationTests {
	
	@Test
	public void testWithoutResolver(){
		ApplicationContext context = new ClassPathXmlApplicationContext("org/springframework/batch/core/annotation/batch-component-context.xml");
		String[] beanNames = context.getBeanNamesForType(TestComponent.class);
		//There should be one instance of TextComponent in the Spring Container, because it's annotated with BatchComponent, and the filter
		//is setup.  However, the beanDefinition, should not have the scope on it, because there is no proxy set.
		assertEquals(1, beanNames.length);
		ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory)context.getAutowireCapableBeanFactory();
		BeanDefinition beanDef = beanFactory.getBeanDefinition(beanNames[0]);
		//Even though BatchComponent is annotated with StepScope, the scope will still be singleton without the resolver.
		assertEquals(BeanDefinition.SCOPE_SINGLETON, beanDef.getScope()); 
	}
	
	@Test
	public void testWithResolver(){
		
		ApplicationContext context = new ClassPathXmlApplicationContext("org/springframework/batch/core/annotation/batch-component-context-with-resolver.xml");
		String[] beanNames = context.getBeanNamesForType(TestComponent.class);
		//There should be one instance of TextComponent in the Spring Container, because it's annotated with BatchComponent, and the filter
		//is setup.  However, the beanDefinition, should not have the scope on it, because there is no proxy set.
		assertEquals(1, beanNames.length);
		ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory)context.getAutowireCapableBeanFactory();
		BeanDefinition beanDef = beanFactory.getBeanDefinition(beanNames[0]);
		//Even though BatchComponent is annotated with StepScope, the scope will still be singleton without the resolver.
		assertEquals("step", beanDef.getScope()); 
	}
	
	@Test
	public void testWithResolverNonScanned(){
		
		ApplicationContext context = new ClassPathXmlApplicationContext("org/springframework/batch/core/annotation/batch-component-context-with-resolver-nonscanned.xml");
		String[] beanNames = context.getBeanNamesForType(TestComponent.class);
		//There should be one instance of TextComponent in the Spring Container, because it's annotated with BatchComponent, and the filter
		//is setup.  However, the beanDefinition, should not have the scope on it, because there is no proxy set.
		assertEquals(1, beanNames.length);
		ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory)context.getAutowireCapableBeanFactory();
		BeanDefinition beanDef = beanFactory.getBeanDefinition(beanNames[0]);
		//Even though BatchComponent is annotated with StepScope, the scope will still be singleton without the resolver.
		assertEquals(BeanDefinition.SCOPE_SINGLETON, beanDef.getScope()); 
	}
	
	@Test
	public void testWithScopedComponent(){
		ApplicationContext context = new ClassPathXmlApplicationContext("org/springframework/batch/core/annotation/batch-component-context-scoped.xml");
		String[] beanNames = context.getBeanNamesForType(TestScopedComponent.class);
		//There should be one instance of TextComponent in the Spring Container, because it's annotated with BatchComponent, and the filter
		//is setup.  However, the beanDefinition, should not have the scope on it, because there is no proxy set.
		assertEquals(1, beanNames.length);
		ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory)context.getAutowireCapableBeanFactory();
		BeanDefinition beanDef = beanFactory.getBeanDefinition(beanNames[0]);
		//Even though BatchComponent is annotated with StepScope, the scope will still be singleton without the resolver.
		assertEquals(BeanDefinition.SCOPE_SINGLETON, beanDef.getScope()); 
	}
}
