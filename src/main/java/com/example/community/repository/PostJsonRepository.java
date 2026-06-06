package com.example.community.repository;

import com.example.community.domain.post.Post;
import com.example.community.util.DataManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PostJsonRepository implements PostRepository{
    private final DataManager<Post> dataManager;

    public PostJsonRepository(@Qualifier("postDataManager") DataManager<Post> dataManager){
        this.dataManager = dataManager;
    }

    @Override
    public List<Post> getPostsByPage(int index, int offset) {
        List<Post> posts = dataManager.readData();
        List<Post> result = new ArrayList<>();
        for(int i = index*offset; i < (index+1)*offset; i++){
            result.add(posts.get(i));
        }

        return result;
    }

    @Override
    public Optional<Post> getPost(long postNum) {
        List<Post> posts = dataManager.readData();
        for(Post p : posts){
            if(p.getPostNum() == postNum){
                return Optional.of(p);
            }
        }

        return Optional.empty();
    }

    @Override
    public List<Post> getPostsByUserNum(long userNum, int index, int offset) {
        List<Post> posts = dataManager.readData();
        List<Post> userPosts = new ArrayList<>();
        List<Post> results = new ArrayList<>();
        for(Post p : posts){
            if(p.getUserNum() == userNum){
                userPosts.add(p);
            }
        }

        for(int i = index*offset; i < (index+1)*offset; i++){
            results.add(userPosts.get(i));
        }

        return results;
    }

    @Override
    public int getPostCount() {
        List<Post> posts = dataManager.readData();

        return posts.size();
    }

    @Override
    public Post addPost(Post post) {
        List<Post> posts = dataManager.readData();
        posts.add(post);

        dataManager.writeData(posts);

        return post;
    }

    @Override
    public Post updatePost(Post post) {
        List<Post> posts = dataManager.readData();
        for(Post p : posts){
            if(p.getPostNum() == post.getPostNum()){
                p.update(post);
                dataManager.writeData(posts);
                return p;
            }
        }

        throw new RuntimeException("게시글 수정 불가"); // 커스텀 예외
    }

    @Override
    public void addLike(long postNum) {
        List<Post> posts = dataManager.readData();
        for(Post p : posts){
            if(p.getPostNum() == postNum){
                p.like();
                dataManager.writeData(posts);
                return;
            }
        }
        throw new RuntimeException("좋아요 불가"); // 커스텀 예외
    }

    @Override
    public void deleteLike(long postNum) {
        List<Post> posts = dataManager.readData();
        for(Post p : posts){
            if(p.getPostNum() == postNum){
                p.unlike();
                dataManager.writeData(posts);
                return;
            }
        }
        throw new RuntimeException("좋아요 취소 불가"); // 커스텀 예외
    }

    @Override
    public void reportPost(long postNum) {
        List<Post> posts = dataManager.readData();
        for(Post p : posts){
            if(p.getPostNum() == postNum){
                p.report();
                dataManager.writeData(posts);
                return;
            }
        }
        throw new RuntimeException("신고 불가"); // 커스텀 예외
    }

    @Override
    public void addComment(long postNum) {
        List<Post> posts = dataManager.readData();
        for(Post p : posts){
            if(p.getPostNum() == postNum){
                p.addComment();
                dataManager.writeData(posts);
                return;
            }
        }
        throw new RuntimeException("댓글추가 불가"); // 커스텀 예외
    }

    @Override
    public void deletePost(long postNum) {
        List<Post> posts = dataManager.readData();
        for(Post p : posts){
            if(p.getPostNum() == postNum){
                p.delete();
                dataManager.writeData(posts);
                return;
            }
        }
        throw new RuntimeException("게시글 삭제 불가"); // 커스텀 예외
    }
}
