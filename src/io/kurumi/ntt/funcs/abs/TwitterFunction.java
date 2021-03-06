package io.kurumi.ntt.funcs.abs;

import com.mongodb.client.*;
import io.kurumi.ntt.db.*;
import io.kurumi.ntt.funcs.twitter.login.*;
import io.kurumi.ntt.model.*;
import io.kurumi.ntt.model.request.*;
import io.kurumi.ntt.twitter.*;
import io.kurumi.ntt.twitter.archive.*;

public abstract class TwitterFunction extends Function {

    public static final String POINT_CHOOSE_ACCPUNT = "t|s";

    public static class TwitterPoint {

        public TwitterFunction function;
        public Msg msg;

        public TwitterPoint(TwitterFunction function,Msg msg) {

            this.function = function;
            this.msg = msg;

        }

    }

    public abstract void onFunction(UserData user,Msg msg,String function,String[] params,TAuth account);

    @Override
    public void onFunction(UserData user,Msg msg,String function,String[] params) {

        if (!TAuth.contains(user.id)) {

            msg.send("这个功能需要授权 Twitter账号 才能使用 (❁´▽`❁)","使用 /login 认证账号 ~").exec();

            return;

        } else {

            if (TAuth.data.countByField("user",user.id) == 1) {

                onFunction(user,msg,function,msg.params(),TAuth.getByUser(user.id).first());

                return;

            }

            final FindIterable<TAuth> accounts = TAuth.getByUser(user.id);

            setPoint(user,POINT_CHOOSE_ACCPUNT,new TwitterPoint(this,msg));

            msg.send("请选择目标账号 Σ( ﾟωﾟ (使用 /cancel 取消) ~").keyboard(new Keyboard() {{

                        for (TAuth account : accounts) {

                            newButtonLine("@" + account.archive().screenName);

                        }

                        newButtonLine("/cancel");

                    }}).exec();

        }

    }

    @Override
    public void onPoint(UserData user,Msg msg,PointStore.Point point) {

        if (point.point == POINT_CHOOSE_ACCPUNT) {

            TwitterPoint data = (TwitterPoint)point.data;

            if (!msg.text().startsWith("@")) {

                msg.send("请选择 Twitter 账号 (˚☐˚! )/").exec();

                return;

            }

            String screenName = msg.text().substring(1);

            TAuth account = TAuth.getById(UserArchive.get(screenName).id);

            if (account == null) {

                msg.send("找不到这个账号 (？) 请重新选择 ((*゜Д゜)ゞ").exec();

                return;

            }

            data.function.onFunction(user,data.msg,data.msg.command(),data.msg.params(),account);
            

        }

    }



}
