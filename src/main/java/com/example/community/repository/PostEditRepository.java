package com.example.community.repository;

import com.example.community.domain.post.PostEditRecord;

import java.util.List;

public interface PostEditRepository {
    void addPostEditRecord(PostEditRecord postEditRecord);
    List<PostEditRecord> getPostEditRecordByPostNum(long postNum);
    void deletePostEditRecord(long postNum);
}
