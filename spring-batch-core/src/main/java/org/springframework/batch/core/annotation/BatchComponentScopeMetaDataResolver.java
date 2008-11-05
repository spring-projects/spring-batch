/**
 * 
 */
package org.springframework.batch.core.annotation;

import java.util.Map;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationScopeMetadataResolver;
import org.springframework.context.annotation.ScopeMetadata;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * Implementation of the ScopeMetaDataResolver that checks for the {@link BatchComponent} 
 * annotation on the BeanDefinition passed in, and if found sets the scope to step.  The default
 * resolver, AnnotationScopeMetadataResolver is used for any classes that don't contain the
 * {@link BatchComponent} annotation.
 * 
 * @author Lucas Ward
 * @since 2.0
 * @see BeanDefinition
 * @see BatchComponent
 */
public class BatchComponentScopeMetaDataResolver implements
		ScopeMetadataResolver {

	ScopeMetadataResolver delegate = new AnnotationScopeMetadataResolver();
	
	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.annotation.ScopeMetadataResolver#resolveScopeMetadata(org.springframework.beans.factory.config.BeanDefinition)
	 */
	public ScopeMetadata resolveScopeMetadata(BeanDefinition definition) {
		
		if(definition instanceof AnnotatedBeanDefinition){
			AnnotatedBeanDefinition annDef = (AnnotatedBeanDefinition) definition;
			Map<String, Object> attributes =
				annDef.getMetadata().getAnnotationAttributes(BatchComponent.class.getName());
			if(attributes != null){
				ScopeMetadata metadata = new ScopeMetadata();
				metadata.setScopeName("step");
				metadata.setScopedProxyMode(ScopedProxyMode.INTERFACES);
				return metadata;
			}
		}
		
		return delegate.resolveScopeMetadata(definition);
	}

	
}
