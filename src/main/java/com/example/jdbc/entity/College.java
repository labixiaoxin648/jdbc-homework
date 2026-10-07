package com.example.jdbc.entity;

import com.example.jdbc.annotation.Column;
import com.example.jdbc.annotation.Id;
import com.example.jdbc.annotation.Table;

/**
 * 学院表 college 对应的实体类。
 *
 * <p>表结构：id、name、code。</p>
 */
@Table("college")
public class College {

    /** 主键，对应字段 id */
    @Id
    @Column("id")
    private Integer id;

    /** 学院名称 */
    @Column("name")
    private String name;

    /** 学院代码 */
    @Column("code")
    private String code;

    public College() {
    }

    public College(String name, String code) {
        this.name = name;
        this.code = code;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return String.format("College{id=%d, name='%s', code='%s'}", id, name, code);
    }
}
