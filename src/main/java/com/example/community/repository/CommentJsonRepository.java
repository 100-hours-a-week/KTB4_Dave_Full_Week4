package com.example.community.repository;

import com.example.community.domain.comment.Comment;
import com.example.community.util.DataManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CommentJsonRepository implements CommentRepository{
    private final DataManager<Comment> dataManager;

    public CommentJsonRepository(@Qualifier("commentDataManager") DataManager<Comment> dataManager){
        this.dataManager = dataManager;
    }

    @Override
    public Comment addComment(Comment comment) {
        List<Comment> comments = dataManager.readData();
        comments.add(comment);

        dataManager.writeData(comments);
        return comment;
    }

    @Override
    public List<Comment> getCommentsByPostNum(long postNum) {
        return dataManager.readData().stream()
                .filter(c -> c.getPostNum() == postNum).toList();
    }

    @Override
    public Optional<Comment> updateComment(long commentNum, String content) {
        List<Comment> comments = dataManager.readData();
        for(Comment c : comments){
            if(c.getCommentNum() == commentNum){
                c.update(content);
                dataManager.writeData(comments);
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    @Override
    public void deleteComment(long commentNum) {
        List<Comment> comments = dataManager.readData();
        for(Comment c: comments){
            if(c.getCommentNum() == commentNum){
                c.delete();
                dataManager.writeData(comments);
                return;
            }
        }
    }
}
