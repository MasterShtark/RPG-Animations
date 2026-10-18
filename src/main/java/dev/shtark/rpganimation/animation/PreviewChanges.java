package dev.shtark.rpganimation.animation;

import java.util.Arrays;

/** Content comparison is exact; revision leases let the server request recovery after a dropped upload. */
public final class PreviewChanges {
    private byte[] previous;
    private int width,height,revision;
    public boolean update(int width,int height,byte[] pixels) {
        if(this.width==width&&this.height==height&&Arrays.equals(previous,pixels))return false;
        this.width=width;this.height=height;previous=pixels.clone();
        revision=revision==Integer.MAX_VALUE?0:revision+1;return true;
    }
    public int revision(){return revision;}
    public void invalidate(){previous=null;}
}
