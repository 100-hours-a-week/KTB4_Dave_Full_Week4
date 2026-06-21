package com.example.community.repository.post;

import com.example.community.domain.exception.NotFoundException;
import com.example.community.domain.post.Post;
import com.example.community.domain.post.PostDTO;
import com.example.community.domain.user.UserInfo;
import com.example.community.repository.user.UserInfoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PostJpaAdapterRepository implements PostRepository{
    private final PostJpaRepository postJpaRepository;
    private final UserInfoJpaRepository userInfoJpaRepository;

    @Override
    public Slice<PostDTO> getPostsByPage(int index, int offset) {
        Pageable pageable = PageRequest.of(index, offset);
        Slice<Post> posts = postJpaRepository.findAllByOrderByPostNumDesc(pageable);
        List<PostDTO> postDTOS = posts.getContent()
                .stream().map(PostDTO::from).toList();
        return new SliceImpl<>(postDTOS, posts.getPageable(), posts.hasNext());
    }

    @Override
    public Optional<PostDTO> getPost(long postNum) {
        Optional<Post> post = postJpaRepository.findByPostNum(postNum);
        return post.map(PostDTO::from);
    }

    @Override
    public List<PostDTO> getPosts(List<Long> postNums) {
        return postJpaRepository.findByPostNumIn(postNums)
                .stream().map(PostDTO::from).toList();
    }

    @Override
    public Page<PostDTO> getPostsByProfileId(long profileId, int index, int offset) {
        Pageable pageable = PageRequest.of(index, offset);
        Page<Post> posts = postJpaRepository.findByUserInfo_ProfileIdOrderByPostNumDesc(profileId, pageable);
        List<PostDTO> postDTOS = posts.getContent().stream().map(PostDTO::from).toList();

        return new PageImpl<>(postDTOS, pageable, posts.getTotalElements());
    }

    @Override
    public long getPostCount()
    {
        return postJpaRepository.count();
    }

    @Override
    public PostDTO addPost(PostDTO post) {
        UserInfo userInfo = userInfoJpaRepository.getReferenceById(post.getProfileId());
        Post newPost = new Post(userInfo, post.getTitle(), post.getContent()
                , post.getImage());
        postJpaRepository.save(newPost);
        return PostDTO.from(newPost);
    }

    @Override
    public PostDTO updatePost(long postNum, String title, String content, String image) {
        Post post = postJpaRepository.findByPostNum(postNum)
                .orElseThrow(()-> new NotFoundException("존재하지 않는 게시글"));
        post.update(title, content, image);
        postJpaRepository.save(post);
        return PostDTO.from(post);
    }

    @Override
    public void view(long postNum) {
        Post post = postJpaRepository.findByPostNum(postNum)
                .orElseThrow(()-> new NotFoundException("존재하지 않는 게시글"));
        post.view();
        postJpaRepository.save(post);
    }

    @Override
    public int like(long postNum) {
        Post post = postJpaRepository.findByPostNum(postNum)
                .orElseThrow(()-> new NotFoundException("존재하지 않는 게시글"));
        post.like();
        postJpaRepository.save(post);
        return post.getLikeCount();
    }

    @Override
    public int unLike(long postNum) {
        Post post = postJpaRepository.findByPostNum(postNum)
                .orElseThrow(()-> new NotFoundException("존재하지 않는 게시글"));
        post.unlike();
        postJpaRepository.save(post);
        return post.getLikeCount();
    }

    @Override
    public int reportPost(long postNum) {
        Post post = postJpaRepository.findByPostNum(postNum)
                .orElseThrow(()-> new NotFoundException("존재하지 않는 게시글"));
        post.report();
        postJpaRepository.save(post);
        return post.getReportCount();
    }

    @Override
    public int addComment(long postNum) {
        Post post = postJpaRepository.findByPostNum(postNum)
                .orElseThrow(()-> new NotFoundException("존재하지 않는 게시글"));
        post.addComment();
        postJpaRepository.save(post);
        return post.getCommentCount();
    }

    @Override
    public void deletePost(long postNum) {
        Post post = postJpaRepository.findByPostNum(postNum)
                .orElseThrow(()-> new NotFoundException("존재하지 않는 게시글"));
        post.delete();
        postJpaRepository.save(post);
    }
}
