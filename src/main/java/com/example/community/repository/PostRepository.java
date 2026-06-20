package com.example.community.repository;

import com.example.community.domain.post.PostDTO;

import java.util.List;
import java.util.Optional;

public interface PostRepository {
    List<PostDTO> getAllPosts();
    List<PostDTO> getPostsByPage(int index, int offset);
    Optional<PostDTO> getPost(long postNum);
    List<PostDTO> getPostsByUserNum(long userNum, int index, int offset);
    // boolean canAddPost(long profileId); // 당일 작성한 게시글 개수 확인
    int getPostCount();
    PostDTO addPost(PostDTO post);
    PostDTO updatePost(long postNum, String title, String content, String image);
    void view(long postNum);
    int like(long postNum);
    int unLike(long postNum);
    int reportPost(long postNum);
    int addComment(long postNum);
    void deletePost(long postNum);
}
