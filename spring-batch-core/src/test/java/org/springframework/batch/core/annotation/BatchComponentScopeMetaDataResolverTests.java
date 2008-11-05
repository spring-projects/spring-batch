/**
 * 
 */
package org.springframework.batch.core.annotation;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;

/**
 * @author Lucas Ward
 *
 */
public class BatchComponentScopeMetaDataResolverTests {

	AnnotatedBeanDefinition annBeanDef;
	AnnotationMetadata metaData;
	BatchComponentScopeMetaDataResolver resolver = new BatchComponentScopeMetaDataResolver();
	
	@Before
	public void init(){
		annBeanDef = createMock(AnnotatedBeanDefinition.class);		
	}
	
	@Test
	public void testNormalCase(){
		expect(annBeanDef.getMetadata()).andReturn(new StandardAnnotationMetadata(StubScopedClass.class));
		expect(annBeanDef.getMetadata()).andReturn(new StandardAnnotationMetadata(StubScopedClass.class));
		replay(annBeanDef);
		assertEquals("step",resolver.resolveScopeMetadata(annBeanDef).getScopeName());
		verify(annBeanDef);
	}
	
	@Test
	public void testBatchComponent(){
	
		expect(annBeanDef.getMetadata()).andReturn(new StandardAnnotationMetadata(TestComponent.class));
		replay(annBeanDef);
		assertEquals("step",resolver.resolveScopeMetadata(annBeanDef).getScopeName());
		verify(annBeanDef);
	}
	
	@Scope("step")
	private class StubScopedClass{}
}
