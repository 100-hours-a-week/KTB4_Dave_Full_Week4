package com.example.community.configuration;

import com.example.community.util.DataManager;
import com.example.community.util.PathDataManager;
import com.example.community.domain.comment.CommentDTO;
import com.example.community.domain.post.PostDTO;
import com.example.community.domain.post.PostEditRecordDTO;
import com.example.community.domain.post.TemporaryPostDTO;
import com.example.community.domain.token.TokenDTO;
import com.example.community.domain.user.UserDTO;
import com.example.community.domain.user.UserLikePostDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;

@Configuration
public class DataManagerConfig {
    @Bean
    public DataManager<UserDTO> userDataManager(@Value("${app.userFile}") String path, ObjectMapper objectMapper) {
        return new PathDataManager<UserDTO>(Path.of(path), objectMapper, UserDTO.class);
    }

    @Bean
    public DataManager<UserLikePostDTO> userLikeDataManager(@Value("${app.userLikeFile}") String path, ObjectMapper objectMapper) {
        return new PathDataManager<UserLikePostDTO>(Path.of(path), objectMapper, UserLikePostDTO.class);
    }

    @Bean
    public DataManager<PostDTO> postDataManager(@Value("${app.postFile}") String path, ObjectMapper objectMapper) {
        return new PathDataManager<PostDTO>(Path.of(path), objectMapper, PostDTO.class);
    }

    @Bean
    public DataManager<PostEditRecordDTO> postEditDataManager(@Value("${app.postEditFile}") String path, ObjectMapper objectMapper) {
        return new PathDataManager<PostEditRecordDTO>(Path.of(path), objectMapper, PostEditRecordDTO.class);
    }

    @Bean
    public DataManager<TemporaryPostDTO> temporaryPostDataManager(@Value("${app.temporaryPostFile}") String path, ObjectMapper objectMapper) {
        return new PathDataManager<TemporaryPostDTO>(Path.of(path), objectMapper, TemporaryPostDTO.class);
    }

    @Bean
    public DataManager<CommentDTO> commentDataManager(@Value("${app.commentFile}") String path, ObjectMapper objectMapper) {
        return new PathDataManager<CommentDTO>(Path.of(path), objectMapper, CommentDTO.class);
    }

    @Bean
    public DataManager<TokenDTO> refreshTokenDataManager(@Value("${app.refreshTokenFile}") String path, ObjectMapper objectMapper){
        return new PathDataManager<TokenDTO>(Path.of(path), objectMapper, TokenDTO.class);
    }



}
