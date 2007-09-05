package org.springframework.batch.sample.mapping;

import org.springframework.batch.io.file.FieldSet;
import org.springframework.batch.io.file.FieldSetMapper;
import org.springframework.batch.sample.domain.NflGame;

public class NflGameMapper implements FieldSetMapper {

	public Object mapLine(FieldSet fs) {
		
		if(fs == null){
			return null;
		}
		
		NflGame nflGame = new NflGame();
		nflGame.setId(fs.readString("id"));
		nflGame.setYear(fs.readInt("year"));
		nflGame.setTeam(fs.readString("team"));
		nflGame.setWeek(fs.readInt("week"));
		nflGame.setOpponent(fs.readString("opponent"));
		nflGame.setCompletes(fs.readInt("completes"));
		nflGame.setAttempts(fs.readInt("attempts"));
		nflGame.setPassingYards(fs.readInt("passingYards"));
		nflGame.setPassingTd(fs.readInt("passingTd"));
		nflGame.setInterceptions(fs.readInt("interceptions"));
		nflGame.setRushes(fs.readInt("rushes"));
		nflGame.setRushYards(fs.readInt("rushYards"));
		nflGame.setReceptions(fs.readInt("receptions", 0));
		nflGame.setReceptionYards(fs.readInt("receptionYards"));
		nflGame.setTotalTd(fs.readInt("totalTd"));
		
		return nflGame;
	}

}
