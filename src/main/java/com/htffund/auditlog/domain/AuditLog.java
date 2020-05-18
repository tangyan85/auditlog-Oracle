package com.htffund.auditlog.domain;

public class AuditLog
{

    public static enum OperationEnum
    {
        insert, update, delete
    }

    private String tableName;
    private String columnName;
    private Long parentId;
    private Object primaryKey;
    private String operation;
    private Object oldValue;
    private Object newValue;

    public AuditLog(String tableName, String columnName, Long parentId, Object primaryKey, String operation, Object oldValue, Object newValue)
    {
        this.tableName = tableName;
        this.columnName = columnName;
        this.parentId = parentId;
        this.primaryKey = primaryKey;
        this.operation = operation;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getTableName()
    {
        return tableName;
    }

    public void setTableName(String tableName)
    {
        this.tableName = tableName;
    }

    public String getColumnName()
    {
        return columnName;
    }

    public void setColumnName(String columnName)
    {
        this.columnName = columnName;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public Object getPrimaryKey()
    {
        return primaryKey;
    }

    public void setPrimaryKey(Object primaryKey)
    {
        this.primaryKey = primaryKey;
    }

    public String getOperation()
    {
        return operation;
    }

    public void setOperation(String operation)
    {
        this.operation = operation;
    }

    public Object getOldValue()
    {
        return oldValue;
    }

    public void setOldValue(Object oldValue)
    {
        this.oldValue = oldValue;
    }

    public Object getNewValue()
    {
        return newValue;
    }

    public void setNewValue(Object newValue)
    {
        this.newValue = newValue;
    }
}
