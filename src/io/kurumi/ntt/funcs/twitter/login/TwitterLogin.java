package io.kurumi.ntt.funcs.twitter.login;

import cn.hutool.core.util.*;
import io.kurumi.ntt.*;
import io.kurumi.ntt.db.*;
import io.kurumi.ntt.funcs.abs.*;
import io.kurumi.ntt.model.*;
import io.kurumi.ntt.model.request.*;
import io.kurumi.ntt.twitter.*;
import io.kurumi.ntt.utils.*;
import java.util.*;
import twitter4j.*;
import twitter4j.auth.*;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.not;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;
import static java.util.Arrays.asList;

public class TwitterLogin extends Function {

    public static TwitterLogin INSTANCE = new TwitterLogin();

    @Override
    public void functions(LinkedList<String> names) {
        
        names.add("login");
        
    }

    final String POINT_INPUT_CALLBACK = "t|l";

    @Override
    public void points(LinkedList<String> points) {
        
        points.add(POINT_INPUT_CALLBACK);
        
    }

    @Override
    public int target() {
       
        return Private;
        
        
    }
    
    

    public HashMap<Long,RequestToken> cache = new HashMap<>();

    @Override
    public void onFunction(UserData user,Msg msg,String function,String[] params) {

        try {

            RequestToken request = ApiToken.defaultToken.createApi().getOAuthRequestToken("oob");

            cache.put(user.id,request);

            msg.send("点 " + Html.a("这里",request.getAuthorizationURL()) + " 认证 ~").html().exec();

            // msg.send("因为咱是一个简单的程序 所以不能自动收到认证！ T_T ","","请记住 : 认证账号之后会跳转到一个不可访问的界面 : 在浏览器显示的地址是 127.0.0.1 , 这时候不要关闭浏览器！复制这个链接并发送给咱就可以了 ~","","如果不小心关闭了浏览器 请使用 /cancel 取消认证并重新请求认证 ^_^").exec();

            msg.send("(｡•̀ᴗ-)✧ 请输入 pin 码 : (使用 /cancel 取消认证 ) ~").exec();

            setPoint(user,POINT_INPUT_CALLBACK);

            // 不需要保存Point 因为request token的cache也不会保存。

        } catch (TwitterException e) {

            msg.send(e.toString()).exec();

            msg.send("请求认证链接失败 :( ","这可能是因为同时请求的人太多，或者有人不停重复请求... 也有可能是咱Twitter账号被停用了。 ","","那么，请再来一次吧 ~").exec();

        }


    }

    @Override
    public void onPoint(UserData user,Msg msg,PointStore.Point point) {

        if (!msg.hasText() || msg.text().length() != 7 || !NumberUtil.isNumber(msg.text())) {

            msg.reply("乃好像需要输入认证的 7位 PIN码 ~ 使用 /cancel 取消 :)").exec();

            return;

        }

        /*

         URL url = URLUtil.url(msg.text());

         if (url == null) {

         msg.send("乃好像忘了之前使用了 /login ！现在请发送跳转到的地址 ( ˶‾᷄࿀‾᷅˵ ) 如果不小心关掉了浏览器那就取消认证并再来一次吧 ( ⚆ _ ⚆ ) ","","取消使用 /cancel ").exec();

         return;

         }

         HashMap<String, String> params = HttpUtil.decodeParamMap(msg.text(),CharsetUtil.UTF_8);

         */


        RequestToken requestToken = cache.get(user.id);

        if (requestToken == null) {

            clearPoint(user);
            
            msg.send("缓存丢失 请重新认证 :(").exec();

        } else {

            try {

                AccessToken access = ApiToken.defaultToken.createApi().getOAuthAccessToken(requestToken,msg.text());
                
                long accountId = access.getUserId();
                
                TAuth old = TAuth.getById(accountId);

                if (old != null) {
                    
                    if (!user.id.equals(old.user)) {
                        
                        new Send(old.user,"乃的账号 " + old.archive().urlHtml() + " 已被 " + user.userName() + " 认证（●＾o＾●").html().exec();
                       
                    }
                    
                }
                
                TAuth auth = new TAuth();
                
                auth.apiKey = ApiToken.defaultToken.apiToken;
                auth.apiKeySec = ApiToken.defaultToken.apiSecToken;
                
                auth.id = accountId;
                auth.user = user.id;
                auth.accToken = access.getToken();
                auth.accTokenSec = access.getTokenSecret();
                
                clearPoint(user);

                TAuth.data.setById(accountId,auth);
           
                msg.send("好！现在认证成功 , " + auth.archive().urlHtml()).html().exec();

                new Send(this,Env.DEVELOPER_ID,user.userName() + " 认证了 " + auth.archive().urlHtml()).html().exec();

                cache.remove(user.id);

            } catch (TwitterException e) {

                msg.send("链接好像失效了...","请重新认证 ( /login ) (｡>∀<｡)").exec();

            }
            

        }
    }



}
