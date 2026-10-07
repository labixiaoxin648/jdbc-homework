package com.example.jdbc;

import com.example.jdbc.entity.College;
import com.example.jdbc.entity.Student;
import com.example.jdbc.util.DBUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

/**
 * 作业演示主类：使用 {@link JDBCTool} 完成 DateTest 库中
 * student（学生表）与 college（学院表）的增、删、改、查操作。
 *
 * <p>运行方式：{@code mvn clean compile exec:java}</p>
 */
public class Main {

    public static void main(String[] args) {
        printTitle();
        try (Connection connection = DBUtil.getConnection()) {
            String displayUrl = DBUtil.getUrl().split("\\?")[0];
            System.out.println("[连接成功] " + displayUrl);
            System.out.println("           用户：" + DBUtil.getUser()
                    + "（字符集 utf8，时区 Asia/Shanghai，完整 URL 见 util/DBUtil.java）");
            ensureSchema(connection);

            demoCollege(connection);
            demoStudent(connection);
            demoGetOneById(connection);
            demoSourceFreeQuery(connection);
        } catch (Exception e) {
            System.err.println("[运行失败] " + e.getMessage());
            e.printStackTrace();
        }
        printTail();
    }

    // ==========================================================
    // 一、学院表 college 的增删改查
    // ==========================================================
    private static void demoCollege(Connection connection) {
        section("一、学院表 college —— 增、删、改、查");

        // ---- 增 ----
        step("1. 新增学院 save()");
        College newCollege = new College("自动化学院", "AUTO");
        int rows = JDBCTool.save(newCollege, connection);
        System.out.println("    新增行数：" + rows + "，实体对象：" + newCollege + "（id 由数据库自增后未回填）");

        // ---- 查（全表） ----
        step("2. 查询全部学院 resultSetToList()");
        List<College> all = queryColleges(connection);
        all.forEach(c -> System.out.println("    " + c));

        // ---- 改 ----
        if (!all.isEmpty()) {
            College target = all.get(0);
            step("3. 修改学院 update()：把 id=" + target.getId() + " 的名称改为「计算机与人工智能学院」");
            target.setName("计算机与人工智能学院");
            int updated = JDBCTool.update(target, connection);
            System.out.println("    修改行数：" + updated + "，修改后：" + JDBCTool.getOneById(String.valueOf(target.getId()), College.class, connection));
        }

        // ---- 删 ----
        College last = queryColleges(connection).stream().reduce((a, b) -> b).orElse(null);
        if (last != null) {
            step("4. 删除学院 delete()：删除 id=" + last.getId() + "（" + last.getName() + "）");
            int deleted = JDBCTool.delete(last, connection);
            System.out.println("    删除行数：" + deleted);
        }
        System.out.println("    删除后剩余学院：");
        queryColleges(connection).forEach(c -> System.out.println("      " + c));
    }

