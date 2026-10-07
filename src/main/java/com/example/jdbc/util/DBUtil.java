package com.example.jdbc.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 数据库连接工具类：统一负责驱动的加载与连接的获取 / 释放。
 *
 * <p>默认连接本机 MySQL 8.0 的 {@code DateTest} 库（root / 123456），
 * 也可以在启动时用系统属性覆盖，例如：</p>
 * <pre>
 * -Djdbc.url=jdbc:mysql://127.0.0.1:3306/DateTest -Djdbc.user=root -Djdbc.password=123456
 * </pre>
 */
public final class DBUtil {

    /** 默认连接地址：字符集 utf8、关闭 SSL、时区 Asia/Shanghai */
    private static final String DEFAULT_URL =
            "jdbc:mysql://127.0.0.1:3306/DateTest"
                    + "?useUnicode=true&characterEncoding=utf8&useSSL=false"
                    + "&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";

    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "123456";

    static {
        try {
            // MySQL 8.x 驱动类；显式加载保证在任意运行环境下都能找到驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("未找到 MySQL 驱动，请检查 pom.xml 中的依赖：" + e.getMessage());
        }
    }

    private DBUtil() {
    }

    /** 获取数据库连接 */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getUrl(), getUser(), getPassword());
    }

    public static String getUrl() {
        return System.getProperty("jdbc.url", DEFAULT_URL);
    }

    public static String getUser() {
        return System.getProperty("jdbc.user", DEFAULT_USER);
    }

    public static String getPassword() {
        return System.getProperty("jdbc.password", DEFAULT_PASSWORD);
    }

    /** 静默关闭 ResultSet / Statement / Connection */
    public static void closeQuietly(AutoCloseable... closeables) {
        if (closeables == null) {
            return;
        }
        for (AutoCloseable c : closeables) {
            if (c instanceof ResultSet || c instanceof Statement || c instanceof Connection) {
                try {
                    c.close();
                } catch (Exception ignored) {
                    // 关闭失败不影响主流程
                }
            }
        }
    }
}
