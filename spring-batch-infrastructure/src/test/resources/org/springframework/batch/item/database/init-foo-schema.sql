DROP TABLE T_FOOS if exists;
DROP TABLE T_BARS if exists;
DROP TABLE T_WRITE_FOOS if exists;

CREATE TABLE T_FOOS (
	ID INT NOT NULL,
	NAME VARCHAR(45),
	CODE VARCHAR(10),
	VALUE INTEGER
);

create table T_BARS (
    id int not null primary key,
    name varchar(80),
    foo_date timestamp
);

CREATE TABLE T_WRITE_FOOS (
    ID INT NOT NULL,
    NAME VARCHAR(45),
    VALUE INTEGER
);

ALTER TABLE T_FOOS ADD PRIMARY KEY (ID);

INSERT INTO t_foos (id, name, value) VALUES (1, 'bar1', 1);
INSERT INTO t_foos (id, name, value) VALUES (2, 'bar2', 2);
INSERT INTO t_foos (id, name, value) VALUES (3, 'bar3', 3);
INSERT INTO t_foos (id, name, value) VALUES (4, 'bar4', 4);
INSERT INTO t_foos (id, name, value) VALUES (5, 'bar5', 5);

ALTER TABLE T_WRITE_FOOS ADD PRIMARY KEY (ID);

-- FIXME: syntax error with the following even though it is taken from the official docs of HSQLDB 2.7.1
-- http://hsqldb.org/doc/guide/sqlroutines-chapt.html#src_returning_data

-- CREATE PROCEDURE read_foos()
-- READS SQL DATA DYNAMIC RESULT SETS 1
-- BEGIN ATOMIC
--     DECLARE result CURSOR WITH RETURN FOR SELECT * FROM T_FOOS FOR READ ONLY;
--     OPEN result;
-- END;
--
-- CREATE PROCEDURE read_some_foos(IN from_id INTEGER, IN to_id INTEGER)
-- READS SQL DATA DYNAMIC RESULT SETS 1
-- BEGIN ATOMIC
--     DECLARE result CURSOR WITH RETURN FOR SELECT * FROM T_FOOS WHERE ID >= from_id and ID <= to_id FOR READ ONLY;
--     OPEN result;
-- END;
