package io.kurumi.ntbot.ui.entries;

import com.pengrad.telegrambot.model.*;
import io.kurumi.ntbot.*;
import io.kurumi.ntbot.ui.ext.*;
import io.kurumi.ntbot.md.*;

public class TopLevel {

    public static final String COMMAND_START = "start";

    public static void processTopLevel(UserData userData, Message msg) {

        if (MsgExt.isCommand(msg)) {

            switch (MsgExt.getCommandName(msg)) {

                case COMMAND_START : start(userData, msg);break;
                case MainUI.COMMAND : MainUI.main(userData, msg);break;
                case AccountUI.COMMAND : AccountUI.main(userData, msg);break;

            }

        } else {

            //  repeat(userData, msg);

            new MsgExt.Send(msg.chat(), "嘤").send();

        }

    }

    public static void start(UserData userData, Message msg) {


        String[] startMessage = Markdown.encode(new String[] {

            userData.name + " 你好呀！ 这里是NTTBot ！ (不是复读机！)","",

            "Bot的源码在这里 ！ [NTTools](https://github.com/HiedaNaKan/NTTools)",
            "可以的话能给个star吗 (◦˙▽˙◦)","",

            "咱的推特在 [NTTBot](https://twitter.com/NTToolsBot)",
            "这里！欢迎关注哦 ⊙∀⊙ 咱会在十分钟之内回关呢 ←_←","",

            "现在 用 /main 来打开主菜单哦 >_<" 

        });

        new MsgExt.Send(msg.chat(), startMessage).markdown().disableWebPagePreview().send();

    }

    public static void repeat(UserData userData, Message msg) {

        new MsgExt.Send(msg, msg.text()).send();

    }


}