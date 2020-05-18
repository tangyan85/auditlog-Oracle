package com.htffund.auditlog.interceptor.handler;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.commons.lang.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DBMetaDataHolder
{
    @SuppressWarnings("unchecked")
    private final Map<String, String> primaryKeys = new CaseInsensitiveMap();

    @SuppressWarnings("unchecked")
    private final Map<String, List<String>> tableColumns = new CaseInsensitiveMap();

    private Boolean initialized = Boolean.FALSE;
    private AuditLogTableCreator auditLogTableCreator;

    public DBMetaDataHolder(AuditLogTableCreator auditLogTableCreator)
    {
        this.auditLogTableCreator = auditLogTableCreator;
    }

    public void init(Connection connection)
    {
        if (initialized == Boolean.FALSE || hasNoCurrentAuditLogTable())
        {
            synchronized (this)
            {
                if (connection != null)
                {
                    if (initialized == Boolean.FALSE)
                    {
                        try
                        {
                            ResultSet resultSet = connection.getMetaData().getTables(null,
                                    connection.getMetaData().getUserName(), "%", new String[]{"TABLE"});
                            while (resultSet.next())
                            {
                                String tableName = resultSet.getString("TABLE_NAME");
                                buildOneSingleTableMetaData(connection, tableName);
                            }
                            resultSet.close();
                            initialized = Boolean.TRUE;
                        } catch (SQLException e)
                        {
                            e.printStackTrace();
                        }
                    }

                    if (hasNoCurrentAuditLogTable())
                    {
                        String newTableName = auditLogTableCreator.createNew(connection);
                        if (StringUtils.isNotBlank(newTableName))
                        {
                            buildOneSingleTableMetaData(connection, newTableName);
                        }
                    }
                }
            }
        }
    }

    private Boolean hasNoCurrentAuditLogTable()
    {
        String currentTableName = auditLogTableCreator.getCurrentTableName();
        return !primaryKeys.containsKey(currentTableName) || !tableColumns.containsKey(currentTableName);
    }

    private void buildOneSingleTableMetaData(Connection connection, String tableName)
    {
        primaryKeys.put(tableName, retrievePrimaryKey(connection, tableName));
        tableColumns.put(tableName, retrieveColumns(connection, tableName));
    }

    private String retrievePrimaryKey(Connection connection, String table)
    {
        String primaryKey = null;
        try
        {
            ResultSet resultSet = connection.getMetaData().getPrimaryKeys(null, connection.getMetaData().getUserName(), table);
            if (resultSet.next())
                primaryKey = resultSet.getString("COLUMN_NAME");
            resultSet.close();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
        return primaryKey;
    }

    private List<String> retrieveColumns(Connection connection, String table)
    {
        List<String> columns = null;
        if (!tableColumns.containsKey(table))
        {
            try
            {
                columns = new ArrayList<>();
                ResultSet resultSet = connection.getMetaData().getColumns(null, connection.getMetaData().getUserName(), table, "%");
                while (resultSet.next())
                {
                    String columnName = resultSet.getString("COLUMN_NAME");
                    columns.add(columnName);
                }
                tableColumns.put(table, columns);
                resultSet.close();
            } catch (SQLException e)
            {
                e.printStackTrace();
            }
        } else
        {
            columns = tableColumns.get(table);
        }
        return columns;
    }

    Map<String, String> getPrimaryKeys()
    {
        return primaryKeys;
    }

    Map<String, List<String>> getTableColumns()
    {
        return tableColumns;
    }

    AuditLogTableCreator getAuditLogTableCreator()
    {
        return auditLogTableCreator;
    }
}
