package org.springframework.batch.core.configuration.xml;

import java.util.List;

import org.springframework.batch.core.annotation.AfterWrite;

public class TestPojoListener extends AbstractTestComponent {

	@AfterWrite
	public void after(List<Object> items){
		executed = true;
	}

}
