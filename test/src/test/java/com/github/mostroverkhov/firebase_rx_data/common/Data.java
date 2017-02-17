package com.github.mostroverkhov.firebase_rx_data.common;

/**
 * Created by Maksym Ostroverkhov on 15.02.2017.
 */
public final class Data {
    private int id;
    private String data;

    public Data(int id, String data) {
        this.id = id;
        this.data = data;
    }

    public Data() {
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getId() {
        return id;
    }

    public String getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Data data1 = (Data) o;

        if (id != data1.id) return false;
        return data != null ? data.equals(data1.data) : data1.data == null;

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Data{");
        sb.append("id=").append(id);
        sb.append(", data='").append(data).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
