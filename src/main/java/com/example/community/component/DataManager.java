package com.example.community.component;

import java.util.List;

public interface DataManager<T> {
    List<T> readData();
    void writeData(List<T> list);
}
