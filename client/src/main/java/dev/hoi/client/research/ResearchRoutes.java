package dev.hoi.client.research;

import java.util.*;
import dev.hoi.client.research.ResearchLayout.Node;
import dev.hoi.client.research.TfrResearchLayout.Segment;


final class ResearchRoutes {
    private record Point(int x, int y) {}
    private record Pending(int index, int distance, int estimate) {}

    static boolean blocked(Segment s, List<Node> nodes) {
        for (var n : nodes) {
            boolean cuts = s.x1() == s.x2()
                    ? s.x1() > n.x() && s.x1() < n.x()+n.width() && Math.max(s.y1(),s.y2()) > n.y() && Math.min(s.y1(),s.y2()) < n.y()+n.height()
                    : s.y1() > n.y() && s.y1() < n.y()+n.height() && Math.max(s.x1(),s.x2()) > n.x() && Math.min(s.x1(),s.x2()) < n.x()+n.width();
            if (cuts) return true;
        }
        return false;
    }

    private static List<Point> ports(Node n) {
        int cx=n.x()+n.width()/2, cy=n.y()+n.height()/2;
        return List.of(new Point(cx,n.y()),new Point(cx,n.y()+n.height()),new Point(n.x(),cy),new Point(n.x()+n.width(),cy));
    }

    static List<Segment> around(Node from, Node to, ResearchLayout layout) {
        var starts=ports(from); var goals=ports(to);
        var xx=new TreeSet<Integer>(); var yy=new TreeSet<Integer>();
        int margin=(int)Math.ceil(4*TfrResearchLayout.scale(layout));
        for (var n:layout.nodes()) {
            xx.add(n.x()-margin); xx.add(n.x()+n.width()+margin); xx.add(n.x()+n.width()/2);
            yy.add(n.y()-margin); yy.add(n.y()+n.height()+margin); yy.add(n.y()+n.height()/2);
        }
        for (var p:starts) {xx.add(p.x());yy.add(p.y());}
        for (var p:goals) {xx.add(p.x());yy.add(p.y());}
        int[] xs=xx.stream().mapToInt(Integer::intValue).toArray(), ys=yy.stream().mapToInt(Integer::intValue).toArray();
        int columns=xs.length, size=columns*ys.length;
        int[] cost=new int[size], previous=new int[size]; Arrays.fill(cost,Integer.MAX_VALUE); Arrays.fill(previous,-1);
        var targets=new HashSet<Integer>();
        for (var p:goals) targets.add(Arrays.binarySearch(ys,p.y())*columns+Arrays.binarySearch(xs,p.x()));
        var queue=new PriorityQueue<Pending>(Comparator.comparingInt(Pending::estimate).thenComparingInt(Pending::index));
        for (var p:starts) {
            int i=Arrays.binarySearch(ys,p.y())*columns+Arrays.binarySearch(xs,p.x()); cost[i]=0;
            queue.add(new Pending(i,0,estimate(p.x(),p.y(),goals)));
        }
        int reached=-1;
        while (!queue.isEmpty()) {
            var current=queue.remove(); int i=current.index();
            if (cost[i]!=current.distance()) continue;
            if (targets.contains(i)) {reached=i;break;}
            int x=i%columns,y=i/columns;
            for (int direction=0;direction<4;direction++) {
                int nx=x+(direction==0?-1:direction==1?1:0), ny=y+(direction==2?-1:direction==3?1:0);
                if (nx<0||nx>=columns||ny<0||ny>=ys.length) continue;
                int j=ny*columns+nx, distance=cost[i]+Math.abs(xs[nx]-xs[x])+Math.abs(ys[ny]-ys[y]);
                if (distance>=cost[j]||blocked(new Segment(xs[x],ys[y],xs[nx],ys[ny]),layout.nodes())) continue;
                cost[j]=distance;previous[j]=i;
                queue.add(new Pending(j,distance,distance+estimate(xs[nx],ys[ny],goals)));
            }
        }
        if (reached<0) throw new IllegalStateException("No research connector: "+from.tech().id()+" -> "+to.tech().id());
        var path=new ArrayList<Point>();
        for (int i=reached;i>=0;i=previous[i]) path.add(new Point(xs[i%columns],ys[i/columns]));
        Collections.reverse(path);
        var result=new ArrayList<Segment>(); Point start=path.getFirst(), end=start;
        for (int i=1;i<path.size();i++) {
            var next=path.get(i);
            if (i>1 && !((start.x()==end.x()&&end.x()==next.x())||(start.y()==end.y()&&end.y()==next.y()))) {
                result.add(new Segment(start.x(),start.y(),end.x(),end.y())); start=end;
            }
            end=next;
        }
        if (!start.equals(end)) result.add(new Segment(start.x(),start.y(),end.x(),end.y()));
        return List.copyOf(result);
    }
    private static int estimate(int x,int y,List<Point> goals) {
        return goals.stream().mapToInt(p->Math.abs(x-p.x())+Math.abs(y-p.y())).min().orElseThrow();
    }
    private ResearchRoutes() {}
}
