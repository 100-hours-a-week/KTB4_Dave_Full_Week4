package com.example.community.configuration;

import com.example.community.util.DataManager;
import com.example.community.util.PathDataManager;
import com.example.community.domain.comment.Comment;
import com.example.community.domain.post.Post;
import com.example.community.domain.post.PostEditRecord;
import com.example.community.domain.post.TemporaryPost;
import com.example.community.domain.token.Token;
import com.example.community.domain.user.User;
import com.example.community.domain.user.UserLikePost;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;

@Configuration
public class DataManagerConfig {
    @Bean
    public DataManager<User> userDataManager(@Value("${app.userFile") Path path, ObjectMapper objectMapper) {
        return new PathDataManager<User>(path, objectMapper);
    }

    @Bean
    public DataManager<UserLikePost> userLikeDataManager(@Value("${app.userLikeFile") Path path, ObjectMapper objectMapper) {
        return new PathDataManager<UserLikePost>(path, objectMapper);
    }

    @Bean
    public DataManager<Post> postDataManager(@Value("${app.postFile") Path path, ObjectMapper objectMapper) {
        return new PathDataManager<Post>(path, objectMapper);
    }

    @Bean
    public DataManager<PostEditRecord> postEditDataManager(@Value("${app.postEditFile") Path path, ObjectMapper objectMapper) {
        return new PathDataManager<PostEditRecord>(path, objectMapper);
    }

    @Bean
    public DataManager<TemporaryPost> temporaryPostDataManager(@Value("${app.temporaryPostFile") Path path, ObjectMapper objectMapper) {
        return new PathDataManager<TemporaryPost>(path, objectMapper);
    }

    @Bean
    public DataManager<Comment> commentDataManager(@Value("${app.commentFile") Path path, ObjectMapper objectMapper) {
        return new PathDataManager<Comment>(path, objectMapper);
    }

    @Bean
    public DataManager<Token> refreshTokenDataManager(@Value("${app.refershTokenFile") Path path, ObjectMapper objectMapper){
        return new PathDataManager<Token>(path, objectMapper);
    }



}
