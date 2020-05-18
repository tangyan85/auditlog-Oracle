-- Create table
create table AUDIT_LOG
(
  id          NUMBER not null,
  tablename   VARCHAR2(50) not null,
  columnname  VARCHAR2(50),
  primarykey  NUMBER,
  parentid    NUMBER,
  newvalue    VARCHAR2(520),
  oldvalue    VARCHAR2(520),
  operation   VARCHAR2(50),
  createtime  DATE,
  createclerk NUMBER
)
tablespace USERS
  pctfree 10
  initrans 1
  maxtrans 255
  storage
  (
    initial 64K
    next 1M
    minextents 1
    maxextents unlimited
  );
-- Create/Recreate primary, unique and foreign key constraints 
alter table AUDIT_LOG
  add constraint PK_AUDITLOG primary key (ID)
  using index 
  tablespace USERS
  pctfree 10
  initrans 2
  maxtrans 255
  storage
  (
    initial 64K
    next 1M
    minextents 1
    maxextents unlimited
  );


-- Create sequence 
create sequence SEQ_AUDIT_LOG
minvalue 1
maxvalue 9999999999999999999999999999
start with 381
increment by 1
cache 20;
