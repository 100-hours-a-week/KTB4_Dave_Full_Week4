package com.example.community.repository;

import com.example.community.component.DataManager;
import com.example.community.domain.user.UserLikePost;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserLikeJsonRepository implements UserLikeRepository{
    private final DataManager<UserLikePost> dataManager;

    public UserLikeJsonRepository(@Qualifier("userLikeDataManager") DataManager<UserLikePost> dataManager){
        this.dataManager = dataManager;
    }

    @Override
    public List<UserLikePost> getUserLikePosts(long userNum) {
        return dataManager.readData().stream().filter(ul -> ul.getUserNum() == userNum).toList();
    }

    @Override
    public boolean isUserLikePost(UserLikePost userLikePost) {
        for(UserLikePost ul : dataManager.readData()){
            if(ul.equals(userLikePost)){
                return true;
            }
        }

        return false;
    }

    @Override
    public Optional<UserLikePost> addUserLikePost(UserLikePost userLikePost) {
        List<UserLikePost> userLikePosts = dataManager.readData();
        userLikePosts.add(userLikePost);
        try {
            dataManager.writeData(userLikePosts);
        }catch (Exception e){
            e.printStackTrace();
            return Optional.empty();
        }
        return Optional.of(userLikePost);
    }

    @Override
    public void deleteUserLikePost(UserLikePost userLikePost) {
        List<UserLikePost> userLikePosts = dataManager.readData();
        userLikePosts.remove(userLikePost);

        dataManager.writeData(userLikePosts);
    }
}
