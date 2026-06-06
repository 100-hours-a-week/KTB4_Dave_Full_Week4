package com.example.community.component;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PathDataManager<T> implements DataManager<T>{
    private final Path path;
    private final ObjectMapper objectMapper;

    public PathDataManager(Path path, ObjectMapper objectMapper){
        this.path = path;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<T> readData() {

        List<T> data = null;
        try {
            data = objectMapper.readValue(
                    path.toFile(),
                    new TypeReference<>() {
                    }
            );
        }
        catch(Exception e){
            // 커스텀 예외로 관리할 예정
            if(e instanceof JacksonException){
                System.out.println("객체 형식 안 맞음");
            }
            if(e instanceof UnsupportedOperationException){
                System.out.println("파일 관련 문제, 이름이나 권한 관련");
            }
        }

        return data == null
                ? new ArrayList<>()
                : data;
    }

    @Override
    public void writeData(List<T> data) {
        try {
            objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(
                            path.toFile(),
                            data
                    );
        } catch (Exception e){
            // 커스텀 예외로 관리할 예정
            if(e instanceof JacksonException){
                System.out.println("객체 형식 안 맞음");
            }
            if(e instanceof UnsupportedOperationException){
                System.out.println("파일 관련 문제, 이름이나 권한 관련");
            }
        }
    }
}
