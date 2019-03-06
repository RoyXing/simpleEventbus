package com.xingzy;

/**
 * @author roy.xing
 * @date 2019/3/6
 */
public class EventBean {

    private String name;
    private int age;

    public EventBean(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
