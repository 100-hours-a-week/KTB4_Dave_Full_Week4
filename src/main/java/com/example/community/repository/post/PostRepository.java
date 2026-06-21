package com.example.community.repository.post;

import com.example.community.domain.post.PostDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Optional;

public interface PostRepository {
    Slice<PostDTO> getPostsByPage(int index, int offset);
    Optional<PostDTO> getPost(long postNum);
    List<PostDTO> getPosts(List<Long> postNums);
    Page<PostDTO> getPostsByProfileId(long profileId, int index, int offset);
    // boolean canAddPost(long profileId); // 당일 작성한 게시글 개수 확인
    long getPostCount();
    PostDTO addPost(PostDTO post);
    PostDTO updatePost(long postNum, String title, String content, String image);
    void view(long postNum);
    int like(long postNum);
    int unLike(long postNum);
    int reportPost(long postNum);
    int addComment(long postNum);
    void deletePost(long postNum);
}
