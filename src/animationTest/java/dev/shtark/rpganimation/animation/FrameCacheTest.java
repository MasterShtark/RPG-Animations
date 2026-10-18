package dev.shtark.rpganimation.animation;
public final class FrameCacheTest {
    public static void run() {
        var cache = new FrameCache<Object,Integer>();
        Object a = new String("same"), b = new String("same");
        int[] calls={0}, state={0};
        java.util.function.Function<Object,Integer> read=key->{calls[0]++;return state[0];};
        cache.begin();
        for(int i=0;i<20;i++) check(cache.get(a,read)==0,"idle value");
        check(calls[0]==1,"one probe across render passes");
        state[0]=1; check(cache.get(b,read)==1,"identity isolates replacement player");
        check(cache.get(a,read)==0,"stable within frame");
        cache.end(); check(cache.retainedEntries()==0,"release player references");
        check(cache.get(a,read)==1,"tick outside frame is fresh");
        state[0]=2; check(cache.get(a,read)==2,"unknown state is fresh outside frame");
        cache.begin();check(cache.get(a,read)==2,"next frame observes changes");
        state[0]=0;cache.begin();check(cache.get(a,read)==0,"new frame clears even if previous post missed");cache.end();
        Object[] players=new Object[100];for(int i=0;i<100;i++)players[i]=new Object();
        calls[0]=0;
        for(int frame=0;frame<10;frame++) {
            cache.begin();
            for(var player:players)for(int pass=0;pass<20;pass++)cache.get(player,read);
            cache.end();
        }
        check(calls[0]==1000,"1000 probes for 20000 lookups across ten frames");
        System.out.println("Passed: frame cache identity, fresh ticks, attack transitions, reference cleanup; 20000 render lookups use 1000 probes");
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
