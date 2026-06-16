package com.example.community.repository;

import com.example.community.domain.post.Post;

import java.util.List;
import java.util.Optional;

public interface PostRepository {
    List<Post> getAllPosts();
    List<Post> getPostsByPage(int index, int offset);
    Optional<Post> getPost(long postNum);
    List<Post> getPostsByUserNum(long userNum, int index, int offset);
    // boolean canAddPost(long userNum); // 당일 작성한 게시글 개수 확인
    int getPostCount();
    Post addPost(Post post);
    Post updatePost(long postNum, String title, String content, String image);
    void view(long postNum);
    int like(long postNum);
    int unLike(long postNum);
    int reportPost(long postNum);
    int addComment(long postNum);
    void deletePost(long postNum);
}
