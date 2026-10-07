package com.example.jdbc.entity;

import com.example.jdbc.annotation.Column;
import com.example.jdbc.annotation.Id;
import com.example.jdbc.annotation.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 学生表 student 对应的实体类。
 *
 * <p>表结构：id、name、major、age、enrollment_time（入学时间）、
 * graduated（是否毕业）、tuition（学费）。</p>
 */
@Table("student")
public class Student {

    /** 主键，对应字段 id */
    @Id
    @Column("id")
    private Integer id;

    /** 姓名 */
    @Column("name")
    private String name;

    /** 专业 */
    @Column("major")
    private String major;

    /** 年龄 */
    @Column("age")
    private Integer age;

    /** 入学时间 */
    @Column("enrollment_time")
    private LocalDate enrollmentTime;

    /** 是否毕业：true 已毕业，false 未毕业 */
    @Column("graduated")
    private Boolean graduated;

    /** 学费 */
    @Column("tuition")
    private BigDecimal tuition;

    public Student() {
    }

    public Student(String name, String major, Integer age, LocalDate enrollmentTime,
                   Boolean graduated, BigDecimal tuition) {
        this.name = name;
        this.major = major;
        this.age = age;
        this.enrollmentTime = enrollmentTime;
        this.graduated = graduated;
        this.tuition = tuition;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public LocalDate getEnrollmentTime() {
        return enrollmentTime;
    }

    public void setEnrollmentTime(LocalDate enrollmentTime) {
        this.enrollmentTime = enrollmentTime;
    }

    public Boolean getGraduated() {
        return graduated;
    }

    public void setGraduated(Boolean graduated) {
        this.graduated = graduated;
    }

    public BigDecimal getTuition() {
        return tuition;
    }

    public void setTuition(BigDecimal tuition) {
        this.tuition = tuition;
    }

    @Override
    public String toString() {
        return String.format(
                "Student{id=%d, name='%s', major='%s', age=%d, enrollmentTime=%s, graduated=%s, tuition=%s}",
                id, name, major, age, enrollmentTime, graduated, tuition);
    }
}
