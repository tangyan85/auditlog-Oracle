package com.htffund.auditlog.interceptor.handler;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleDeleteStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleSelectQueryBlock;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.htffund.auditlog.domain.AuditLog;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OracleDeleteSqlAuditHandler extends AbstractSQLAuditHandler
{

    private String querySql;

    private Map<String, List<List<AuditLog>>> auditLogsBeforeDelete;

    private Boolean preHandled = Boolean.FALSE;

    public OracleDeleteSqlAuditHandler(Connection connection, DBMetaDataHolder dbMetaDataHolder, Method clerkIdMethod, String sql,String[] excludeTables)
    {
        super(connection, dbMetaDataHolder, clerkIdMethod, sql,excludeTables);
    }

    @Override
    protected SQLStatement parseSQLStatement(SQLStatementParser statementParser)
    {
        return statementParser.parseDeleteStatement();
    }

    @Override
    protected SQLTableSource getMajorTableSource(SQLStatement statement)
    {
        if (statement instanceof OracleDeleteStatement)
        {
            return ((OracleDeleteStatement) statement).getFrom() != null ? ((OracleDeleteStatement) statement).getFrom() : ((OracleDeleteStatement) statement).getTableSource();
        } else
        {
            return null;
        }
    }

    private List<String> buildTableSourceAliases(SQLTableSource tableSource)
    {
        List<String> tables = new ArrayList<>();
        if (tableSource instanceof SQLExprTableSource)
        {
            SQLExpr expr = ((SQLExprTableSource) tableSource).getExpr();
            if (expr instanceof SQLPropertyExpr)
            {
                tables.add(SQLUtils.toOracleString(((SQLPropertyExpr) expr).getOwner()));
            } else if (expr instanceof SQLIdentifierExpr)
            {
                tables.add(getTableToAliasMap().get(((SQLIdentifierExpr) expr).getName()));
            }
        } else if (tableSource instanceof SQLJoinTableSource)
        {
            tables.addAll(buildTableSourceAliases(((SQLJoinTableSource) tableSource).getLeft()));
            tables.addAll(buildTableSourceAliases(((SQLJoinTableSource) tableSource).getRight()));
        }
        return tables;
    }


    @Override
    public void preHandle()
    {
        if (getSqlStatement() instanceof OracleDeleteStatement)
        {
        	OracleDeleteStatement deleteStatement = (OracleDeleteStatement) getSqlStatement();
            SQLTableSource affectTableSource = deleteStatement.getTableSource() != null ? deleteStatement.getTableSource() : deleteStatement.getFrom();
            List<String> affectAliasList = buildTableSourceAliases(affectTableSource);
            SQLTableSource from = deleteStatement.getFrom() != null ? deleteStatement.getFrom() : deleteStatement.getTableSource();
            SQLExpr where = deleteStatement.getWhere();
            //SQLOrderBy orderBy = deleteStatement.getOrderBy();
            //SQLLimit limit = deleteStatement.getLimit();
            OracleSelectQueryBlock selectQueryBlock = new OracleSelectQueryBlock();
            for (String alias : affectAliasList)
            {
                selectQueryBlock.getSelectList().add(new SQLSelectItem(SQLUtils.toSQLExpr(
                        String.format("%s.%s", alias, getDbMetaDataHolder().getPrimaryKeys().get(getAliasToTableMap().get(alias))))));
                for (String columnName : getDbMetaDataHolder().getTableColumns().get(getAliasToTableMap().get(alias)))
                {
                    selectQueryBlock.getSelectList().add(new SQLSelectItem(SQLUtils.toSQLExpr(
                            String.format("%s.%s", alias, columnName))));
                }
            }
            selectQueryBlock.setFrom(from);
            selectQueryBlock.setWhere(where);
            //selectQueryBlock.setOrderBy(orderBy);
            //selectQueryBlock.setLimit(limit);
            querySql = trimSQLWhitespaces(SQLUtils.toOracleString(selectQueryBlock));
            auditLogsBeforeDelete = getCurrentDataForTables();
            preHandled = Boolean.TRUE;
        }
    }

    @Override
    public void postHandle(Object args)
    {
        if (preHandled)
        {
            saveAuditLog(auditLogsBeforeDelete);
        }
    }


    @SuppressWarnings("unchecked")
    private Map<String, List<List<AuditLog>>> getCurrentDataForTables()
    {
        Map<String, List<List<AuditLog>>> resultListMap = new CaseInsensitiveMap();
        PreparedStatement statement = null;
        try
        {
            statement = getConnection().prepareStatement(querySql);
            ResultSet resultSet = statement.executeQuery();
            int columnCount = resultSet.getMetaData().getColumnCount();
            int row = 0;
            while (resultSet.next())
            {
                Map<String, Object> primaryKeyMap = new CaseInsensitiveMap();
                for (int i = 1; i < columnCount + 1; i++)
                {
                    //String tableName = resultSet.getMetaData().getTableName(i);
                	String currentTableName=resultSet.getMetaData().getTableName(i);
                    String tableName = (StringUtils.isBlank(currentTableName))?getTables().get(0):currentTableName;
                    if (StringUtils.isNotBlank(tableName))
                    {
                        List<List<AuditLog>> list = resultListMap.get(tableName);
                        if (list == null)
                            list = new ArrayList<>();
                        if (list.size() <= row)
                        {
                            list.add(new ArrayList());
                            primaryKeyMap.put(tableName, resultSet.getObject(i));
                        } else
                        {
                        	if(null==resultSet.getObject(i)){
                   			    continue;
                   		    }
                            List<AuditLog> cols = list.get(row);
                            cols.add(new AuditLog(tableName, getDbMetaDataHolder().getTableColumns().get(tableName).get(i - 2), null,
                                    primaryKeyMap.get(tableName), AuditLog.OperationEnum.delete.name(), resultSet.getObject(i), null));
                        }
                        resultListMap.put(tableName, list);
                    }
                }
                row++;
            }
            resultSet.close();
        } catch (SQLException e)
        {
            e.printStackTrace();
        } finally
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
        return resultListMap;
    }

}
