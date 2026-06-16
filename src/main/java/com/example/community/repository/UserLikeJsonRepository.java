package com.example.community.repository;

import com.example.community.util.DataManager;
import com.example.community.domain.user.UserLikePostDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserLikeJsonRepository implements UserLikeRepository{
    private final DataManager<UserLikePostDTO> dataManager;

    public UserLikeJsonRepository(@Qualifier("userLikeDataManager") DataManager<UserLikePostDTO> dataManager){
        this.dataManager = dataManager;
    }

    @Override
    public List<UserLikePostDTO> getUserLikePosts(long userNum) {
        return dataManager.readData().stream().filter(ul -> ul.getUserNum() == userNum).toList();
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
    public UserLikePostDTO addUserLikePost(UserLikePostDTO userLikePost) {
        List<UserLikePostDTO> userLikePosts = dataManager.readData();
        userLikePosts.add(userLikePost);
        dataManager.writeData(userLikePosts);
        return userLikePost;
    }

    @Override
    public void deleteUserLikePost(UserLikePostDTO userLikePost) {
        List<UserLikePostDTO> userLikePosts = dataManager.readData();
        userLikePosts.remove(userLikePost);

        dataManager.writeData(userLikePosts);
    }
}
