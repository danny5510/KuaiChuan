package com.sharemedia.kuaichuan.entities;

public class Project {
    public String name;
    public String address;
    public int percent;
    public int qty_required;
    public int qty_finished;
    public Project(String n, String a) {
        this.name = n;
        this.address = a;
    }
}
