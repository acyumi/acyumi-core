package com.acyumi.util;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * (MySQL)数据库字段(列)注释工具类.
 * <br>
 * 如果需要使用此工具类，请在SpringBoot启动类或其他配置类上使用@Import({ColumnCommentUtils.class})
 * <br><br>
 * 另外如果不想让IOC容器初始化某个Bean，可以在启动类上加注解进行过滤 <br>
 * 请查看{@link org.springframework.context.annotation.ComponentScan#excludeFilters()} <br>
 * 和{@link org.springframework.context.annotation.FilterType#ASSIGNABLE_TYPE} <br>
 *
 * @author Mr.XiHui
 * @date 2019/1/3
 * @see org.springframework.context.annotation.Import
 */
public class ColumnCommentUtils {

    /**
     * 列名注释Map
     * key: table.column
     * value: comment
     */
    private static final Map<String, String> COLUMN_COMMENT_MAP = new HashMap<>();
    private static JdbcTemplate jdbcTemplate;

    /**
     * 注解@Import的使用会使Spring框架自动调用此唯一的构造方法
     *
     * @param jdbcTemplate 自动注入的JdbcTemplate
     * @see org.springframework.context.annotation.Import
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
        tableName = tableName.toLowerCase();
        String key = tableName + "." + columnName.replaceAll("_", "").toUpperCase();
        return COLUMN_COMMENT_MAP.getOrDefault(key, columnName);
    }

    /**
     * 刷新列名注释缓存.
     */
    public static void refreshColumnComments() {

        if (jdbcTemplate == null) {
            throw new RuntimeException("ColumnCommentUtils工具类未初始化，请参考工具类注释初始化之后再使用");
        }

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
            columnName = columnName.replaceAll("_", "").toUpperCase();
            COLUMN_COMMENT_MAP.put(tableName + "." + columnName, columnComment);

        }, currentDatabase);

    }

}
