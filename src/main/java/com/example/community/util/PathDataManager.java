package com.example.community.util;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PathDataManager<T> implements DataManager<T>{
    private final Path path;
    private final ObjectMapper objectMapper;
    private final Class<T> type;

    public PathDataManager(Path path, ObjectMapper objectMapper, Class<T> type) {
        this.path = path;
        this.objectMapper = objectMapper;
        this.type = type;
    }

    @Override
    public List<T> readData() {
        try {
            return objectMapper.readValue(
                    path.toFile(),
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, type)
            );
        }
        catch(Exception e){
            // 커스텀 예외로 관리할 예정
            if(e instanceof JacksonException){
                System.out.println("객체 형식 안 맞음");
                throw e;
            }
            if(e instanceof UnsupportedOperationException){
                System.out.println("파일 관련 문제, 이름이나 권한 관련");
                throw e;
            }
            e.printStackTrace();
        }
        return List.of();
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
