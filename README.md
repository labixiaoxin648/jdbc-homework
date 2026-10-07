# jdbc-homework —— ORM 与 JDBC 作业

用 **Java 反射 + 注解** 实现一个通用 ORM 工具类 `JDBCTool`，完成 `DateTest` 库中
`student`（学生表）与 `college`（学院表）的增、删、改、查。

## 一、作业目标对应关系

| 作业要求 | 实现位置 |
| --- | --- |
| 理解 ORM 基本原理 | `JDBCTool` 通过注解建立「类 ↔ 表」「属性 ↔ 字段」的映射 |
| Java 反射创建对象与属性赋值 | `JDBCTool.newInstance()` / `setFieldValue()` / `getFieldValue()` |
| JDBC 增删改查 | `PreparedStatement` + `ResultSet`，全部使用占位符 `?` 防注入 |
| 方法1 `resultSetToList(ResultSet, Class<T>)` | 结果集 → 对象列表，字段一一对应 |
| 方法2 `save(T, Connection)` | 动态拼装 `INSERT INTO ... VALUES (?, ...)` |
| 方法3 `update(T, Connection)` | 动态拼装 `UPDATE ... SET ... WHERE 主键 = ?` |
| 方法4 `delete(T, Connection)` | 拼装 `DELETE FROM ... WHERE 主键 = ?` |
| 方法5 `getOneById(String, Class<T>, Connection)` | `SELECT * ... WHERE 主键 = ?` → 单个对象 |
| 辅助方法 | `resolveTableName` / `resolveColumnName` / `resolveIdField` / `convertValue` / `bindParameters` / `executeUpdate` / `camelToUnderscore` 等 |
| 数据库 DateTest 两张表 | `sql/date_test.sql`（student 7 个字段，college 3 个字段） |
| Maven 构建 | `pom.xml`，`mvn clean compile exec:java` 一键运行 |
| Git 版本管理 | 仓库根目录 `.git/`，提交历史见 `git log` |
| 运行效果抓图 | `doc/screenshots/运行截图01~03.png` |

## 二、项目结构

```
jdbc-homework
├── pom.xml                                  # Maven 构建配置（MySQL 8 驱动 + exec 插件）
├── sql/
│   └── date_test.sql                        # 建库建表 + 初始数据脚本
├── doc/
│   ├── run-output.txt                       # 完整控制台运行输出
│   └── screenshots/                         # 运行效果抓图
└── src/main/java/com/example/jdbc/
    ├── JDBCTool.java                        # ★ 通用 ORM 工具类（5 个方法 + 辅助方法）
    ├── Main.java                            # 演示入口：两表增删改查
    ├── annotation/
    │   ├── Table.java                       # 类 -> 表名
    │   ├── Column.java                      # 属性 -> 字段名
    │   └── Id.java                          # 主键标记
    ├── entity/
    │   ├── Student.java                     # 学生实体（id/name/major/age/入学时间/是否毕业/学费）
    │   └── College.java                     # 学院实体（id/name/code）
    └── util/
        └── DBUtil.java                      # 连接与关闭的统一管理
```

## 三、运行步骤

1. 准备 MySQL 8.0（本机 3306，账号 `root` / `123456`）；
2. 执行建库脚本（也可跳过，程序启动时会自动 `CREATE TABLE IF NOT EXISTS`）：

   ```bash
   mysql -u root -p < sql/date_test.sql
   ```

3. 编译并运行：

   ```bash
   mvn clean compile exec:java
   ```

4. 若账号密码不同，可用系统属性覆盖：

   ```bash
   mvn clean compile exec:java -Dexec.jvmArgs="-Djdbc.password=你的密码"
   ```

## 四、ORM 映射原理简述

- **表名映射**：实体类标注 `@Table("student")`；未标注时按驼峰转下划线（`StudentInfo` → `student_info`）；
- **字段映射**：属性标注 `@Column("enrollment_time")`；未标注时同样驼峰转下划线（`enrollmentTime` → `enrollment_time`）；
- **主键映射**：属性标注 `@Id`，`update` / `delete` / `getOneById` 以它作为 WHERE 条件；
- **对象创建**：`clazz.getDeclaredConstructor().newInstance()` 反射调用无参构造；
- **属性赋值**：`Field.set(obj, value)`，并用 `convertValue()` 处理
  `TINYINT → Boolean`、`DATE → LocalDate`、`DECIMAL → BigDecimal` 等类型转换。

新增实体类时只需按同样规则加注解，`JDBCTool` 一行代码都不用改。
