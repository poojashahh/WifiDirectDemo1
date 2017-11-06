package com.example.android.wifidirect;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by yudizsolutions on 11/04/16.
 */
public class MyPrefs {

    SharedPreferences myPrefs;
    SharedPreferences.Editor prefEditor;
    Context context;
    Boolean isPreview;
    private static MyPrefs mPrefs;
    public static MyPrefs getInstance(Context context) {
        if (mPrefs == null)
            mPrefs = new MyPrefs(context);
        return mPrefs;
    }
    private MyPrefs(Context context) {
        this.context = context;
        myPrefs = context.getSharedPreferences("Vozsays", 0);
        prefEditor = myPrefs.edit();
    }
    public void setPreCallBack(boolean isPreview) {
        prefEditor.putBoolean("isPreview", isPreview);
        prefEditor.commit();
    }
    public boolean isPreview() {
        return myPrefs.getBoolean("isPreview", false);
    }

}
