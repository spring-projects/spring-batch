package org.springframework.batch.sandbox;

import org.springframework.batch.item.ItemWriter;

import java.util.List;


public class LoggingItemWriter implements ItemWriter<Customer> {
	public void write(List<? extends Customer> items) throws Exception {

		System.out.println();

		for( Customer c : items)
		   System.out.println( "customer = " + c.toString());
		
		System.out.println( "----------------------------------------------------------------------------------------" );
	}
}
