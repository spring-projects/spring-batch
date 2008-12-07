package org.springframework.batch.sample.domain.order;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.sample.domain.order.internal.AddressFieldSetMapper;
import org.springframework.batch.sample.support.AbstractFieldSetMapperTests;


public class AddressFieldSetMapperTests extends AbstractFieldSetMapperTests {

	private static final String ADDRESSEE = "Jan Hrach";
	private static final String ADDRESS_LINE_1 = "Plynarenska 7c";
	private static final String ADDRESS_LINE_2 = "";
	private static final String CITY = "Bratislava";
	private static final String STATE = "";
	private static final String COUNTRY = "Slovakia";
	private static final String ZIP_CODE = "80000";
	
	

	protected Object expectedDomainObject() {
		Address address = new Address();
		address.setAddressee(ADDRESSEE);
		address.setAddrLine1(ADDRESS_LINE_1);
		address.setAddrLine2(ADDRESS_LINE_2);
		address.setCity(CITY);
		address.setState(STATE);
		address.setCountry(COUNTRY);
		address.setZipCode(ZIP_CODE);
		return address;
	}

	protected FieldSet fieldSet() {
		String[] tokens = 
			new String[]{ADDRESSEE, ADDRESS_LINE_1, ADDRESS_LINE_2, CITY, STATE, COUNTRY,  ZIP_CODE};
		String[] columnNames = 
			new String[]{
				AddressFieldSetMapper.ADDRESSEE_COLUMN, 
				AddressFieldSetMapper.ADDRESS_LINE1_COLUMN, 
				AddressFieldSetMapper.ADDRESS_LINE2_COLUMN, 
				AddressFieldSetMapper.CITY_COLUMN, 
				AddressFieldSetMapper.STATE_COLUMN, 
				AddressFieldSetMapper.COUNTRY_COLUMN, 
				AddressFieldSetMapper.ZIP_CODE_COLUMN };
		
		return  new DefaultFieldSet(tokens, columnNames);
	}

	protected FieldSetMapper<Address> fieldSetMapper() {
		return new AddressFieldSetMapper();
	}
}
