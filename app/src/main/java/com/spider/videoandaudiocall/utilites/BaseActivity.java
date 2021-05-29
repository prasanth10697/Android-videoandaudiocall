package com.spider.videoandaudiocall.utilites;

import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {


    /*--- Baseactivity Toast Helper ---*/
    public void toast(String text, Context context) {

        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

}
