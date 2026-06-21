package com.example.community.repository.comment;

import com.example.community.domain.comment.CommentDTO;
import com.example.community.domain.exception.NotFoundException;
import com.example.community.util.DataManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CommentJsonRepository implements CommentRepository{
    private final DataManager<CommentDTO> dataManager;

    public CommentJsonRepository(@Qualifier("commentDataManager") DataManager<CommentDTO> dataManager){
        this.dataManager = dataManager;
    }

    @Override
    public CommentDTO addComment(CommentDTO comment) {
        List<CommentDTO> comments = dataManager.readData();
        comments.add(comment);

        dataManager.writeData(comments);
        return comment;
    }

    @Override
    public List<CommentDTO> getCommentsByPostNum(long postNum) {
        return dataManager.readData().stream()
                .filter(c -> c.getPostNum() == postNum).toList();
    }

    @Override
    public Optional<CommentDTO> getComment(long commentNum) {
        for(CommentDTO c : dataManager.readData()){
            if(c.getCommentNum() == commentNum){
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    @Override
    public long getCommentCount() {
        return dataManager.readData().size();
    }

    @Override
    public CommentDTO updateComment(long commentNum, String content) {
        List<CommentDTO> comments = dataManager.readData();
        for(CommentDTO c : comments){
            if(c.getCommentNum() == commentNum){
                c.update(content);
                dataManager.writeData(comments);
                return c;
            }
        }
        throw new NotFoundException("존재하지 않는 댓글");
    }

    @Override
    public void deleteComment(long commentNum) {
        List<CommentDTO> comments = dataManager.readData();
        for(CommentDTO c: comments){
            if(c.getCommentNum() == commentNum){
                c.delete();
                dataManager.writeData(comments);
                return;
            }
        }
    }
}
