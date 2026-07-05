package com.example.community.post.repository;

import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.dto.PostDTO;
import com.example.community.util.DataManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PostJsonRepository implements PostRepository{
    private final DataManager<PostDTO> dataManager;

    public PostJsonRepository(@Qualifier("postDataManager") DataManager<PostDTO> dataManager){
        this.dataManager = dataManager;
    }


    @Override
    public Slice<PostDTO> getPostsByPage(int index, int offset) {
        List<PostDTO> posts = dataManager.readData().stream()
                .filter(p -> !p.isDeleted()).toList();
        int end = posts.size();
        if(end > (index+1)*offset){
            end = (index+1)*offset+1;
        }
        List<PostDTO> result = new ArrayList<>();
        for(int i = index*offset; i < end; i++){
            result.add(posts.get(i));
        }
        boolean hasNext = false;
        if(result.size() > offset){
            hasNext = true;
            result.removeLast();
        }
        Pageable pageable = PageRequest.of(index, offset);

        return new SliceImpl<>(result, pageable, hasNext);
    }

    @Override
    public Optional<PostDTO> getPost(long postNum) {
        List<PostDTO> posts = dataManager.readData();
        for(PostDTO p : posts){
            if(p.getPostNum() == postNum && !p.isDeleted()){
                dataManager.writeData(posts);
                return Optional.of(p);
            }
        }

        return Optional.empty();
    }

    @Override
    public List<PostDTO> getPosts(List<Long> postNums) {
        return List.of();
    }

    @Override
    public Page<PostDTO> getPostsByProfileId(long userNum, int index, int offset) {
        List<PostDTO> posts = dataManager.readData();
        List<PostDTO> userPosts = new ArrayList<>();
        List<PostDTO> results = new ArrayList<>();
        for(PostDTO p : posts){
            if(p.getProfileId() == userNum && !p.isDeleted()){
                userPosts.add(p);
            }
        }

        int end = userPosts.size();
        if(end > (index+1)*offset){
            end = (index+1)*offset;
        }

        for(int i = index*offset; i < end; i++){
            results.add(userPosts.get(i));
        }

        return new PageImpl<>(results, PageRequest.of(index, offset), userPosts.size());
    }

    @Override
    public long getPostCount() {
        List<PostDTO> posts = dataManager.readData().stream()
                .filter(p -> !p.isDeleted()).toList();

        return posts.size();
    }

    @Override
    public PostDTO addPost(PostDTO post) {
        List<PostDTO> posts = dataManager.readData();
        posts.add(post);

        dataManager.writeData(posts);

        return post;
    }

    @Override
    public PostDTO updatePost(long postNum, String title, String content, String image) {
        List<PostDTO> posts = dataManager.readData();
        for(PostDTO p : posts){
            if(p.getPostNum() == postNum && !p.isDeleted()){
                p.update(title, content, image);
                dataManager.writeData(posts);
                return p;
            }
        }

        throw new NotFoundException("존재하지 않는 게시글"); // 커스텀 예외
    }

    @Override
    public void view(long postNum) {
        List<PostDTO> posts = dataManager.readData();
        for(PostDTO p : posts){
            if(p.getPostNum() == postNum && !p.isDeleted()){
                p.view();
                dataManager.writeData(posts);
                return;
            }
        }

        throw new NotFoundException("존재하지 않는 게시글"); // 커스텀 예외
    }


    @Override
    public int like(long postNum) {
        List<PostDTO> posts = dataManager.readData();
        for(PostDTO p : posts){
            if(p.getPostNum() == postNum && !p.isDeleted()){
                p.like();
                dataManager.writeData(posts);
                return p.getLikeCount();
            }
        }
        throw new NotFoundException("존재하지 않는 게시글"); // 커스텀 예외
    }

    @Override
    public int unLike(long postNum) {
        List<PostDTO> posts = dataManager.readData();
        for(PostDTO p : posts){
            if(p.getPostNum() == postNum && !p.isDeleted()){
                p.unlike();
                dataManager.writeData(posts);
                return p.getLikeCount();
            }
        }
        throw new NotFoundException("존재하지 않는 게시글"); // 커스텀 예외
    }

    @Override
    public int reportPost(long postNum) {
        List<PostDTO> posts = dataManager.readData();
        for(PostDTO p : posts){
            if(p.getPostNum() == postNum && !p.isDeleted()){
                p.report();
                dataManager.writeData(posts);
                return p.getReportCount();
            }
        }
        throw new NotFoundException("존재하지 않는 게시글"); // 커스텀 예외
    }

    @Override
    public int addComment(long postNum) {
        List<PostDTO> posts = dataManager.readData();
        for(PostDTO p : posts){
            if(p.getPostNum() == postNum && !p.isDeleted()){
                p.addComment();
                dataManager.writeData(posts);
                return p.getCommentCount();
            }
        }
        throw new NotFoundException("존재하지 않는 게시글"); // 커스텀 예외
    }

    @Override
    public void deletePost(long postNum) {
        List<PostDTO> posts = dataManager.readData();
        for(PostDTO p : posts){
            if(p.getPostNum() == postNum && !p.isDeleted()){
                p.delete();
                dataManager.writeData(posts);
                return;
            }
        }
        throw new NotFoundException("존재하지 않는 게시글"); // 커스텀 예외
    }
}
