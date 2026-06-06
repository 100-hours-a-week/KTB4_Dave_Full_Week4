package com.example.community.repository;

import com.example.community.util.DataManager;
import com.example.community.domain.token.Token;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;

public class RefreshTokenJsonRepository implements RefreshTokenRepository{
    private final DataManager<Token> dataManager;

    public RefreshTokenJsonRepository(@Qualifier("refreshTokenDataManager") DataManager<Token> dataManager){
        this.dataManager = dataManager;
    }

    @Override
    public void addRefreshToken(Token token) {
        List<Token> tokens = dataManager.readData();
        tokens.add(token);

        dataManager.writeData(tokens);
    }

    @Override
    public boolean checkRefreshToken(Token token) {
        for(Token t : dataManager.readData()){
            if(t.equals(token)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void deleteRefreshToken(Token token) {
        List<Token> tokens = dataManager.readData();
        tokens.remove(token);

        dataManager.writeData(tokens);
    }

    @Override
    public void deleteRefreshToken(long userNum) {
        List<Token> tokens = dataManager.readData();
        Token delete = null;

        for(Token t : tokens){
            if(t.userNum() == userNum){
                delete = t;
                break;
            }
        }
        if(delete != null){
            tokens.remove(delete);
        }

        dataManager.writeData(tokens);
    }
}
