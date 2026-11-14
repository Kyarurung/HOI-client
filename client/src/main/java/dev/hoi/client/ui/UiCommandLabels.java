package dev.hoi.client.ui;

import dev.hoi.client.ui.tooltip.TooltipText;
import com.google.gson.JsonParser;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class UiCommandLabels {
    private static Map<String,String> labels;
    private UiCommandLabels() {}
    public static void clear(){labels=null;}
    public static String text(String key,String fallback) {
        if(labels==null) {
            var result=new HashMap<String,String>();var resource=Minecraft.getInstance().getResourceManager().getResource(Identifier.parse("hoi:command/labels.json"));
            if(resource.isPresent())try(var reader=resource.orElseThrow().openAsReader()){JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(e->result.put(e.getKey(),e.getValue().getAsString()));}catch(java.io.IOException|IllegalArgumentException ignored){}
            labels=Map.copyOf(result);
        }
        String text=labels.getOrDefault(key,fallback);
        return withoutShortcuts(key.equals("SEPARATION_LINE")?text:text.replaceAll("(?m)^-{3,}$",""));
    }
    public static String withoutShortcuts(String text) {
        if(text==null||text.isEmpty())return text;
        text=text.replace("이 버튼을 눌러서 모드를 전환하거나 §GCtrl§!을 길게 누르십시오.","이 버튼을 눌러서 모드를 전환하십시오.").replace("이 버튼을 눌러서 모드를 전환하거나 §GAlt§!를 길게 누르십시오.","이 버튼을 눌러서 모드를 전환하십시오.");
        var lines=new ArrayList<String>();boolean removedTail=false;
        for(String line:text.split("\\R",-1)) {
            String visible=line.replaceAll("§(?:#[0-9a-fA-F]{6}|.)","").strip();
            if(visible.matches("(?i)^(?:단축키|키 설정|shortcut|hotkey)\\s*[:：].*")||visible.startsWith("£ctrl + £b로 ")) {
                while(!lines.isEmpty()&&TooltipText.blank(lines.getLast()))lines.removeLast();
                removedTail=true;continue;
            }
            if(removedTail&&TooltipText.blank(line))continue;
            lines.add(line.replaceAll(" 또는 £(?:ctrl\\s*\\+\\s*£)?[a-zA-Z]+\\s*$",""));removedTail=false;
        }
        return String.join("\n",lines);
    }
}
