package org.springframework.batch.item.database;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.ClearFailedException;
import org.springframework.util.Assert;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.dao.DataAccessResourceFailureException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * {@link org.springframework.batch.item.ItemWriter} that is aware of the JPA EntityManagerFactory and can
 * take some responsibilities to do with chunk boundaries away from a less smart
 * {@link org.springframework.batch.item.ItemWriter} (the delegate). A delegate is required, and will be used
 * to do the actual writing of the item.<br/>
 *
 * It is required that {@link #write(Object)} is called inside a transaction,
 * and that {@link #flush()} is then subsequently called before the transaction
 * commits, or {@link #clear()} before it rolls back.<br/>
 *
 * The reader must be configured with an {@link javax.persistence.EntityManagerFactory} that is capable
 * of participating in Spring managed transactions.
 * 
 * The writer is thread safe after its properties are set (normal singleton
 * behaviour), so it can be used to write in multiple concurrent transactions.
 * Note, however, that the set of failed items is stored in a collection
 * internally, and this collection is never cleared, so it is not a great idea
 * to go on using the writer indefinitely. Normally it would be used for the
 * duration of a batch job and then discarded.
 *
 * @author Dave Syer
 * @author Thomas Risberg
 *
 */
public class JpaAwareItemWriter<T> extends AbstractTransactionalResourceItemWriter<T> implements InitializingBean {

	/**
	 * Key for items processed in the current transaction {@link org.springframework.batch.repeat.RepeatContext}.
	 */
	private static final String ITEMS_PROCESSED = JpaAwareItemWriter.class.getName() + ".ITEMS_PROCESSED";

	private ItemWriter<? super T> delegate;

	private EntityManagerFactory entityManagerFactory;

	/**
	 * Public setter for the {@link org.springframework.batch.item.ItemWriter} property.
	 *
	 * @param delegate the delegate to set
	 */
	public void setDelegate(ItemWriter<? super T> delegate) {
		this.delegate = delegate;
	}

	/**
	 * Set the EntityManager to be used internally.
	 *
	 * @param entityManagerFactory the entityManagerFactory to set
	 */
	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	/**
	 * Check mandatory properties - there must be a delegate and entityManagerFactory.
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(delegate, "An ItemWriter to be used as a delegate is required.");
		Assert.notNull(entityManagerFactory, "An EntityManagerFactory is required");
	}

	/**
	 * Delegate to subclass and flush the EntityManager.
	 */
	protected void doFlush() {
		delegate.flush();
		EntityManager entityManager =
				EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
		if (entityManager == null) {
			throw new DataAccessResourceFailureException("Unable to obtain a transactional EntityManager");
		}
		entityManager.flush();
		entityManager.clear();
	}

	/**
	 * Call the delegate clear() method, and then clear the EntityManager.
	 */
	protected void doClear() throws ClearFailedException {
		delegate.clear();
		EntityManager entityManager =
				EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
		if (entityManager == null) {
			throw new DataAccessResourceFailureException("Unable to obtain a transactional EntityManager");
		}
		entityManager.clear();
	}

	protected String getResourceKey() {
		return ITEMS_PROCESSED;
	}

	protected void doWrite(T item) throws Exception {
		delegate.write(item);
	}

}
