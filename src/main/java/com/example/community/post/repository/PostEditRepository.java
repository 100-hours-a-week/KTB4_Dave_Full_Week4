package com.example.community.post.repository;

import com.example.community.post.dto.PostEditRecordDTO;

import java.util.List;

public interface PostEditRepository {
    void addPostEditRecord(PostEditRecordDTO postEditRecord);
    List<PostEditRecordDTO> getPostEditRecordByPostNum(long postNum);
    void deletePostEditRecord(long postNum);
}
