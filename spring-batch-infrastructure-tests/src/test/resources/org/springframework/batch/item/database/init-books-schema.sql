DROP TABLE T_BOOKS if exists;
DROP TABLE T_AUTHORS if exists;

CREATE TABLE T_AUTHORS (
	ID BIGINT NOT NULL PRIMARY KEY,
	NAME VARCHAR(45)
);

CREATE TABLE T_BOOKS(
	ID BIGINT NOT NULL PRIMARY KEY,
	NAME VARCHAR(45),
	AUTHOR_ID BIGINT NOT NULL
);

INSERT INTO T_AUTHORS (id, name) VALUES (1, 'author 1');
INSERT INTO T_AUTHORS (id, name) VALUES (2, 'author 2');
INSERT INTO T_AUTHORS (id, name) VALUES (3, 'author 3');

INSERT INTO T_BOOKS (id, name, author_id) VALUES (1, 'author 1 - book 1', 1);
INSERT INTO T_BOOKS (id, name, author_id) VALUES (2, 'author 1 - book 2', 1);
INSERT INTO T_BOOKS (id, name, author_id) VALUES (3, 'author 2 - book 1', 2);
INSERT INTO T_BOOKS (id, name, author_id) VALUES (4, 'author 2 - book 2', 2);
INSERT INTO T_BOOKS (id, name, author_id) VALUES (5, 'author 3 - book 1', 3);
INSERT INTO T_BOOKS (id, name, author_id) VALUES (6, 'author 3 - book 2', 3);
