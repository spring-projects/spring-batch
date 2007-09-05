package org.springframework.batch.sample.mapping;

import org.springframework.batch.io.file.FieldSet;
import org.springframework.batch.io.file.FieldSetMapper;
import org.springframework.batch.sample.domain.NflPlayer;

public class NflPlayerMapper implements FieldSetMapper {

	public Object mapLine(FieldSet fs) {
		
		if(fs == null){
			return null;
		}
		
		NflPlayer nflPlayer = new NflPlayer();
		nflPlayer.setID(fs.readString("ID"));
		nflPlayer.setLastName(fs.readString("lastName"));
		nflPlayer.setFirstName(fs.readString("firstName"));
		nflPlayer.setPosition(fs.readString("position"));
		nflPlayer.setDebutYear(fs.readInt("debutYear"));
		nflPlayer.setBirthYear(fs.readInt("birthYear"));
		
		return nflPlayer;
	}
	

}
