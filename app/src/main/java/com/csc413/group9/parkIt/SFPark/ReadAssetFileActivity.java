package com.csc413.group9.parkIt.SFPark;

/**
 * Created by khanh on 5/4/15.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class ReadAssetFileActivity {

    private String readFileContent(String file) throws IOException{


        TextView txtContent = (TextView) findViewById(R.id.txtContent);
        TextView txtFileName = (TextView) findViewById(R.id.txtFileName);
        ImageView imgAssets = (ImageView) findViewById(R.id.imgAssets);
        AssetManager assetManager = getAssets();
        try{
            String[] files = assetManager.list("Files");
            for(int i = 0; i < files.length; i++) {
                txtFileName.append("\n file=:" + i + "= name =>" + files[i]);
            }
            }catch(IOException ex){
            ex.printStackTrace();
        }
        InputStream input = getContentResolver().openInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        StringBuilder stringBuilder = new StringBuilder();
        String current
        try{
            input = assetManager.open("GPS.txt");
            int size = input.available();
            byte[] buffer = new byte[size];
            input.read(buffer);
            input.close();
            String text = new String(buffer);
            txtContent.setText(text);
        }catch(IOException ex){
            ex.printStackTrace();
        }

    }

}
