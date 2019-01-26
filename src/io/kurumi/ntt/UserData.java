package io.kurumi.ntt;

import cn.hutool.core.io.*;
import cn.hutool.json.*;
import com.pengrad.telegrambot.model.*;
import io.kurumi.ntt.serialize.*;
import io.kurumi.ntt.twitter.*;
import io.kurumi.ntt.ui.*;
import io.kurumi.ntt.ui.request.*;
import java.io.*;
import java.util.*;

import java.io.File;

public class UserData {

    public Data data;
    public long id;
    public File userDataFile;

    public Chat chat;

    public UserData(Data data, long id) {

        this.data = data;
        this.id = id;

        userDataFile = new File(data.dataDir, "users/" + id + ".json");

        refresh();

    }

    public boolean registered = false;
    public boolean isAdmin = false;
    public boolean isBanned = false;

    public String userName;
    public String name;
    public boolean isBot = false;

    public DataObject point;
    
    public void setPoint(String pointStr) {
        
        point = new DataObject();
 
        point.setPoint(pointStr);
        
    }
    
    public String getPoint() {

        if (point == null) return "";
        
        else return point.getPoint();

    }
    

    // public LinkedList<ApiToken> apiTokens = new LinkedList<>();
    public LinkedList<TwiAccount> twitterAccounts = new LinkedList<>();

    public void setName(String first, String last) {

        if (last != null && !"".equals(last)) {

            first =  last + " " + first;

        }

        name = first;

    }

    public void refresh() {

        try {

            JSONObject userData = new JSONObject(FileUtil.readUtf8String(userDataFile));

            registered = userData.getBool("registered", false);
            isAdmin = userData.getBool("is_admin", false);
            isBanned = userData.getBool("is_banned", false);
            userName = userData.getStr("user_name", "");
            name = userData.getStr("name", "");
            isBot = userData.getBool("is_bot", false);
            
            String pointStr = userData.getStr("point");
            
            if (pointStr != null) {
                
                point = new DataObject(pointStr);
                
            }

            chat = SerUtil.toObject(userData.getStr("char"));

            JSONArray twitterAccountList = userData.getJSONArray("twitter_accounts");

            twitterAccounts.clear();

            for (Object obj : twitterAccountList) {

                twitterAccounts.add(new TwiAccount((JSONObject)obj));

            }

            /*

             JSONArray apiTokenList = userData.getJSONArray("api_tokens");

             apiTokens.clear();

             for (Object obj : apiTokenList) {

             apiTokens.add(new ApiToken((JSONObject)obj));

             }

             */


        } catch (Exception e) {}

    }

    public JSONObject toJSONObject() {

        JSONObject userData = new JSONObject();

        userData.put("registered", registered);
        userData.put("is_admin", isAdmin);
        userData.put("is_banned", isBanned);

        userData.put("user_name", userName);
        userData.put("name", name);

        userData.put("is_bot", isBot);
        
        userData.put("point", point);

        userData.put("chat", SerUtil.toString(chat));



        /*

         JSONArray apiTokenList = new JSONArray();

         for (ApiToken apiToken : apiTokens) {

         apiTokenList.add(apiToken.toJSONObject());

         }

         */

        JSONArray twitterAccountList = new JSONArray();

        for (TwiAccount account : twitterAccounts) {

            twitterAccountList.add(account.toJsonObject());

        }

        //  userData.put("api_tokens", apiTokenList);
        userData.put("twitter_accounts", twitterAccountList);


        return userData;

    }

    public void save() {

        FileUtil.writeUtf8String(toJSONObject().toStringPretty(), userDataFile);

    }

    public void update(Message from) {

        update(from.from());
        chat = from.chat();

    }


    public void update(User from) {

        userName = from.username();
        setName(from.firstName() , from.lastName());
        isBot = from.isBot();

    }
    
    public TwiAccount find(String accountId) {
        
        return find(Long.parseLong(accountId));
        
    }

    public TwiAccount find(long accountId) {

        for (TwiAccount acc : twitterAccounts) {

            if (acc.accountId == accountId) {

                return acc;

            }

        }

        return null;

    }

    public void delete() {

        FileUtil.del(userDataFile);

    }

    public SendMsg send(String... msg) {
        
        return new SendMsg(chat,msg);
        
    }

}