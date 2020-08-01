package edu.cmu.hcii.sugilite;

import android.graphics.drawable.Drawable;

public class EachInstance {
    private Drawable UIDrawable;
    private String semantictag;

    public void setIcon(Drawable icon) {
        UIDrawable = icon ;
    }
    public void setSemantictag(String tag) {
        semantictag = tag ;
    }
    public Drawable getIcon() {
        return this.UIDrawable ;
    }
    public String getSemantictag() {
        return this.semantictag;
    }
}
