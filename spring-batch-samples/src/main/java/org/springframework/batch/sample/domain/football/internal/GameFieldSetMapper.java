/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.sample.domain.football.internal;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.sample.domain.football.Game;

public class GameFieldSetMapper implements FieldSetMapper<Game> {

	public Game mapFieldSet(FieldSet fs) {
		
		if(fs == null){
			return null;
		}
		
		Game game = new Game();
		game.setId(fs.readString("id"));
		game.setYear(fs.readInt("year"));
		game.setTeam(fs.readString("team"));
		game.setWeek(fs.readInt("week"));
		game.setOpponent(fs.readString("opponent"));
		game.setCompletes(fs.readInt("completes"));
		game.setAttempts(fs.readInt("attempts"));
		game.setPassingYards(fs.readInt("passingYards"));
		game.setPassingTd(fs.readInt("passingTd"));
		game.setInterceptions(fs.readInt("interceptions"));
		game.setRushes(fs.readInt("rushes"));
		game.setRushYards(fs.readInt("rushYards"));
		game.setReceptions(fs.readInt("receptions", 0));
		game.setReceptionYards(fs.readInt("receptionYards"));
		game.setTotalTd(fs.readInt("totalTd"));
		
		return game;
	}

}
