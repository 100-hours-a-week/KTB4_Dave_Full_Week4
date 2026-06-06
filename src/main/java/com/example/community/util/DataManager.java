package com.example.community.util;

import java.util.List;

public interface DataManager<T> {
    List<T> readData();
    void writeData(List<T> list);
}
