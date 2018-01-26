ALTER TABLE task_records ADD COLUMN SCHEDULE_ID BIGINT;


CREATE TABLE TASK_PARAM (
  ID BIGINT,
  NAME VARCHAR(255),
  TYPE VARCHAR(255),
  VALUE VARCHAR(255),
  DESCRIPTION VARCHAR(255),
  SCHEDULE_ID BIGINT,
  PRIMARY KEY (ID));

ALTER TABLE TASK_PARAM ADD CONSTRAINT FK_task_param_schedules FOREIGN KEY (SCHEDULE_ID) REFERENCES SCHEDULES (ID);


INSERT INTO SEQUENCE(SEQ_NAME, SEQ_COUNT) values ('TPARAM_ID',1);