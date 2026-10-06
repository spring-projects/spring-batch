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
