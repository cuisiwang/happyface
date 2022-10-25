package com.example.picprocessor;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

public class AnimDialog extends Dialog {
    public AnimDialog(@NonNull Context context) {
        super(context);
    }

    public AnimDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Window window=this.getWindow();
        if(window!=null){
            window.setWindowAnimations(R.style.zoom_out);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.width = WindowManager.LayoutParams.MATCH_PARENT;
            attributes.height = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(attributes);
        }
    }
}
