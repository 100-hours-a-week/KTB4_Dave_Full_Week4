package component;

import domain.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class UserDataManager implements DataManager<User>{
    private final Path userFile;
    private final ObjectMapper objectMapper;

    public UserDataManager(@Value("${app.userFile}") Path userFile, ObjectMapper objectMapper){
        this.userFile = userFile;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<User> readDate() {
        List<User> users =
                objectMapper.readValue(
                        userFile.toFile(),
                        new TypeReference<List<User>>() {}
                );

        return users == null
                ? new ArrayList<>()
                : users;
    }

    @Override
    public void writeData(List<User> list) {
        objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValue(
                        userFile.toFile(),
                        list
                );
    }
}
