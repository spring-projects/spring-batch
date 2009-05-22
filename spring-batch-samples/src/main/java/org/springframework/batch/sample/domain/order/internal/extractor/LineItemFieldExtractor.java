package org.springframework.batch.sample.domain.order.internal.extractor;

import org.springframework.batch.item.file.transform.FieldExtractor;
import org.springframework.batch.sample.domain.order.LineItem;

/**
 * @author Dan Garrette
 * @since 2.0.1
 */
public class LineItemFieldExtractor implements FieldExtractor<LineItem> {

	public Object[] extract(LineItem item) {
		return new Object[] { "ITEM:", item.getItemId(), item.getPrice() };
	}

}
