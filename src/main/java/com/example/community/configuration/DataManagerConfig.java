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
    public DataManager<User> userDataManager(@Value("${app.userFile}") String path, ObjectMapper objectMapper) {
        return new PathDataManager<User>(Path.of(path), objectMapper, User.class);
    }

    @Bean
    public DataManager<UserLikePost> userLikeDataManager(@Value("${app.userLikeFile}") String path, ObjectMapper objectMapper) {
        return new PathDataManager<UserLikePost>(Path.of(path), objectMapper, UserLikePost.class);
    }

    @Bean
    public DataManager<Post> postDataManager(@Value("${app.postFile}") String path, ObjectMapper objectMapper) {
        return new PathDataManager<Post>(Path.of(path), objectMapper,Post.class);
    }

    @Bean
    public DataManager<PostEditRecord> postEditDataManager(@Value("${app.postEditFile}") String path, ObjectMapper objectMapper) {
        return new PathDataManager<PostEditRecord>(Path.of(path), objectMapper, PostEditRecord.class);
    }

    @Bean
    public DataManager<TemporaryPost> temporaryPostDataManager(@Value("${app.temporaryPostFile}") String path, ObjectMapper objectMapper) {
        return new PathDataManager<TemporaryPost>(Path.of(path), objectMapper, TemporaryPost.class);
    }

    @Bean
    public DataManager<Comment> commentDataManager(@Value("${app.commentFile}") String path, ObjectMapper objectMapper) {
        return new PathDataManager<Comment>(Path.of(path), objectMapper, Comment.class);
    }

    @Bean
    public DataManager<Token> refreshTokenDataManager(@Value("${app.refreshTokenFile}") String path, ObjectMapper objectMapper){
        return new PathDataManager<Token>(Path.of(path), objectMapper, Token.class);
    }



}
