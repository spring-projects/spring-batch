DROP TABLE TRADE;
DROP SEQUENCE TRADE_SEQ;
DROP TABLE CUSTOMER;
DROP SEQUENCE CUSTOMER_SEQ;
DROP TABLE PLAYERS;
DROP TABLE GAMES;
DROP TABLE PLAYER_SUMMARY;
 
CREATE TABLE TRADE  (
			ID BIGINT PRIMARY KEY NOT NULL,  
            VERSION BIGINT,
            ISIN VARCHAR(45) NOT NULL, 
            QUANTITY BIGINT,
            PRICE FLOAT, 
            CUSTOMER VARCHAR(45)
);
 
 
CREATE SEQUENCE TRADE_SEQ;
 
CREATE TABLE CUSTOMER (
            ID BIGINT PRIMARY KEY NOT NULL,
    		VERSION BIGINT,
            NAME VARCHAR(45),
            CREDIT FLOAT
);
 
CREATE SEQUENCE CUSTOMER_SEQ;
 
INSERT INTO customer (id, version, name, credit) VALUES (1, 0, 'customer1', 100000);
INSERT INTO customer (id, version, name, credit) VALUES (2, 0, 'customer2', 100000);
INSERT INTO customer (id, version, name, credit) VALUES (3, 0, 'customer3', 100000);
INSERT INTO customer (id, version, name, credit) VALUES (4, 0, 'customer4', 100000);


CREATE TABLE PLAYERS (
	PLAYER_ID char(8) not null primary key,
	LAST_NAME varchar(35) not null,
	FIRST_NAME varchar(25) not null,
	POSITION varchar(10),
	YEAR_OF_BIRTH integer not null,
	YEAR_DRAFTED integer not null);

CREATE TABLE GAMES (
   PLAYER_ID char(8) not null,
   YEAR      integer not null,
   TEAM      char(3) not null,
   WEEK      integer not null,
   OPPONENT  char(3),
   COMPLETES integer,
   ATTEMPTS  integer,
   PASSING_YARDS integer,
   PASSING_TD    integer,
   INTERCEPTIONS integer,
   RUSHES integer,
   RUSH_YARDS integer,
   RECEPTIONS integer,
   RECEPTIONS_YARDS integer,
   TOTAL_TD integer
);

CREATE TABLE PLAYER_SUMMARY  (
		  ID CHAR(8) NOT NULL , 
		  YEAR INTEGER NOT NULL,
		  COMPLETES INTEGER NOT NULL , 
		  ATTEMPTS INTEGER NOT NULL , 
		  PASSING_YARDS INTEGER NOT NULL , 
		  PASSING_TD INTEGER NOT NULL , 
		  INTERCEPTIONS INTEGER NOT NULL , 
		  RUSHES INTEGER NOT NULL , 
		  RUSH_YARDS INTEGER NOT NULL , 
		  RECEPTIONS INTEGER NOT NULL , 
		  RECEPTIONS_YARDS INTEGER NOT NULL , 
		  TOTAL_TD INTEGER NOT NULL );   