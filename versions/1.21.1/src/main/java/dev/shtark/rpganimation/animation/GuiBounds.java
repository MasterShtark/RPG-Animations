package dev.shtark.rpganimation.animation;

/** Pixel bounds in scaled GUI coordinates. */
public record GuiBounds(int x,int y,int width,int height) {
    public GuiBounds union(GuiBounds other) {
        if(other.width<=0||other.height<=0)return this;
        int left=Math.min(x,other.x),top=Math.min(y,other.y);
        return new GuiBounds(left,top,Math.max(x+width,other.x+other.width)-left,Math.max(y+height,other.y+other.height)-top);
    }
    public GuiBounds clipped(int screenWidth,int screenHeight) {
        int left=Math.clamp(x,0,screenWidth),top=Math.clamp(y,0,screenHeight);
        int right=Math.clamp(x+width,left,screenWidth),bottom=Math.clamp(y+height,top,screenHeight);
        return new GuiBounds(left,top,right-left,bottom-top);
    }
}
