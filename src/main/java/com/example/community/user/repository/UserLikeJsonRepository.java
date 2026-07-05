package com.example.community.user.repository;

import com.example.community.util.DataManager;
import com.example.community.user.dto.UserLikePostDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class UserLikeJsonRepository implements UserLikeRepository{
    private final DataManager<UserLikePostDTO> dataManager;

    public UserLikeJsonRepository(@Qualifier("userLikeDataManager") DataManager<UserLikePostDTO> dataManager){
        this.dataManager = dataManager;
    }

    @Override
    public Page<UserLikePostDTO> getUserLikePosts(long userNum, Pageable pageable) {
        List<UserLikePostDTO> userLikePostDTOS = dataManager.readData().stream().filter(ul -> ul.getProfileId() == userNum).toList();
        List<UserLikePostDTO> result = new ArrayList<>();
        int start = Math.toIntExact(pageable.getOffset());
        int end = start + pageable.getPageSize();
        for(int i = start; i < end; i++){
            result.add(userLikePostDTOS.get(i));
        }

        return new PageImpl<>(result, pageable, userLikePostDTOS.size());
    }

    @Override
    public boolean isUserLikePost(UserLikePostDTO userLikePost) {
        for(UserLikePostDTO ul : dataManager.readData()){
            if(ul.equals(userLikePost)){
                return true;
            }
        }

        return false;
    }

    @Override
    public void addUserLikePost(UserLikePostDTO userLikePost) {
        List<UserLikePostDTO> userLikePosts = dataManager.readData();
        userLikePosts.add(userLikePost);
        dataManager.writeData(userLikePosts);
    }

    @Override
    public void deleteUserLikePost(UserLikePostDTO userLikePost) {
        List<UserLikePostDTO> userLikePosts = dataManager.readData();
        userLikePosts.remove(userLikePost);

        dataManager.writeData(userLikePosts);
    }
}
