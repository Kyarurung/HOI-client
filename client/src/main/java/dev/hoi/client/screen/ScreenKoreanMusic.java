package dev.hoi.client.screen;

import dev.hoi.client.audio.AudioKoreanMusic;
import dev.hoi.client.audio.AudioPlaylistPlayback;
import dev.hoi.client.audio.AudioStoryMusic;
import dev.hoi.client.ui.*;
import dev.hoi.client.screen.dialog.DialogStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Locale;
import java.util.List;

public final class ScreenKoreanMusic extends Screen {
    private final Screen parent;
    private int x,y,w,h,offset,visible,left,top,rowHeight;
    private List<AudioPlaylistPlayback.AudioTrack> tracks;
    public ScreenKoreanMusic(Screen parent){super(Component.literal("TFR Korea Plus 음악"));this.parent=parent instanceof DialogStack stack?stack.backdrop():parent;}
    @Override protected void init(){
        w=Math.min(584,width-12);h=Math.min(444,height-12);
        x=(width-w)/2;y=(height-h)/2;
        left=Math.max(78,w*184/780);top=(w-left)*121/594;rowHeight=18;
        visible=Math.max(1,(h-top-12)/rowHeight);
        var p=AudioKoreanMusic.PLAYBACK;tracks=p.tracks();
        offset=Math.clamp(offset,0,Math.max(0,tracks.size()-visible));
        addRenderableWidget(new UiPanelButton("×",x+w-19,y+3,16,16,this::onClose));
        var station=new UiPanelButton("TFR Korea Plus ON/OFF",x+7,y+top+6,left-14,(left-14)*130/162,AudioKoreanMusic::toggleStation){
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta){
                UiAssets.draw(g,AudioKoreanMusic.stationEnabled()?"music/station_on":"music/station",getX(),getY(),getWidth(),getHeight());
            }
        };
        station.active=!tracks.isEmpty();
        station.setTooltip(UiTooltips.create(Component.literal("클릭하여 음악 켜기/끄기")));
        addRenderableWidget(station);
        int cx=x+left+(w-left)/2,cy=y+top*54/100;
        addRenderableWidget(new net.minecraft.client.gui.components.AbstractSliderButton(x+left+(w-left)*260/594,y+top*37/121,(w-left)*110/594,Math.max(12,top*20/121),Component.literal("음량"),AudioKoreanMusic.volume()){
            @Override protected void updateMessage(){setMessage(Component.literal("음량 "+Math.round(value*100)+"%"));setTooltip(UiTooltips.create(getMessage()));}
            @Override protected void applyValue(){AudioKoreanMusic.volume(value);}
            @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) {}
            @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
                g.fill(getX(),getY(),getRight(),getBottom(),0xFF151719);
                UiAssets.nineSlice(g,"music/volume_bg",getX(),getY(),getWidth(),getHeight(),2,1);
                g.enableScissor(getX(),getY(),getX()+(int)(getWidth()*value),getY()+getHeight());
                UiAssets.nineSlice(g,"music/volume",getX(),getY(),getWidth(),getHeight(),2,1);g.disableScissor();
                UiAssets.draw(g,"music/volume_handle",getX()+(int)((getWidth()-8)*value),getY(),8,getHeight());
            }
        });
        control("이전 곡","previous",cx-52,cy,20,20,()->playStep(-1));
        control("재생","play",cx-27,cy,34,20,AudioKoreanMusic::togglePause);
        control("정지","stop",cx+12,cy,20,20,AudioKoreanMusic::stop);
        control("다음 곡","next",cx+37,cy,20,20,()->playStep(1));
        for(int i=offset;i<Math.min(tracks.size(),offset+visible);i++){
            int index=i;var track=tracks.get(i);
            var button=new UiPanelButton(track.title(),x+left+5,y+top+5+(i-offset)*rowHeight,w-left-14,rowHeight,()->AudioKoreanMusic.play(index)){
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta){
                    UiAssets.nineSlice(g,"music/row",getX(),getY(),getWidth(),getHeight(),2,1);
                    if(index==AudioKoreanMusic.PLAYBACK.selected()&&AudioKoreanMusic.PLAYBACK.requested())g.fill(getX(),getY()+2,getRight(),getBottom()-2,0x403E842E);
                    if(isHovered())g.fill(getX(),getY()+1,getRight(),getBottom()-1,0x20585858);
                    g.text(font,font.plainSubstrByWidth(track.title(),getWidth()-60),getX()+5,getY()+(getHeight()-font.lineHeight)/2+1,0xFFD7D7D7);
                    if(index==AudioKoreanMusic.PLAYBACK.selected()&&AudioKoreanMusic.isPlaying())UiAssets.draw(g,"music/selected",getRight()-52,getY()+4,10,10);
                    long seconds=(long)track.seconds();
                    g.text(font,String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60),getRight()-34,getY()+(getHeight()-font.lineHeight)/2+1,0xFFBBBBBB);
                }
            };
            button.setTooltip(UiTooltips.create(Component.literal(track.title())));addRenderableWidget(button);
        }
    }
    private void playStep(int direction){var p=AudioKoreanMusic.PLAYBACK;if(!tracks.isEmpty())AudioKoreanMusic.play(Math.floorMod(p.selected()+direction,tracks.size()));}
    private void control(String label,String image,int px,int py,int width,int height,Runnable action){
        var button=new UiPanelButton(label,px,py,width,height,action){
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta){
                boolean pause=image.equals("play")&&!AudioStoryMusic.active()&&AudioKoreanMusic.PLAYBACK.requested()&&!AudioKoreanMusic.PLAYBACK.paused();
                String title=pause?"일시정지":label;
                if(!getMessage().getString().equals(title)){setMessage(Component.literal(title));setTooltip(UiTooltips.create(getMessage()));}
                UiAssets.draw(g,"music/"+(pause?"pause":image),getX(),getY(),getWidth(),getHeight());
            }
        };
        button.active=!tracks.isEmpty();button.setTooltip(UiTooltips.create(Component.literal(label)));addRenderableWidget(button);
    }
    @Override public void tick(){if(tracks!=AudioKoreanMusic.PLAYBACK.tracks())rebuildWidgets();}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){
        if(mx>=x+left&&mx<x+w&&my>=y+top&&my<y+h){
            offset=Math.clamp(offset-(int)Math.signum(vertical)*3,0,Math.max(0,tracks.size()-visible));rebuildWidgets();return true;
        }
        return super.mouseScrolled(mx,my,horizontal,vertical);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        UiMenuStyle.panel(g,x,y,w,h);
        UiAssets.draw(g,"music/header",x+left,y,w-left,top);
        int logoHeight=Math.min(50,top-18),logoWidth=logoHeight*34/39;
        int logoTop=y+(top-logoHeight-4-font.lineHeight)/2;
        UiAssets.draw(g,"music/logo",x+(left-logoWidth)/2,logoTop,logoWidth,logoHeight);
        g.centeredText(font,"기지",x+left/2,logoTop+logoHeight+4,0xFFD7D7D7);
        g.outline(x+3,y+top,left-6,h-top-3,0xFF696970);
        g.outline(x+left,y+top,w-left-3,h-top-3,0xFF696970);
        int stationHeight=(left-14)*130/162;
        var p=AudioKoreanMusic.PLAYBACK;
        if(!tracks.isEmpty()){
            var track=tracks.get(p.selected());
            if(!track.cover().isEmpty())UiAssets.draw(g,track.cover(),x+8,y+top+stationHeight+16,left-16,Math.min(left-16,h-top-stationHeight-28));
            g.centeredText(font,font.plainSubstrByWidth(track.title(),w-left-65),x+left+(w-left)/2,y+top*24/121-font.lineHeight/2,0xFFCFD0B6);
        }else g.text(font,"음악 리소스팩이 필요합니다.",x+left+8,y+top+12,UiMenuStyle.MUTED);
        if(tracks.size()>visible){
            int trackHeight=h-top-10,thumb=Math.max(10,trackHeight*visible/tracks.size());
            int thumbY=y+top+5+(trackHeight-thumb)*offset/(tracks.size()-visible);
            g.fill(x+w-7,y+top+5,x+w-4,y+h-5,0xFF131417);g.fill(x+w-7,thumbY,x+w-4,thumbY+thumb,0xFF70716B);
        }
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
}
