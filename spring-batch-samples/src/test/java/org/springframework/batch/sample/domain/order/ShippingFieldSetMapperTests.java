package org.springframework.batch.sample.domain.order;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.sample.domain.order.internal.ShippingFieldSetMapper;
import org.springframework.batch.sample.support.AbstractFieldSetMapperTests;

public class ShippingFieldSetMapperTests extends AbstractFieldSetMapperTests{

	private static final String SHIPPER_ID = "1";
	private static final String SHIPPING_INFO = "most interesting and informative shipping info ever";
	private static final String SHIPPING_TYPE_ID = "X";

	protected Object expectedDomainObject() {
		ShippingInfo info = new ShippingInfo();
		info.setShipperId(SHIPPER_ID);
		info.setShippingInfo(SHIPPING_INFO);
		info.setShippingTypeId(SHIPPING_TYPE_ID);
		return info;
	}

	protected FieldSet fieldSet() {
		String[] tokens = new String[]{SHIPPER_ID, SHIPPING_INFO, SHIPPING_TYPE_ID};
		String[] columnNames = new String[]{
				ShippingFieldSetMapper.SHIPPER_ID_COLUMN,
				ShippingFieldSetMapper.ADDITIONAL_SHIPPING_INFO_COLUMN,
				ShippingFieldSetMapper.SHIPPING_TYPE_ID_COLUMN
		};
		return new DefaultFieldSet(tokens, columnNames);
	}

	protected FieldSetMapper<ShippingInfo> fieldSetMapper() {
		return new ShippingFieldSetMapper();
	}

}
