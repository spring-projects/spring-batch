package org.springframework.batch.execution.repository.dao;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.EmptyInterceptor;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.execution.runtime.DefaultJobIdentifier;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifier;
import org.springframework.util.ClassUtils;

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
public class EntityNameInterceptor extends EmptyInterceptor {

	private static final String SIMPLE_JOB_INSTANCE = "SimpleJobInstance";
	private Map identifierTypes;
	private Set entrySet;

	public EntityNameInterceptor() {
		Map types = new HashMap();
		types.put(ScheduledJobIdentifier.class,
				"ScheduledJobInstance");
		types.put(DefaultJobIdentifier.class, "DefaultJobInstance");
		setIdentifierTypes(types);
	};

	/**
	 * Public setter for the identifier types. A map from Class (the
	 * {@link JobIdentifier} implementation) to String (the entity name). If a
	 * map from String to String is provided it will be interpreted as a map
	 * from class name to entity name.
	 * 
	 * @throws IllegalArgumentException
	 *             if a String key is provided that is not a Class name.
	 * 
	 * @param types
	 *            the identifierTypes to set
	 */
	public void setIdentifierTypes(Map types) {

		this.identifierTypes = new HashMap();
		for (Iterator iterator = types.entrySet().iterator(); iterator
				.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			Object key = entry.getKey();
			if (key instanceof Class) {
				identifierTypes.put(key, entry.getValue());
			} else {
				try {
					Class classKey = ClassUtils.forName(key.toString());
					identifierTypes.put(classKey, entry.getValue());
				} catch (ClassNotFoundException e) {
					throw new IllegalArgumentException(
							"Could not convert key in identifierTypes to type Class: ["
									+ key + "]", e);
				}
			}
		}

		this.entrySet = new TreeSet(new ClassComparator());
		entrySet.addAll(identifierTypes.keySet());

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

			for (Iterator iterator = entrySet.iterator(); iterator.hasNext();) {
				Class key = (Class) iterator.next();
				if (key.isAssignableFrom(instance.getIdentifier().getClass())) {
					return (String) identifierTypes.get(key);
				}
			}
			return SIMPLE_JOB_INSTANCE;
		}
		return super.getEntityName(object);
	}

	/**
	 * Comparator for classes to order by inheritance.
	 * 
	 * @author Dave Syer
	 * 
	 */
	private class ClassComparator implements Comparator {
		/**
		 * @return 1 if arg0 is assignable from arg1
		 * @return -1 otherwise
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Object arg0, Object arg1) {
			Class cls0 = (Class) arg0;
			Class cls1 = (Class) arg1;
			if (cls0.isAssignableFrom(cls1)) {
				return 1;
			}
			return -1;
		}
	}

}
