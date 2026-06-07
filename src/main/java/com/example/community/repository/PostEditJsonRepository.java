package com.example.community.repository;

import com.example.community.domain.post.PostEditRecord;
import com.example.community.util.DataManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class PostEditJsonRepository implements PostEditRepository{
    private final DataManager<PostEditRecord> dataManager;

    public PostEditJsonRepository(@Qualifier("postEditDataManager")DataManager<PostEditRecord> dataManager){
        this.dataManager = dataManager;
    }

    @Override
    public void addPostEditRecord(PostEditRecord postEditRecord) {
        List<PostEditRecord> postEditRecords = dataManager.readData();
        postEditRecords.add(postEditRecord);

        dataManager.writeData(postEditRecords);
    }

    @Override
    public List<PostEditRecord> getPostEditRecordByPostNum(long postNum) {

        return dataManager.readData().stream().filter(er -> er.postNum() == postNum).toList();
    }

    @Override
    public void deletePostEditRecord(long postNum) {
        List<PostEditRecord> postEditRecords = dataManager.readData();
        List<PostEditRecord> deletes = new ArrayList<>();
        for(PostEditRecord record : postEditRecords){
            if(record.postNum() == postNum){
                deletes.add(record);
            }
        }
        for(PostEditRecord record : deletes) {
            postEditRecords.remove(record);
        }

        dataManager.writeData(postEditRecords);
    }
}
