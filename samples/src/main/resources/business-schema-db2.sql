DROP TABLE TRADE;
DROP SEQUENCE TRADE_SEQ;
DROP TABLE CUSTOMER;
DROP SEQUENCE CUSTOMER_SEQ;
 
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