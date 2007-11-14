package org.springframework.batch.execution.repository.dao;

import org.hibernate.EmptyInterceptor;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Hibernate interceptor that can distinguish between the various
 * {@link JobIdentifier} strategies in a {@link JobInstance}. Its task is to
 * return the correct entity name based on the type of {@link JobIdentifier}
 * used. There is necessarily some tight coupling between this and the Hibernate
 * mappings for {@link JobInstance} because the map from {@link JobIdentifier}
 * type to entity name is in both places.
 * 
 * 
 * @author Dave Syer
 * 
 */
public class EntityNameInterceptor extends EmptyInterceptor implements InitializingBean {

	private EntityNameLocator entityNameLocator;

	/**
	 * Public setter for the {@link EntityNameLocator} property.
	 *
	 * @param entityNameLocator the entityNameLocator to set
	 */
	public void setEntityNameLocator(EntityNameLocator entityNameLocator) {
		this.entityNameLocator = entityNameLocator;
	}
	
	/**
	 * Check mandatory properties ({@link #entityNameLocator}).
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(entityNameLocator, "EntityNameLocator must be provided.");
	}

	/**
	 * If the object is a {@link JobInstance} search the identifier types for an
	 * entity name based on the {@link JobIdentifier} type. Fall back to
	 * SimpleJobIdentifier if the value is not found.
	 * 
	 * @see org.hibernate.EmptyInterceptor#getEntityName(java.lang.Object)
	 */
	public String getEntityName(Object object) {
		if (object instanceof JobInstance) {
			JobInstance instance = (JobInstance) object;
			return entityNameLocator
					.locate(instance.getIdentifier().getClass());
		}
		return super.getEntityName(object);
	}

}
