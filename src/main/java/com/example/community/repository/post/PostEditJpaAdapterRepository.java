package com.example.community.repository.post;

import com.example.community.domain.exception.NotFoundException;
import com.example.community.domain.post.Post;
import com.example.community.domain.post.PostEditRecord;
import com.example.community.domain.post.PostEditRecordDTO;
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
