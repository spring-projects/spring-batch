package org.springframework.batch.sample.domain.trade.internal;

import java.math.BigDecimal;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.sample.domain.trade.Trade;
import org.springframework.batch.sample.support.AbstractFieldSetMapperTests;

public class TradeFieldSetMapperTests extends AbstractFieldSetMapperTests {

	private static final String CUSTOMER = "Mike Tomcat";

	private static final BigDecimal PRICE = new BigDecimal(1.3);

	private static final long QUANTITY = 7;

	private static final String ISIN = "fj893gnsalX";

	protected Object expectedDomainObject() {
		Trade trade = new Trade();
		trade.setIsin(ISIN);
		trade.setQuantity(QUANTITY);
		trade.setPrice(PRICE);
		trade.setCustomer(CUSTOMER);
		return trade;
	}

	protected FieldSet fieldSet() {
		String[] tokens = new String[4];
		tokens[TradeFieldSetMapper.ISIN_COLUMN] = ISIN;
		tokens[TradeFieldSetMapper.QUANTITY_COLUMN] = String.valueOf(QUANTITY);
		tokens[TradeFieldSetMapper.PRICE_COLUMN] = String.valueOf(PRICE);
		tokens[TradeFieldSetMapper.CUSTOMER_COLUMN] = CUSTOMER;

		return new DefaultFieldSet(tokens);
	}

	protected FieldSetMapper<Trade> fieldSetMapper() {
		return new TradeFieldSetMapper();
	}

}
