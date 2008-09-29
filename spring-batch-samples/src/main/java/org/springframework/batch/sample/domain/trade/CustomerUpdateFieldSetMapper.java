/**
 * 
 */
package org.springframework.batch.sample.domain.trade;

import java.math.BigDecimal;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.item.file.mapping.FieldSet;
import org.springframework.batch.item.file.mapping.FieldSetMapper;

/**
 * {@link FieldSetMapper} for mapping the 
 * 
 * @author Lucas Ward
 *
 */
public class CustomerUpdateFieldSetMapper extends StepExecutionListenerSupport implements FieldSetMapper<CustomerUpdate> {

	StepExecution stepExecution;
	
	public CustomerUpdate mapLine(FieldSet fs) {
		
		char code = fs.readChar(0);
		if(code == 'F'){
			long customerUpdateTotal = stepExecution.getReadCount();
			long fileUpdateTotal = fs.readLong(1);
			if(customerUpdateTotal != fileUpdateTotal){
				throw new IllegalStateException("The total number of customer updates in the file footer does not match the " +
						"number entered  File footer total: [" + fileUpdateTotal + "] Total encountered during processing: [" +
						customerUpdateTotal + "]");
			}
			else{
				//return null, because the footer indicates an end of processing.
				return null;
			}
		}
		
		CustomerOperation operation = CustomerOperation.fromCode(code);
		String name = fs.readString(1);
		BigDecimal credit = fs.readBigDecimal(2);
		
		return  new CustomerUpdate(operation, name, credit);
	}
	
	@Override
	public void beforeStep(StepExecution stepExecution) {

		this.stepExecution = stepExecution;
	}	
}
