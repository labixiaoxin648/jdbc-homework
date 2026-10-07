package com.example.jdbc;

import com.example.jdbc.annotation.Column;
import com.example.jdbc.annotation.Id;
import com.example.jdbc.annotation.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通用 JDBC 工具类（ORM 雏形）。
 *
 * <p>核心思路：借助 <b>Java 反射</b> 在「实体对象」与「数据库记录」之间建立映射——
 * 用 {@link Table} 注解定位表名，用 {@link Column} 注解定位字段名，
 * 用 {@link Id} 注解定位主键，然后动态拼装 SQL、动态创建对象并回填属性。</p>
 *
 * <p>对外提供 5 个方法：</p>
 * <ol>
 *   <li>{@link #resultSetToList(ResultSet, Class)} —— 结果集转对象列表（查）</li>
 *   <li>{@link #save(Object, Connection)} —— 新增</li>
 *   <li>{@link #update(Object, Connection)} —— 修改</li>
 *   <li>{@link #delete(Object, Connection)} —— 删除</li>
 *   <li>{@link #getOneById(String, Class, Connection)} —— 按主键查单个对象</li>
 * </ol>
 */
public final class JDBCTool {

    private JDBCTool() {
    }

    // ==========================================================
    // 对外五大方法
    // ==========================================================

    /**
     * 方法 1：把 ResultSet 转换成 List&lt;T&gt;。
     *
     * <p>对结果集中的每一行，都通过反射创建 clazz 的实例，
     * 再把该行的每个字段值赋给对象上对应的属性。</p>
     *
     * @param rs    已经执行完查询的结果集（方法内部不负责关闭）
     * @param clazz 目标实体类的 Class 对象
     * @return 与结果集记录一一对应的对象列表；结果集为空时返回空集合
     */
    public static <T> List<T> resultSetToList(ResultSet rs, Class<T> clazz) {
        List<T> result = new ArrayList<>();
        if (rs == null || clazz == null) {
            return result;
        }
        Map<Field, String> fieldColumnMap = resolveFieldColumnMap(clazz);
        try {
            Set<String> labels = resultSetLabels(rs);
            while (rs.next()) {
                T instance = newInstance(clazz);
                for (Map.Entry<Field, String> entry : fieldColumnMap.entrySet()) {
                    Field field = entry.getKey();
                    String column = entry.getValue();
                    // 结果集里没有这一列（例如只 SELECT 了部分字段）时跳过
                    if (!labels.contains(column.toLowerCase())) {
                        continue;
                    }
                    Object value = readColumn(rs, column);
                    setFieldValue(instance, field, convertValue(value, field.getType()));
                }
                result.add(instance);
            }
        } catch (SQLException e) {
            throw new RuntimeException("resultSetToList 映射失败：" + e.getMessage(), e);
        }
        return result;
    }

    /**
     * 方法 2：把对象保存（INSERT）到它对应的表中。
     *
     * <p>字段值来自对象的非空属性；若主键为空则交给数据库自增。</p>
     *
     * @return 受影响的行数（新增成功为 1）
     */
    public static <T> int save(T obj, Connection connection) {
        requireObject(obj, connection);
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) obj.getClass();

        Map<Field, String> fieldColumnMap = resolveFieldColumnMap(clazz);
        Field idField = resolveIdField(clazz);

        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<Object> params = new ArrayList<>();

        for (Map.Entry<Field, String> entry : fieldColumnMap.entrySet()) {
            Object value = getFieldValue(obj, entry.getKey());
            // 自增主键为空时不参与 INSERT
            if (value == null) {
                continue;
            }
            if (columns.length() > 0) {
                columns.append(", ");
                placeholders.append(", ");
            }
            columns.append(entry.getValue());
            placeholders.append("?");
            params.add(value);
        }

        if (params.isEmpty()) {
            throw new IllegalArgumentException("没有可插入的字段：对象所有属性均为 null");
        }
        if (idField != null && getFieldValue(obj, idField) == null) {
            // 仅是提示：主键为空表示由数据库自增
            log("save() 主键为空，交由数据库自增生成");
        }

        String sql = "INSERT INTO " + resolveTableName(clazz)
                + " (" + columns + ") VALUES (" + placeholders + ")";
        return executeUpdate(connection, sql, params.toArray(), sql);
    }

    /**
     * 方法 3：根据主键更新（UPDATE）对象。
     *
     * @return 受影响的行数
     */
    public static <T> int update(T obj, Connection connection) {
        requireObject(obj, connection);
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) obj.getClass();

        Map<Field, String> fieldColumnMap = resolveFieldColumnMap(clazz);
        Field idField = resolveIdField(clazz);
        Object idValue = getFieldValue(obj, idField);
        if (idValue == null) {
            throw new IllegalArgumentException("update 失败：主键属性值为 null");
        }

        List<Object> params = new ArrayList<>();
        StringBuilder sets = new StringBuilder();
        for (Map.Entry<Field, String> entry : fieldColumnMap.entrySet()) {
            if (entry.getKey().equals(idField)) {
                continue; // 主键只出现在 WHERE 中
            }
            if (sets.length() > 0) {
                sets.append(", ");
            }
            sets.append(entry.getValue()).append(" = ?");
            params.add(getFieldValue(obj, entry.getKey()));
        }
        if (sets.length() == 0) {
            throw new IllegalArgumentException("update 失败：没有可更新的字段");
        }
        params.add(idValue);

        String sql = "UPDATE " + resolveTableName(clazz)
                + " SET " + sets
                + " WHERE " + fieldColumnMap.get(idField) + " = ?";
        return executeUpdate(connection, sql, params.toArray(), sql);
    }

    /**
     * 方法 4：根据主键删除（DELETE）对象。
     *
     * @return 受影响的行数
     */
    public static <T> int delete(T obj, Connection connection) {
        requireObject(obj, connection);
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) obj.getClass();

        Field idField = resolveIdField(clazz);
        Object idValue = getFieldValue(obj, idField);
        if (idValue == null) {
            throw new IllegalArgumentException("delete 失败：主键属性值为 null");
        }

        String sql = "DELETE FROM " + resolveTableName(clazz)
                + " WHERE " + resolveFieldColumnMap(clazz).get(idField) + " = ?";
        return executeUpdate(connection, sql, new Object[]{idValue}, sql);
    }

    /**
     * 方法 5：按主键查询单个对象。
     *
     * @param id    主键值（字符串形式，由驱动自动转换为列类型）
     * @param clazz 目标实体类
     * @return 查到的对象；查不到时返回 null
     */
    public static <T> T getOneById(String id, Class<T> clazz, Connection connection) {
        if (clazz == null) {
            throw new IllegalArgumentException("getOneById 失败：clazz 不能为 null");
        }
        if (connection == null) {
            throw new IllegalArgumentException("getOneById 失败：connection 不能为 null");
        }
        String sql = "SELECT * FROM " + resolveTableName(clazz)
                + " WHERE " + resolveFieldColumnMap(clazz).get(resolveIdField(clazz)) + " = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> list = resultSetToList(rs, clazz);
                return list.isEmpty() ? null : list.get(0);
            }
        } catch (SQLException e) {
            throw new RuntimeException("getOneById 执行失败：" + sql + " -> " + e.getMessage(), e);
        }
    }

    // ==========================================================
    // 辅助方法：反射解析（类 -> 表名 / 字段 -> 列名 / 主键）
    // ==========================================================

    /** 取表名：优先用 @Table，否则类名驼峰转下划线 */
    public static String resolveTableName(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        if (table != null && !table.value().isBlank()) {
            return table.value();
        }
        return camelToUnderscore(clazz.getSimpleName());
    }

    /** 取某个属性对应的列名：优先用 @Column，否则属性名驼峰转下划线 */
    public static String resolveColumnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.value().isBlank()) {
            return column.value();
        }
        return camelToUnderscore(field.getName());
    }

    /** 解析实体类所有可映射属性 -> 列名（保持声明顺序） */
    public static Map<Field, String> resolveFieldColumnMap(Class<?> clazz) {
        Map<Field, String> map = new LinkedHashMap<>();
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                map.put(field, resolveColumnName(field));
            }
        }
        return map;
    }

    /** 解析主键属性：优先找 @Id，找不到则退回名为 id / 类名+Id 的属性 */
    public static Field resolveIdField(Class<?> clazz) {
        Field fallback = null;
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (field.isAnnotationPresent(Id.class)) {
                    field.setAccessible(true);
                    return field;
                }
                String lower = field.getName().toLowerCase();
                if (fallback == null && ("id".equals(lower) || lower.endsWith("id"))) {
                    field.setAccessible(true);
                    fallback = field;
                }
            }
        }
        if (fallback == null) {
            throw new IllegalStateException(clazz.getName() + " 未找到主键属性（请使用 @Id 标注）");
        }
        return fallback;
    }

    /** 通过无参构造反射创建对象 */
    public static <T> T newInstance(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("反射创建对象失败，请确认 " + clazz.getName() + " 存在无参构造方法", e);
        }
    }

    /** 反射读取属性值 */
    public static Object getFieldValue(Object obj, Field field) {
        if (obj == null || field == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            return field.get(obj);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("读取属性失败：" + field.getName(), e);
        }
    }

    /** 反射写入属性值 */
    public static void setFieldValue(Object obj, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("为属性赋值失败：" + field.getName(), e);
        }
    }

    // ==========================================================
    // 辅助方法：SQL 执行与结果集处理
    // ==========================================================

    /** 统一的更新入口：预编译 SQL -> 绑定参数 -> 执行 */
    public static int executeUpdate(Connection connection, String sql, Object[] params, String logSql) {
        if (connection == null) {
            throw new IllegalArgumentException("数据库连接为 null，无法执行：" + logSql);
        }
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bindParameters(ps, params);
            int rows = ps.executeUpdate();
            log("SQL: " + logSql + " | 参数: " + java.util.Arrays.toString(params) + " | 影响行数: " + rows);
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("SQL 执行失败：" + logSql + " -> " + e.getMessage(), e);
        }
    }

    /** 参数绑定：java.time 类型转换成 JDBC 可识别的类型 */
    public static void bindParameters(PreparedStatement ps, Object[] params) throws SQLException {
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            if (value instanceof LocalDate localDate) {
                ps.setDate(i + 1, java.sql.Date.valueOf(localDate));
            } else if (value instanceof LocalDateTime localDateTime) {
                ps.setTimestamp(i + 1, Timestamp.valueOf(localDateTime));
            } else if (value instanceof Boolean b) {
                ps.setBoolean(i + 1, b);
            } else {
                ps.setObject(i + 1, value);
            }
        }
    }

    /** 读取一列的值 */
    public static Object readColumn(ResultSet rs, String columnLabel) throws SQLException {
        return rs.getObject(columnLabel);
    }

    /** 取出结果集中所有列名（小写，便于忽略大小写匹配） */
    public static Set<String> resultSetLabels(ResultSet rs) throws SQLException {
        Set<String> labels = new LinkedHashSet<>();
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            labels.add(metaData.getColumnLabel(i).toLowerCase());
        }
        return labels;
    }

    /**
     * 类型转换：把 JDBC 取出的值转换成实体属性声明的类型。
     * 支持 String / 八种基本类型及包装类 / BigDecimal / Boolean /
     * LocalDate / LocalDateTime / java.util.Date。
     */
    @SuppressWarnings("unchecked")
    public static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return defaultValueOf(targetType);
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        if (targetType == String.class) {
            return value.toString();
        }
        if (targetType == Integer.class || targetType == int.class) {
            return toNumber(value).intValue();
        }
        if (targetType == Long.class || targetType == long.class) {
            return toNumber(value).longValue();
        }
        if (targetType == Double.class || targetType == double.class) {
            return toNumber(value).doubleValue();
        }
        if (targetType == Float.class || targetType == float.class) {
            return toNumber(value).floatValue();
        }
        if (targetType == Short.class || targetType == short.class) {
            return toNumber(value).shortValue();
        }
        if (targetType == Byte.class || targetType == byte.class) {
            return toNumber(value).byteValue();
        }
        if (targetType == BigDecimal.class) {
            return value instanceof BigDecimal ? value : new BigDecimal(value.toString());
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return toBoolean(value);
        }
        if (targetType == LocalDate.class) {
            if (value instanceof java.sql.Date d) {
                return d.toLocalDate();
            }
            if (value instanceof Timestamp ts) {
                return ts.toLocalDateTime().toLocalDate();
            }
            return LocalDate.parse(value.toString());
        }
        if (targetType == LocalDateTime.class) {
            if (value instanceof Timestamp ts) {
                return ts.toLocalDateTime();
            }
            if (value instanceof java.sql.Date d) {
                return d.toLocalDate().atStartOfDay();
            }
            return LocalDateTime.parse(value.toString().replace(' ', 'T'));
        }
        if (targetType == Date.class) {
            if (value instanceof Timestamp ts) {
                return new Date(ts.getTime());
            }
            if (value instanceof java.sql.Date d) {
                return new Date(d.getTime());
            }
        }
        return value;
    }

    /** 值类型转换的辅助方法：任意值 -> Number */
    public static Number toNumber(Object value) {
        if (value instanceof Number n) {
            return n;
        }
        if (value instanceof Boolean b) {
            return b ? 1 : 0;
        }
        return new BigDecimal(value.toString().trim());
    }

    /** 值类型转换的辅助方法：任意值 -> Boolean（兼容 MySQL 的 TINYINT、Y/N、true/false） */
    public static Boolean toBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.doubleValue() != 0d;
        }
        String s = value.toString().trim();
        return "1".equals(s) || "true".equalsIgnoreCase(s)
                || "y".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s);
    }

    /** 基本类型在值为 null 时的默认值 */
    public static Object defaultValueOf(Class<?> targetType) {
        if (!targetType.isPrimitive()) {
            return null;
        }
        if (targetType == boolean.class) {
            return Boolean.FALSE;
        }
        if (targetType == char.class) {
            return (char) 0;
        }
        if (targetType == byte.class) {
            return (byte) 0;
        }
        if (targetType == short.class) {
            return (short) 0;
        }
        if (targetType == int.class) {
            return 0;
        }
        if (targetType == long.class) {
            return 0L;
        }
        if (targetType == float.class) {
            return 0F;
        }
        return 0D;
    }

    /** 驼峰命名转下划线命名：enrollmentTime -> enrollment_time */
    public static String camelToUnderscore(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /** 简单日志，便于观察生成的 SQL */
    public static void log(String message) {
        System.out.println("    [JDBCTool] " + message);
    }

    /** 参数校验 */
    private static void requireObject(Object obj, Connection connection) {
        if (obj == null) {
            throw new IllegalArgumentException("实体对象不能为 null");
        }
        if (connection == null) {
            throw new IllegalArgumentException("数据库连接不能为 null");
        }
    }
}
