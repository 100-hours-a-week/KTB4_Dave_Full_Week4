package com.example.community.post.repository;

import com.example.community.post.dto.PostEditRecordDTO;
import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostEditRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PostEditJpaAdapterRepository implements PostEditRepository{
    private final PostEditJpaRepository postEditJpaRepository;
    private final PostJpaRepository postJpaRepository;

    @Override
    public void addPostEditRecord(PostEditRecordDTO postEditRecord) {
        Post post = postJpaRepository.getReferenceById(postEditRecord.postNum());
        PostEditRecord postEdit = new PostEditRecord(post, postEditRecord.version(), postEditRecord.title()
                , postEditRecord.content(), postEditRecord.image(), postEditRecord.writeTime());
        postEditJpaRepository.save(postEdit);
    }

    @Override
    public List<PostEditRecordDTO> getPostEditRecordByPostNum(long postNum) {
        return postEditJpaRepository.findByPost_PostNumOrderByEditIdDesc(postNum)
                .stream().map(PostEditRecordDTO::from).toList();
    }

    @Override
    public void deletePostEditRecord(long postNum) {
        List<PostEditRecord> postEditRecord = postEditJpaRepository.findByPost_PostNumOrderByEditIdDesc(postNum);
        postEditJpaRepository.deleteAll(postEditRecord);
    }
}
