package dev.hoi.client.screen;

final class ScreenTradeAmounts {
    private ScreenTradeAmounts() {}
    static String convoyCell(Integer convoys) {return convoys==null||convoys<0?"—":convoys==0?"-":Integer.toString(convoys);}
    static int neededTradeFactories(double demand,double available,double delivered,double perFactory,int limit) {
        return Math.clamp((int)Math.ceil(Math.max(0,demand-available+delivered)/perFactory),0,limit);
    }
    static double tradeSliderPosition(int factories,double total,double perFactory) {
        return total<=0?0:Math.min(total,factories*perFactory)/total;
    }
    static int tradeSliderFactories(double position,double total,double perFactory) {
        double amount=Math.clamp(position,0,1)*total;
        int lower=(int)Math.floor(amount/perFactory);
        double next=Math.min(total,(lower+1)*perFactory);
        return amount-lower*perFactory<next-amount?lower:(int)Math.ceil(next/perFactory);
    }
}
