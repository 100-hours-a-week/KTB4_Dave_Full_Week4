package com.example.community.repository;

import com.example.community.domain.post.Post;

import java.util.List;
import java.util.Optional;

public interface PostRepository {
    List<Post> getPostsByPage(int index, int offset);
    Optional<Post> getPost(long postNum);
    List<Post> getPostsByUserNum(long userNum, int index, int offset);
    int getPostCount();
    Post addPost(Post post);
    Post updatePost(Post post);
    void addLike(long postNum);
    void deleteLike(long postNum);
    void reportPost(long postNum);
    void addComment(long postNum);
    void deletePost(long postNum);
}
