package com.example.community.repository.post;

import com.example.community.domain.post.PostEditRecordDTO;
import com.example.community.util.DataManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class PostEditJsonRepository implements PostEditRepository{
    private final DataManager<PostEditRecordDTO> dataManager;

    public PostEditJsonRepository(@Qualifier("postEditDataManager")DataManager<PostEditRecordDTO> dataManager){
        this.dataManager = dataManager;
    }

    @Override
    public void addPostEditRecord(PostEditRecordDTO postEditRecord) {
        List<PostEditRecordDTO> postEditRecords = dataManager.readData();
        postEditRecords.add(postEditRecord);

        dataManager.writeData(postEditRecords);
    }

    @Override
    public List<PostEditRecordDTO> getPostEditRecordByPostNum(long postNum) {

        return dataManager.readData().stream().filter(er -> er.postNum() == postNum).toList();
    }

    @Override
    public void deletePostEditRecord(long postNum) {
        List<PostEditRecordDTO> postEditRecords = dataManager.readData();
        List<PostEditRecordDTO> deletes = new ArrayList<>();
        for(PostEditRecordDTO record : postEditRecords){
            if(record.postNum() == postNum){
                deletes.add(record);
            }
        }
        for(PostEditRecordDTO record : deletes) {
            postEditRecords.remove(record);
        }

        dataManager.writeData(postEditRecords);
    }
}
