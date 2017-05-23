package org.springframework.batch.item.file.mapping;

class JsonTestType {
    private int age;
    private String name;

    public JsonTestType() {
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}