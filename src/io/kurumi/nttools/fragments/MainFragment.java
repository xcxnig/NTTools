package io.kurumi.nttools.fragments;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONObject;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import io.kurumi.nttools.server.AuthCache;
import io.kurumi.nttools.twitter.ApiToken;
import io.kurumi.nttools.twitter.TwiAccount;
import io.kurumi.nttools.utils.CData;
import io.kurumi.nttools.utils.UserData;
import java.io.File;
import java.util.LinkedList;
import java.util.Map;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import com.pengrad.telegrambot.response.BaseResponse;

public class MainFragment extends Fragment {

    public int serverPort = -1;
    public String serverDomain;
    public File dataDir;

    public MainFragment(File dataDir) {

        super(null);
        main = this;
        this.dataDir = dataDir;

        refresh();

    }

    @Override
    public String name() { return "main"; }

    public Map<String,String> tokens;

    public void refresh() {

        try {

            JSONObject botData = new JSONObject(FileUtil.readUtf8String(new File(dataDir, "config.json")));

            tokens = (Map<String,String>)((Object)botData.getJSONObject("bot_token"));

            serverPort = botData.getInt("local_port", serverPort);
            serverDomain = botData.getStr("server_domain");


        } catch (Exception e) {}

    }

    public void save() {

        JSONObject botData = new JSONObject();

        botData.put("bot_token", new JSONObject(tokens));

        botData.put("local_port", serverPort);
        botData.put("server_domain", serverDomain);

        FileUtil.writeUtf8String(botData.toStringPretty(), new File(dataDir, "config.json"));

    }

    public static final String POINT_CHOOSE_USER = "c";
    public static final String POINT_BACK_TO_USERS = "b";
    public static final String POINT_DELETE_USER = "d";
    public static final String POINT_REFRESH_USER = "r";

    public static final String POINT_DELETE_STATUS = "s";

    public static final String POINT_DELETE_FRIENDS = "f";
    public static final String POINT_DELETE_FOLLOWRS = "o";


    @Override
    public void processCallbackQuery(UserData user, CallbackQuery callbackQuery) {

        CData data = new CData(callbackQuery.data());

        switch (data.getPoint()) {

                case POINT_CHOOSE_USER : chooseUser(user, callbackQuery, data);return;
                case POINT_BACK_TO_USERS : users(user, null, callbackQuery);return;
                case POINT_REFRESH_USER : refreshUser(user, callbackQuery, data);return;

        }

    }

    private void refreshUser(UserData user, CallbackQuery callbackQuery, CData data) {

        if (data.getUser(user).refresh()) {

            bot.execute(new AnswerCallbackQuery(callbackQuery.id()).text("刷新成功 ~"));
            
            chooseUser(user,callbackQuery,data);

        } else {
            
            bot.execute(new AnswerCallbackQuery(callbackQuery.id()).text("刷新失败 ~").showAlert(true));
            
        }

    }

    public void chooseUser(UserData user, CallbackQuery callbackQuery, CData data) {

        TwiAccount acc = data.getUser(user);

        String[] userMsg = new String[] {

            "推油 " + acc.getFormatedName(),
            "你好呀 这是一些小工具 *٩(๑´∀`๑)ง*"

        };

        LinkedList<InlineKeyboardButton[]> buttons = newButtonList();

        buttons.add(new InlineKeyboardButton[] {

                        new InlineKeyboardButton("返回用户列表").callbackData(cdata(POINT_BACK_TO_USERS).toString())

                    });

        buttons.add(new InlineKeyboardButton[] {

                        new InlineKeyboardButton("刷新账号").callbackData(cdata(POINT_REFRESH_USER, user, acc).toString()),
                        new InlineKeyboardButton("删除账号").callbackData(cdata(POINT_DELETE_USER, user, acc).toString())

                    });

        buttons.add(new InlineKeyboardButton[] {

                        new InlineKeyboardButton("清推").callbackData(cdata(POINT_DELETE_STATUS, user, acc).toString()),
                        new InlineKeyboardButton("清关注").callbackData(cdata(POINT_DELETE_FRIENDS, user, acc).toString()),
                        new InlineKeyboardButton("清关注者").callbackData(cdata(POINT_DELETE_FOLLOWRS, user, acc).toString())

                    });

        bot.execute(new EditMessageText(callbackQuery.message().chat().id(), callbackQuery.message().messageId(), ArrayUtil.join(userMsg, "\n")).replyMarkup(markup(buttons)));

    }