    private static List<College> queryColleges(Connection connection) {
        String sql = "SELECT * FROM college ORDER BY id";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return JDBCTool.resultSetToList(rs, College.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==========================================================
    // 二、学生表 student 的增删改查
    // ==========================================================
    private static void demoStudent(Connection connection) {
        section("二、学生表 student —— 增、删、改、查（含入学时间 / 是否毕业 / 学费）");

        // ---- 增 ----
        step("1. 新增学生 save()");
        Student s1 = new Student("赵六", "人工智能", 19, LocalDate.of(2024, 9, 1), false, new BigDecimal("6200.00"));
        Student s2 = new Student("孙七", "数据科学与大数据技术", 23, LocalDate.of(2020, 9, 1), true, new BigDecimal("5900.50"));
        System.out.println("    " + JDBCTool.save(s1, connection) + " 行受影响 -> " + s1.getName());
        System.out.println("    " + JDBCTool.save(s2, connection) + " 行受影响 -> " + s2.getName());

        // ---- 查（全表） ----
        step("2. 查询全部学生 resultSetToList()（ResultSet -> 实体列表，字段一一对应）");
        List<Student> students = queryStudents(connection);
        students.forEach(s -> System.out.println("    " + s));
        System.out.println("    共 " + students.size() + " 条记录，且 graduated 已由 TINYINT 映射为 boolean");
        students.stream()
                .filter(s -> Boolean.TRUE.equals(s.getGraduated()))
                .forEach(s -> System.out.println("    [已毕业] " + s.getName() + " 学费 " + s.getTuition()));

        // ---- 改 ----
        Student toUpdate = queryStudents(connection).stream()
                .filter(s -> "赵六".equals(s.getName()))
                .findFirst()
                .orElse(null);
        if (toUpdate != null) {
            step("3. 修改学生 update()：把「赵六」的年龄改为 20、学费改为 6666.66、标记为已毕业");
            toUpdate.setAge(20);
            toUpdate.setTuition(new BigDecimal("6666.66"));
            toUpdate.setGraduated(true);
            int updated = JDBCTool.update(toUpdate, connection);
            System.out.println("    修改行数：" + updated);
            System.out.println("    修改后：" + JDBCTool.getOneById(String.valueOf(toUpdate.getId()), Student.class, connection));
        }

        // ---- 删 ----
        Student toDelete = queryStudents(connection).stream()
                .filter(s -> "孙七".equals(s.getName()))
                .findFirst()
                .orElse(null);
        if (toDelete != null) {
            step("4. 删除学生 delete()：删除「" + toDelete.getName() + "」(id=" + toDelete.getId() + ")");
            System.out.println("    删除行数：" + JDBCTool.delete(toDelete, connection));
        }
        System.out.println("    删除后学生列表：");
        queryStudents(connection).forEach(s -> System.out.println("      " + s));
    }

    private static List<Student> queryStudents(Connection connection) {
        String sql = "SELECT * FROM student ORDER BY id";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return JDBCTool.resultSetToList(rs, Student.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==========================================================
    // 三、按主键查询
    // ==========================================================
    private static void demoGetOneById(Connection connection) {
        section("三、按主键查询 getOneById()");
        List<Student> students = queryStudents(connection);
        if (students.isEmpty()) {
            System.out.println("    学生表暂无数据");
            return;
        }
        Student first = students.get(0);
        step("根据 id=" + first.getId() + " 查询学生");
        Student found = JDBCTool.getOneById(String.valueOf(first.getId()), Student.class, connection);
        System.out.println("    查到：" + found);

        step("根据一个不存在的 id=999999 查询学生");
        System.out.println("    查到：" + JDBCTool.getOneById("999999", Student.class, connection) + "（返回 null 表示无此记录）");

        List<College> colleges = queryColleges(connection);
        if (!colleges.isEmpty()) {
            College c = colleges.get(0);
            step("根据 id=" + c.getId() + " 查询学院");
            System.out.println("    查到：" + JDBCTool.getOneById(String.valueOf(c.getId()), College.class, connection));
        }
    }

    // ==========================================================
    // 四、验证：同一个 JDBCTool 可以服务任意实体（ORM 通用性）
    // ==========================================================
    private static void demoSourceFreeQuery(Connection connection) {
        section("四、通用性验证：JDBCTool 不依赖任何具体实体类");
        System.out.println("    实体类的表名 / 字段名 / 主键全部来自注解：");
        System.out.println("      Student -> 表 " + JDBCTool.resolveTableName(Student.class)
                + "，主键 " + JDBCTool.resolveIdField(Student.class).getName());
        System.out.println("      College -> 表 " + JDBCTool.resolveTableName(College.class)
                + "，主键 " + JDBCTool.resolveIdField(College.class).getName());
        System.out.println("    新增实体类时，只要按同样规则加注解，无需修改 JDBCTool 一行代码。");
    }

    // ==========================================================
    // 建表辅助：表不存在时自动创建，保证演示可直接运行
    // ==========================================================
    private static void ensureSchema(Connection connection) {
        String[] ddl = {
                "CREATE TABLE IF NOT EXISTS college ("
                        + "id INT NOT NULL AUTO_INCREMENT,"
                        + "name VARCHAR(50) NOT NULL,"
                        + "code VARCHAR(20) NOT NULL,"
                        + "PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                "CREATE TABLE IF NOT EXISTS student ("
                        + "id INT NOT NULL AUTO_INCREMENT,"
                        + "name VARCHAR(50) NOT NULL,"
                        + "major VARCHAR(50) DEFAULT NULL,"
                        + "age INT DEFAULT NULL,"
                        + "enrollment_time DATE DEFAULT NULL,"
                        + "graduated TINYINT(1) NOT NULL DEFAULT 0,"
                        + "tuition DECIMAL(10,2) DEFAULT NULL,"
                        + "PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        };
        try (Statement statement = connection.createStatement()) {
            for (String sql : ddl) {
                statement.executeUpdate(sql);
            }
            System.out.println("[表结构] college / student 已就绪");
        } catch (Exception e) {
            throw new RuntimeException("初始化表结构失败：" + e.getMessage(), e);
        }
    }

    // ==========================================================
    // 控制台输出辅助
    // ==========================================================
    private static void printTitle() {
        System.out.println("================================================================");
        System.out.println("        ORM + JDBC 作业演示 —— JDBCTool 通用工具类");
        System.out.println("        数据库：DateTest      表：student / college");
        System.out.println("        技术点：Java 反射 + 注解映射 + JDBC 预编译语句");
        System.out.println("================================================================");
    }

    private static void printTail() {
        System.out.println("================================================================");
        System.out.println("        演示结束：两张表的增、删、改、查全部执行完成");
        System.out.println("================================================================");
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("----------------------------------------------------------------");
        System.out.println("  " + title);
        System.out.println("----------------------------------------------------------------");
    }

    private static void step(String title) {
        System.out.println();
        System.out.println("  >> " + title);
    }
}
