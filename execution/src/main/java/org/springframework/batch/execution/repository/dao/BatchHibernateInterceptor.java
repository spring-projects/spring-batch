package org.springframework.batch.execution.repository.dao;

import java.io.Serializable;

import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Hibernate interceptor for batch meta data. It can distinguish between the
 * various {@link JobIdentifier} strategies in a {@link JobInstance}, and can
 * truncate the exit descriptions. Its main task is to return the correct entity
 * name for a {@link JobInstance} based on the type of {@link JobIdentifier}
 * used. There is necessarily some tight coupling between this and the Hibernate
 * mappings for {@link JobInstance} because the map from {@link JobIdentifier}
 * type to entity name is in both places.
 * 
 * @author Dave Syer
 * 
 */
public class BatchHibernateInterceptor extends EmptyInterceptor implements
		InitializingBean {

	/**
	 * 
	 */
	private static final int EXIT_MESSAGE_LENGTH = 250;
	private EntityNameLocator entityNameLocator;

	/**
	 * Public setter for the {@link EntityNameLocator} property.
	 * 
	 * @param entityNameLocator
	 *            the entityNameLocator to set
	 */
	public void setEntityNameLocator(EntityNameLocator entityNameLocator) {
		this.entityNameLocator = entityNameLocator;
	}

	/**
	 * Check mandatory properties ({@link #entityNameLocator}).
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert
				.notNull(entityNameLocator,
						"EntityNameLocator must be provided.");
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

	/**
	 * Ensure that {@link JobExecution} and {@link StepExecution} have exit
	 * status with legal description (not too long).
	 * 
	 * @see org.hibernate.EmptyInterceptor#onFlushDirty(java.lang.Object,
	 *      java.io.Serializable, java.lang.Object[], java.lang.Object[],
	 *      java.lang.String[], org.hibernate.type.Type[])
	 */
	public boolean onFlushDirty(Object entity, Serializable id,
			Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types) {

		if (entity instanceof StepExecution || entity instanceof JobExecution) {
			int index = findExitStatus(propertyNames);
			ExitStatus status = (ExitStatus) currentState[index];
			String description = status == null ? "" : status
					.getExitDescription();
			if (description.length() > EXIT_MESSAGE_LENGTH) {
				status = status.addExitDescription(description.substring(0,
						EXIT_MESSAGE_LENGTH));
				currentState[index] = status;
				// state was modified...
				return true;
			}
		}

		return false;
	}

	/**
	 * @param propertyNames
	 * @return
	 */
	private int findExitStatus(String[] propertyNames) {
		for (int i = 0; i < propertyNames.length; i++) {
			if ("exitStatus".equals(propertyNames[i])) {
				return i;
			}
		}
		return -1;
	}

}
