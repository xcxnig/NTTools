package io.kurumi.ntt.funcs;

import io.kurumi.ntt.fragment.*;
import io.kurumi.ntt.db.*;
import io.kurumi.ntt.model.*;
import cn.hutool.core.util.*;
import cn.hutool.core.text.*;

public class ChineseAction extends Fragment {

	public static ChineseAction INSTANCE = new ChineseAction();
	
	boolean startWithChinese(String msg) {
		
		if (msg == null) return false;
		
		char first = msg.charAt(0);
		
		return first >= 0x4E00 &&  first <= 0x9FA5;
		
	}
	
	@Override
	public boolean onGroupMsg(UserData user,Msg msg,boolean superGroup) {
		
		if (msg.replyTo() != null && startWithChinese(msg.command())) {
			
			msg.send(user.userName() + " " + msg.command() + "了 " + msg.replyTo().from().userName() + " ！").html().exec();
			
			msg.delete();
			
			return true;
			
		}
		
		return false;
		
	}
	
}