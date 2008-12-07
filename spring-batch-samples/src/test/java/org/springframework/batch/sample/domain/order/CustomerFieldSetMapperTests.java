package org.springframework.batch.sample.domain.order;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.sample.domain.order.internal.CustomerFieldSetMapper;
import org.springframework.batch.sample.support.AbstractFieldSetMapperTests;

public class CustomerFieldSetMapperTests extends AbstractFieldSetMapperTests {

	private static final boolean BUSINESS_CUSTOMER = false;
	//private static final String COMPANY_NAME = "Accenture";
	private static final String FIRST_NAME = "Jan";
	private static final String LAST_NAME = "Hrach";
	private static final String MIDDLE_NAME = "";
	private static final boolean REGISTERED = true;
	private static final long REG_ID = 1;
	private static final boolean VIP = true;

	protected Object expectedDomainObject() {
		Customer cs = new Customer();
		cs.setBusinessCustomer(BUSINESS_CUSTOMER);
		cs.setFirstName(FIRST_NAME);
		cs.setLastName(LAST_NAME);
		cs.setMiddleName(MIDDLE_NAME);
		cs.setRegistered(REGISTERED);
		cs.setRegistrationId(REG_ID);
		cs.setVip(VIP);
		return cs;
	}

	protected FieldSet fieldSet() {
		String[] tokens = new String[]{
				Customer.LINE_ID_NON_BUSINESS_CUST,
				FIRST_NAME,
				LAST_NAME,
				MIDDLE_NAME,
				CustomerFieldSetMapper.TRUE_SYMBOL,
				String.valueOf(REG_ID),
				CustomerFieldSetMapper.TRUE_SYMBOL};
		String[] columnNames = new String[]{
				CustomerFieldSetMapper.LINE_ID_COLUMN,
				CustomerFieldSetMapper.FIRST_NAME_COLUMN,
				CustomerFieldSetMapper.LAST_NAME_COLUMN,
				CustomerFieldSetMapper.MIDDLE_NAME_COLUMN,
				CustomerFieldSetMapper.REGISTERED_COLUMN,
				CustomerFieldSetMapper.REG_ID_COLUMN,
				CustomerFieldSetMapper.VIP_COLUMN};
		
		return new DefaultFieldSet(tokens, columnNames);
	}

	protected FieldSetMapper<Customer> fieldSetMapper() {
		return new CustomerFieldSetMapper();
	}

}
