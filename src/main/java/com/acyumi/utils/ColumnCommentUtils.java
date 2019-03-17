package com.acyumi.utils;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 数据库字段(列)注释工具类
 *
 * @author Mr.XiHui
 * @date 2019/1/3
 */
@Component
public class ColumnCommentUtils {

    /**
     * 列名注释Map
     * key: table.column
     * value: comment
     */
    private static final Map<String, String> COLUMN_COMMENT_MAP = new HashMap<>();
    private static JdbcTemplate jdbcTemplate;

    /**
     * 注解@Component的使用会使Spring框架自动调用此唯一的构造方法
     *
     * @param jdbcTemplate 自动注入的JdbcTemplate
     */
    private ColumnCommentUtils(JdbcTemplate jdbcTemplate) {
        ColumnCommentUtils.jdbcTemplate = jdbcTemplate;
        refreshColumnComments();
    }

    /**
     * 获取数据库中对应表字段(列)的注释
     *
     * @param tableName  表名
     * @param columnName 字段(列)名
     * @return columnComment 列的注释，如果查不到则返回字段(列)名
     */
    public static String getColumnComment(String tableName, String columnName) {
        return COLUMN_COMMENT_MAP.getOrDefault(tableName + "." + columnName, columnName);
    }

    /**
     * 刷新列名注释缓存
     */
    public static void refreshColumnComments() {

        String sqlShowDatabases = "show databases";
        String dbInformationSchema = "information_schema";
        Set<String> dbNames = jdbcTemplate.query(sqlShowDatabases, rowSet -> {
            Set<String> hashSet = new HashSet<>();
            while (rowSet.next()) {
                hashSet.add(rowSet.getString(1));
            }
            return hashSet;
        });

        //检查数据库中是否有information_schema这个库，如果没有那说明mysql没有完整初始化，是有问题的
        if (dbNames == null || !dbNames.contains(dbInformationSchema)) {
            throw new RuntimeException("MySQL数据库中的信息表不完整");
        }

        //查出当前连接的数据库名
        String currentDatabase = jdbcTemplate.queryForObject("select database()", String.class);

        String sql = "select table_name, column_name, column_comment " +
                " from information_schema.columns where table_schema = ?";

        //activiti框架的表名前缀
        String activitiTableNamePrefix = "act_";

        jdbcTemplate.query(sql, rowSet -> {

            String tableName = rowSet.getString(1);
            tableName = tableName.toLowerCase();
            if (tableName.startsWith(activitiTableNamePrefix)) {
                return;
            }

            String columnComment = rowSet.getString(3);
            if (ParameterUtils.isEmpty(columnComment)) {
                return;
            }

            String columnName = rowSet.getString(2);
            //columnName = columnName.toLowerCase();
            //COLUMN_COMMENT_MAP.put(tableName + "." + columnName, columnComment);
            columnName = ParameterUtils.snakeCaseToCamelCase(columnName);
            COLUMN_COMMENT_MAP.put(tableName + "." + columnName, columnComment);

        }, currentDatabase);

    }

}
