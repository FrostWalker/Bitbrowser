package io.bitcloud.bitbrowser;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;

class InterfaceWindow {

    private Activity activity;
    private View location;
    private View layout;
    private PopupWindow window;

    InterfaceWindow(Activity activity, View location, int layoutResource, int layoutParams, int animationResource) {
        this.activity = activity;
        this.location = location;

        this.window = new PopupWindow(activity);
        this.setLayout(layoutResource);
        this.window.setWidth(layoutParams);
        this.window.setHeight(layoutParams);
        this.window.setFocusable(true);
        this.window.setBackgroundDrawable(null);
        this.window.setAnimationStyle(animationResource);
    }

    private void setLayout(int layoutResource) {
        LayoutInflater layoutInflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.layout = layoutInflater.inflate(layoutResource, null);
        this.window.setContentView(this.layout);
    }

    boolean isShowing() {
        return this.window.isShowing();
    }

    View findViewById(int id) {
        return this.layout.findViewById(id);
    }

    void show() {
        this.show(0, 0);
    }

    private void show(int x, int y) {
        this.window.showAtLocation(this.location, Gravity.NO_GRAVITY, x, y);
    }

    void dismiss() {
        this.window.dismiss();
    }
}
