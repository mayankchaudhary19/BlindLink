package com.example.blindlink.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.example.blindlink.R;

public class Dialogs{
       public static Dialog setUserDialog(Context context) {
        Dialog applyCouponDialog;
        applyCouponDialog = new Dialog(context);
        applyCouponDialog.setContentView(R.layout.dialog_initial_login);
        applyCouponDialog.setCancelable(false);
        applyCouponDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        applyCouponDialog.getWindow().setGravity(Gravity.BOTTOM);
        applyCouponDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        applyCouponDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        applyCouponDialog.getWindow().setDimAmount(0.5f);
        return applyCouponDialog;
    }
}
