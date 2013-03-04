package org.springframework.batch.item;

import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;

/**
 * Marker interface indicating that an item should have the item count set on it. Typically used within
 * an {@link AbstractItemCountingItemStreamItemReader}.
 *
 * @author Jimmy Praet
 */
public interface ItemCountAware {
	
	void setItemCount(int count);

}
