package component;

import java.util.List;

public interface DataManager<T> {
    List<T> readDate();
    void writeData(List<T> list);
}
