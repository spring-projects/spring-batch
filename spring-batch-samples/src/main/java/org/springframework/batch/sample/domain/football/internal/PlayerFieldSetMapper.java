package org.springframework.batch.sample.domain.football.internal;

import org.springframework.batch.item.file.mapping.FieldSet;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.sample.domain.football.Player;

public class PlayerFieldSetMapper implements FieldSetMapper<Player> {

	public Player mapLine(FieldSet fs, int lineNum) {
		
		if(fs == null){
			return null;
		}
		
		Player player = new Player();
		player.setID(fs.readString("ID"));
		player.setLastName(fs.readString("lastName"));
		player.setFirstName(fs.readString("firstName"));
		player.setPosition(fs.readString("position"));
		player.setDebutYear(fs.readInt("debutYear"));
		player.setBirthYear(fs.readInt("birthYear"));
		
		return player;
	}
	

}
