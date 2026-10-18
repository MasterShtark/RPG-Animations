package dev.shtark.rpganimation.animation;

/** Fixed-size RGB565 wire format avoids image decompression and allocation bombs. */
public final class InventoryPreview {
    public static final int MAX_WIDTH=128,MAX_HEIGHT=96,MAX_BYTES=MAX_WIDTH*MAX_HEIGHT*2;
    public static boolean valid(int width,int height,int bytes) {
        return width==-1&&height==0&&bytes==0 || width==0&&height==0&&bytes==0 || width>0&&height>0&&width<=MAX_WIDTH&&height<=MAX_HEIGHT&&bytes==width*height*2;
    }
    public static int encode(int abgr) {
        int r=abgr&255,g=(abgr>>>8)&255,b=(abgr>>>16)&255;
        return (r>>3)<<11|(g>>2)<<5|(b>>3);
    }
    public static int decode(int rgb) {
        int r=(rgb>>>11)&31,g=(rgb>>>5)&63,b=rgb&31;
        return 0xff000000|((b*255/31)<<16)|((g*255/63)<<8)|(r*255/31);
    }
    private InventoryPreview() {}
}
