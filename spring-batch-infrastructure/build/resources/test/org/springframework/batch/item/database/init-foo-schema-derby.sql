CREATE TABLE T_FOOS (
	ID BIGINT NOT NULL,
	NAME VARCHAR(45),
	VALUE INTEGER
);

ALTER TABLE T_FOOS ADD PRIMARY KEY (ID);

INSERT INTO t_foos (id, name, value) VALUES (1, 'bar1', 1);
INSERT INTO t_foos (id, name, value) VALUES (2, 'bar2', 2);
INSERT INTO t_foos (id, name, value) VALUES (3, 'bar3', 3);
INSERT INTO t_foos (id, name, value) VALUES (4, 'bar4', 4);
INSERT INTO t_foos (id, name, value) VALUES (5, 'bar5', 5);

CREATE PROCEDURE read_foos () 
    PARAMETER STYLE JAVA 
    LANGUAGE JAVA
    READS SQL DATA
    DYNAMIC RESULT SETS 1
    EXTERNAL NAME 'test.jdbc.proc.derby.TestProcedures.readFoos';

CREATE PROCEDURE read_some_foos (from_id INTEGER, to_id INTEGER) 
    PARAMETER STYLE JAVA 
    LANGUAGE JAVA
    READS SQL DATA
    DYNAMIC RESULT SETS 1
    EXTERNAL NAME 'test.jdbc.proc.derby.TestProcedures.readSomeFoos';
