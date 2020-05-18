package com.htffund.auditlog.interceptor.handler;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;

public class AuditLogTableCreator
{
//应该用不到了
    private final static String sqlTemplate = "CREATE TABLE %s (" +
            "ID bigint(20) NOT NULL AUTO_INCREMENT," +
            "TableName varchar(50) DEFAULT NULL," +
            "ColumnName varchar(50) DEFAULT NULL," +
            "PrimaryKey bigint(20) DEFAULT NULL," +
            "ParentID bigint(20) DEFAULT NULL," +
            "NewValue VARCHAR(520) DEFAULT NULL," +
            "OldValue VARCHAR(520) DEFAULT NULL," +
            "Operation varchar(50) DEFAULT NULL," +
            "CreateTime datetime DEFAULT NULL," +
            "CreateClerk int(11) DEFAULT NULL," +
            "PRIMARY KEY (ID)) ENGINE=InnoDB;";
    
    private final static String oracleSqlTemplate = "CREATE TABLE %s (" +
            "ID number NOT NULL AUTO_INCREMENT," +
            "TableName varchar(50) DEFAULT NULL," +
            "ColumnName varchar(50) DEFAULT NULL," +
            "PrimaryKey number DEFAULT NULL," +
            "ParentID number DEFAULT NULL," +
            "NewValue VARCHAR(520) DEFAULT NULL," +
            "OldValue VARCHAR(520) DEFAULT NULL," +
            "Operation varchar(50) DEFAULT NULL," +
            "CreateTime datetime DEFAULT NULL," +
            "CreateClerk int(11) DEFAULT NULL," +
            "PRIMARY KEY (ID)) ENGINE=InnoDB;";

    private String defaultTableName;

    private String preTableName;

    private String currentValidTableName;

    private Boolean splitEnable;

    public AuditLogTableCreator(Boolean splitEnable, String defaultTableName, String preTableName)
    {

        this.splitEnable = splitEnable;
        this.defaultTableName = defaultTableName;
        this.preTableName = preTableName;

        currentValidTableName = getCurrentTableName();
    }

    String getCurrentTableName()
    {
        if (splitEnable)
        {
            Calendar calendar = Calendar.getInstance();
            String calendarMonth;
            if (calendar.get(Calendar.MONTH) < 9)
            {
                calendarMonth = "0" + (calendar.get(Calendar.MONTH) + 1);
            } else
            {
                calendarMonth = String.valueOf((calendar.get(Calendar.MONTH) + 1));
            }
            return preTableName + calendar.get(Calendar.YEAR) + calendarMonth;
        } else
        {
            return defaultTableName;
        }

    }

    String createNew(Connection connection)
    {
        try
        {
            String tableNameByNowDate = getCurrentTableName();
            Statement statement = connection.createStatement();
            statement.execute(String.format(sqlTemplate, tableNameByNowDate));
            statement.close();
            currentValidTableName = tableNameByNowDate;
            return tableNameByNowDate;
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    String getCurrentValidTableName()
    {
        return currentValidTableName;
    }

    Boolean getSplitEnable()
    {
        return splitEnable;
    }
}
