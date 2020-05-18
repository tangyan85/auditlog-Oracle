package com.htffund.auditlog.interceptor.handler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.htffund.auditlog.domain.AuditLog;

abstract class AbstractSQLAuditHandler extends AbstractSQLHandler
{

    private final String auditLogInsertSQL = "insert into %s " +
            "(ID,TableName, ColumnName, PrimaryKey, ParentID, NewValue, OldValue, Operation, CreateTime, CreateClerk) " +
            "values(?,?, ?, ?, ?, ?, ?, ?, sysdate, ?)";
    
    private final String genSeqID="select SEQ_audit_log.Nextval  from dual ";

    private final Object noClerkId = -1L;
    private DBMetaDataHolder dbMetaDataHolder;
    private Method clerkIdMethod;
    private String[] excludeTables;
    private Boolean isSkipTable=Boolean.FALSE;


	AbstractSQLAuditHandler(Connection connection, DBMetaDataHolder dbMetaDataHolder, Method clerkIdMethod, String sql,String[] excludeTables)
    {
        super(connection, sql);
        this.dbMetaDataHolder = dbMetaDataHolder;
        this.clerkIdMethod = clerkIdMethod;
        this.excludeTables=excludeTables;
        judgeIsSkip();
    }
	
    @Override
	public boolean IsSkipTable()
    {
        return isSkipTable;
    }
	
    @Override
    protected SQLStatement parseSQLStatement(SQLStatementParser statementParser)
    {
        return statementParser.parseInsert();
    }

    void saveAuditLog(List<List<AuditLog>> auditLogList)
    {
        try
        {
            boolean autoCommit = getConnection().getAutoCommit();
            if (autoCommit)
            {
                getConnection().setAutoCommit(false);
            }
            for (List<AuditLog> auditLogs : auditLogList)
            {
                if (auditLogs.size() > 0)
                {
                    Object parentID = saveAuditLog(auditLogs.get(0), null);
                    auditLogs.remove(0);
                    for (AuditLog auditLog : auditLogs)
                    {
                        saveAuditLog(auditLog, parentID);
                    }
                }
            }
            if (autoCommit)
            {
                getConnection().commit();
                getConnection().setAutoCommit(true);
            }
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    void saveAuditLog(Map<String, List<List<AuditLog>>> auditLogListMap)
    {
        List<List<AuditLog>> auditLogList = new ArrayList<>();
        for (List<List<AuditLog>> lists : auditLogListMap.values())
        {
            auditLogList.addAll(lists);
        }
        saveAuditLog(auditLogList);
    }

    private Object saveAuditLog(AuditLog auditLog, Object parentID)
    {
        Object resultId = null;
        PreparedStatement preparedStatement = null;
        try
        {
        	resultId=genAuditLogSeqID();
            preparedStatement = getConnection().prepareStatement(String.format(auditLogInsertSQL,
                    dbMetaDataHolder.getAuditLogTableCreator().getCurrentValidTableName()));

            int i = 1;
            preparedStatement.setObject(i++, resultId);
            preparedStatement.setObject(i++, auditLog.getTableName());
            preparedStatement.setObject(i++, auditLog.getColumnName());
            preparedStatement.setObject(i++, auditLog.getPrimaryKey());
            preparedStatement.setObject(i++, parentID);
            preparedStatement.setObject(i++, auditLog.getNewValue());
            preparedStatement.setObject(i++, auditLog.getOldValue());
            preparedStatement.setObject(i++, auditLog.getOperation());
            //preparedStatement.setObject(i++, insertTime);
            preparedStatement.setObject(i, noClerkId);
            try
            {
                if (clerkIdMethod != null)
                    preparedStatement.setObject(i, clerkIdMethod.invoke(null, null));
            } catch (RuntimeException | IllegalAccessException | InvocationTargetException e)
            {
                // do nothing.
            }
            int affectRows = preparedStatement.executeUpdate();
            if (affectRows > 0 && parentID == null)
            {}
        } catch (SQLException e)
        {
            e.printStackTrace();
        } finally
        {
            if (preparedStatement != null)
            {
                try
                {
                    preparedStatement.close();
                } catch (SQLException e)
                {
                    e.printStackTrace();
                }
            }
        }
        return resultId;
    }
    
    private Object genAuditLogSeqID(){
    	Statement statement = null;
    	Object resultId = null;
        try {
			statement = getConnection().createStatement();
	        ResultSet resultSet = statement.executeQuery(genSeqID);
	        if (resultSet.next())
	        {
	        	resultId = resultSet.getObject(1);
	        }
	        resultSet.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}finally
        {
            if (statement != null)
            {
                try
                {
                    statement.close();
                } catch (SQLException e)
                {
                    e.printStackTrace();
                }
            }
        }
       
        return resultId;
    }
    
    private void judgeIsSkip(){
        for(String excludeTable:excludeTables){
        	if(excludeTable.equalsIgnoreCase(getCurrentDataTable())){
        		isSkipTable=Boolean.TRUE;
        		break;
        	}
        }
    }

    DBMetaDataHolder getDbMetaDataHolder()
    {
        return dbMetaDataHolder;
    }

    public Method getClerkIdMethod()
    {
        return clerkIdMethod;
    }

    public Boolean getIsSkipTable() {
		return isSkipTable;
	}

}