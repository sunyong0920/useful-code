package com.vdian.useful.domain;

import java.lang.reflect.Field;

/**
 * @author jifang
 * @since 16/7/22 下午5:57.
 */
public class Teacher extends User {

    private String name;

    public Teacher() {
    }

    public Teacher(String name) {
        this.name = name;
    }

    public static void main(String[] args) {
        Field[] fields = Teacher.class.getSuperclass().getDeclaredFields();
        for (Field field : fields) {
            System.out.println(field.getName());
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