    @Override
    public void processPrivateMessage(UserData user, Message msg) {

        if (user.point == null) {

            if (isCommand(msg)) {

                String commandName = getCommandName(msg);

                switch (commandName) {

                        case "start" : case "help" :

                        help(user, msg); return;

                        case "auth" : 

                        auth(user, msg);return;

                        case "users" : 

                        users(user, msg, null);


                }

            }

        }

    }

    public void users(UserData user, Message msg , CallbackQuery query) {

        if (user.twitterAccounts.size() == 0) {

            bot.execute(new SendMessage(msg.chat().id(), "还没有认证账号啦！ (*σ´∀`)σ"));
            return;
            
        }

        String userMsg = "这是所有已经认证的账号 ~\n也可以用/auth添加新账号哦 ~";

        LinkedList<InlineKeyboardButton[]> buttons = newButtonList();

        for (TwiAccount acc : user.twitterAccounts) {

            buttons.add(new InlineKeyboardButton[] {

                            new InlineKeyboardButton(acc.name).callbackData(cdata(POINT_CHOOSE_USER, user, acc).toString())

                        });

        }

        InlineKeyboardMarkup markup = markup(buttons);

        if (query == null) {

            bot.execute(new SendMessage(msg.chat().id(), userMsg).replyMarkup(markup));

        } else {
            
            EditMessageText req = new EditMessageText(query.id(), userMsg).replyMarkup(markup);

            System.out.println(req.toWebhookResponse());
            
            BaseResponse resp = bot.execute(req);
            
            System.out.println(resp.isOk());
            System.out.println(resp.description());
            
            System.out.println(resp);
            
            
            
        }

    }

    private void auth(final UserData user, final Message msg) {

        bot.execute(new SendMessage(msg.chat().id(), "正在请求认证链接 (｡>∀<｡)"));

        try {

            final RequestToken requestToken = ApiToken.defaultToken.createApi().getOAuthRequestToken("https://" + main.serverDomain + "/callback");

            AuthCache.cache.put(requestToken.getToken(), new AuthCache.Listener() {

                    @Override
                    public void onAuth(String oauthVerifier) {

                        System.out.println("authing");
                        
                        try {

                            AccessToken accessToken =  ApiToken.defaultToken.createApi().getOAuthAccessToken(requestToken, oauthVerifier);

                            TwiAccount account = new TwiAccount(ApiToken.defaultToken.apiToken, ApiToken.defaultToken.apiSecToken, accessToken.getToken(), accessToken.getTokenSecret());

                            account.refresh();

                            user.twitterAccounts.add(account);

                            user.save();

                            bot.execute(new SendMessage(msg.chat().id(), account.getFormatedName() + " 认证成功  (*^▽^*)"));

                        } catch (Exception e) {

                            bot.execute(new SendMessage(msg.chat().id(), e.toString()));

                        }

                    }

                });

            bot.execute(new SendMessage(msg.chat().id(), "请求成功 ╰(*´︶`*)╯\n 点这里认证 : " + requestToken.getAuthenticationURL()));



        } catch (TwitterException e) {

            bot.execute(new SendMessage(msg.chat().id(), e.toString()));

        }

    }

    public void help(UserData user, Message msg) {

        String[] help = new String[] {

            "这里是奈间家的BOT (◦˙▽˙◦)","",

            "/auth 认证新的Twitter用户",
            "/users 一些小工具"

        };

        bot.execute(new SendMessage(msg.chat().id(), ArrayUtil.join(help, "\n")));

    }



}
