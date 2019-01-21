package io.kurumi.ntbot.md;

import org.commonmark.renderer.html.*;
import org.commonmark.parser.*;

public class Markdown {
    
    private static HtmlRenderer renderer = HtmlRenderer.builder().build();
    private static Parser parser = Parser.builder().build();
    
    public static String toHtml(String content) {
        
       return renderer.render(parser.parse(content));
        
    }
    
    public static String[] encode(String[] content) {
        
        for (int index = 0;index < content.length;index ++) {
            
            content[index] = encode(content[index]);
            
        }
        
        return content;
        
    }
    
    public static String encode(String content) {
        
        return content.replace("_","\\_");
        
    }
    
}