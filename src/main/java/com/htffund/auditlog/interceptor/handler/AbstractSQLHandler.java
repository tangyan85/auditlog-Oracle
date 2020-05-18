package com.htffund.auditlog.interceptor.handler;

import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.dialect.oracle.parser.OracleStatementParser;
import com.alibaba.druid.sql.parser.SQLStatementParser;

import org.apache.commons.collections.map.CaseInsensitiveMap;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractSQLHandler implements ISQLHandler
{
    private Connection connection;
    private String sql;
    private List<String> tables;
    private Map<String, String> aliasToTableMap;
    private Map<String, String> tableToAliasMap;
    private SQLStatement sqlStatement;
    private static Pattern pattern1 = Pattern.compile("(([A-Za-z0-9_]+)(?:\\.))?`?([A-Za-z0-9_]+)`?");
    private static Pattern pattern2 = Pattern.compile("`\\.`");
    private static Pattern pattern3 = Pattern.compile("[\\s]+");
    private String currentDataTable;//delete update  insert不应该就一张表吗?
    
	protected abstract SQLTableSource getMajorTableSource(SQLStatement statement);

    protected abstract SQLStatement parseSQLStatement(SQLStatementParser statementParser);

    AbstractSQLHandler(Connection connection, String sql)
    {
        this.connection = connection;
        this.sql = sql;
        init();
    }

    private void init()
    {
        sqlStatement = parseSQLStatement(new OracleStatementParser(sql));
        SQLTableSource sqlTableSource = getMajorTableSource(sqlStatement);
        if (sqlTableSource != null)
        {
            aliasToTableMap = buildAliasToTableMap(sqlTableSource);
            tableToAliasMap = reverseKeyAndValueOfMap(aliasToTableMap);
            tables = new ArrayList<>(tableToAliasMap.keySet());
        }
		try {
			if (null!=tables && tables.size() == 1) {
				currentDataTable = tables.get(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    static String trimSQLWhitespaces(String sql)
    {
        return pattern3.matcher(sql).replaceAll(" ");
    }

    private static String normalize(String name)
    {
        if (name == null)
        {
            return null;
        }

        if (name.length() > 2)
        {
            char c0 = name.charAt(0);
            char x0 = name.charAt(name.length() - 1);
            if ((c0 == '"' && x0 == '"') || (c0 == '`' && x0 == '`'))
            {
                String normalizeName = name.substring(1, name.length() - 1);
                if (c0 == '`')
                {
                    normalizeName = pattern2.matcher(normalizeName).replaceAll(".");
                }
                return normalizeName;
            }
        }

        return name;
    }

    static String[] separateAliasAndColumn(String combinedColumn)
    {
        String alias = null;
        String column = null;
        Matcher matcher = pattern1.matcher(combinedColumn);
        if (matcher.matches())
        {
            switch (matcher.groupCount())
            {
                case 3:
                    alias = matcher.group(2);
                    column = matcher.group(3);
                    break;
                case 1:
                    column = matcher.group(1);
                    break;
                default:
                    break;
            }
        }
        return new String[]{alias, column};
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> reverseKeyAndValueOfMap(Map<String, String> map)
    {
        Set<String> keySet = map.keySet();
        Map<String, String> resultMap = new CaseInsensitiveMap();
        for (String key : keySet)
        {
            String value = map.get(key);
            resultMap.put(value, key);
        }
        return resultMap;
    }

    final String determineTableForColumn(String column)
    {
        try
        {
            for (String table : tables)
            {
                ResultSet resultSet = getConnection().getMetaData().getColumns(null,
                        getConnection().getMetaData().getUserName(), table, column);
                if (resultSet.next())
                {
                    resultSet.close();
                    return table;
                }
            }
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> buildAliasToTableMap(SQLTableSource tableSource)
    {
        Map<String, String> map = new CaseInsensitiveMap();
        if (tableSource != null && SQLJoinTableSource.class == tableSource.getClass())
        {
            map.putAll(buildAliasToTableMap(((SQLJoinTableSource) tableSource).getLeft()));
            map.putAll(buildAliasToTableMap(((SQLJoinTableSource) tableSource).getRight()));
        } else if (tableSource != null)
        	//else if (tableSource != null && SQLExprTableSource.class == tableSource.getClass())
        {
            SQLExprTableSource exprTableSource = (SQLExprTableSource) tableSource;
            String alias = exprTableSource.getAlias();
            if (alias == null)
            {
                if (exprTableSource.getExpr() instanceof SQLName)
                {
                    alias = ((SQLName) exprTableSource.getExpr()).getSimpleName();
                }
            }
            map.put(normalize(alias), ((SQLName) exprTableSource.getExpr()).getSimpleName());
        }
        return map;
    }

    Connection getConnection()
    {
        return connection;
    }

    public String getSql()
    {
        return sql;
    }

    List<String> getTables()
    {
        return tables;
    }

    Map<String, String> getAliasToTableMap()
    {
        return aliasToTableMap;
    }

    Map<String, String> getTableToAliasMap()
    {
        return tableToAliasMap;
    }

    SQLStatement getSqlStatement()
    {
        return sqlStatement;
    }
    
    public String getCurrentDataTable() {
		return currentDataTable;
	}
}