package com.appiphany.nacc.model;

import java.io.Serializable;

public class CacheItem implements Serializable{
    public static final String TABLE_NAME = "cache_item";
    public static final String ID = "id";
    public static final String TYPE = "type";
    public static final String DATA = "data";

    public static final int TYPE_SITE = 1;
    public static final int TYPE_MARK_GUIDE = 2;
    public static final int TYPE_REMOVE_GUIDE = 3;

    private String id;
    private int type;
    private String data;

    public CacheItem() {
    }

    public CacheItem(String id, int type, String data) {
        this.id = id;
        this.type = type;
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
