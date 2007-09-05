package org.springframework.batch.sample.item.provider;

import org.springframework.batch.io.Skippable;
import org.springframework.batch.io.file.FieldSetInputSource;
import org.springframework.batch.io.file.FieldSetMapper;
import org.springframework.batch.io.file.support.DefaultFlatFileInputSource;
import org.springframework.batch.item.ItemProvider;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.batch.sample.domain.NflPlayer;



public class NflPlayerItemProvider implements ItemProvider, Restartable, Skippable{

	DefaultFlatFileInputSource inputSource = null;
	
	FieldSetMapper fieldSetMapper = null;
	
	public void setFieldSetMapper(FieldSetMapper fieldSetMapper) {
		this.fieldSetMapper = fieldSetMapper;
	}
	
	public void setInputSource(DefaultFlatFileInputSource inputSource) {
		this.inputSource = inputSource;
	}	
	
	public Object getKey(Object item) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object next() throws Exception {
		
		NflPlayer nflPlayer = (NflPlayer)fieldSetMapper.mapLine(inputSource.readFieldSet());
		return nflPlayer;
	}

	public boolean recover(Object data, Throwable cause) {
		// TODO Auto-generated method stub
		return false;
	}

	public RestartData getRestartData() {
		return inputSource.getRestartData();
	}

	public void restoreFrom(RestartData data) {
		inputSource.restoreFrom(data);
	}

	public void skip() {
		inputSource.skip();
	}

}
