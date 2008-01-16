package org.springframework.batch.sample.mapping;

import java.util.Calendar;

import org.springframework.batch.io.file.mapping.DefaultFieldSet;
import org.springframework.batch.io.file.mapping.FieldSet;
import org.springframework.batch.io.file.mapping.FieldSetMapper;
import org.springframework.batch.sample.domain.Order;

public class HeaderFieldSetMapperTests extends AbstractFieldSetMapperTests {

	private static final long ORDER_ID = 1;
	private static final String DATE = "2007-01-01";

	protected Object expectedDomainObject() {
		Order order = new Order();
		Calendar calendar = Calendar.getInstance();
		calendar.set(2007, 0, 1, 0, 0, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		order.setOrderDate(calendar.getTime());
		order.setOrderId(ORDER_ID);
		return order;
	}

	protected FieldSet fieldSet() {
		String[] tokens = new String[]{
				String.valueOf(ORDER_ID), 
				DATE
		};
		String[] columnNames = new String[]{
				HeaderFieldSetMapper.ORDER_ID_COLUMN,
				HeaderFieldSetMapper.ORDER_DATE_COLUMN
		};
		return new DefaultFieldSet(tokens, columnNames);
	}

	protected FieldSetMapper fieldSetMapper() {
		return new HeaderFieldSetMapper();
	}

}
