package com.example.community.repository.post;

import com.example.community.domain.post.PostEditRecordDTO;

import java.util.List;

public interface PostEditRepository {
    void addPostEditRecord(PostEditRecordDTO postEditRecord);
    List<PostEditRecordDTO> getPostEditRecordByPostNum(long postNum);
    void deletePostEditRecord(long postNum);
}
