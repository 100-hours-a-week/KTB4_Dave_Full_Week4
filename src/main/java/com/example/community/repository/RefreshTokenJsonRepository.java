package com.example.community.repository;

import com.example.community.domain.token.TokenDTO;
import com.example.community.util.DataManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RefreshTokenJsonRepository implements RefreshTokenRepository{
    private final DataManager<TokenDTO> dataManager;

    public RefreshTokenJsonRepository(@Qualifier("refreshTokenDataManager") DataManager<TokenDTO> dataManager){
        this.dataManager = dataManager;
    }

    @Override
    public void addRefreshToken(TokenDTO token) {
        List<TokenDTO> tokens = dataManager.readData();
        tokens.add(token);

        dataManager.writeData(tokens);
    }

    @Override
    public boolean checkRefreshToken(TokenDTO token) {
        for(TokenDTO t : dataManager.readData()){
            if(t.equals(token)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void updateRefreshToken(TokenDTO token) {
        List<TokenDTO> tokens = dataManager.readData();
        for(TokenDTO t : tokens){
            if(t.userNum() == token.userNum()){
                t = token;
                dataManager.writeData(tokens);
                return;
            }
        }

    }

    @Override
    public void deleteRefreshToken(String token) {
        List<TokenDTO> tokens = dataManager.readData();
        TokenDTO delete = null;

        for(TokenDTO t : tokens){
            if(t.jwtToken().equals(token)){
                delete = t;
                break;
            }
        }
        if(delete != null){
            tokens.remove(delete);
            dataManager.writeData(tokens);
        }


    }

    @Override
    public void deleteRefreshToken(long userNum) {
        List<TokenDTO> tokens = dataManager.readData();
        TokenDTO delete = null;

        for(TokenDTO t : tokens){
            if(t.userNum() == userNum){
                delete = t;
                break;
            }
        }
        if(delete != null){
            tokens.remove(delete);
            dataManager.writeData(tokens);
        }
    }
}
