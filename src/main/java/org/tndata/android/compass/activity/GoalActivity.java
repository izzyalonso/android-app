package org.tndata.android.compass.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.tndata.android.compass.R;
import org.tndata.android.compass.util.CompassUtil;

import at.grabner.circleprogress.CircleProgressView;


/**
 * Created by isma on 9/24/15.
 */
public class GoalActivity extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal);

        ImageView hero = (ImageView)findViewById(R.id.goal_hero);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)hero.getLayoutParams();
        int heroHeight = CompassUtil.getScreenWidth(this)*2/3;
        params.height = heroHeight;
        hero.setLayoutParams(params);
        hero.setBackgroundColor(Color.RED);

        CircleProgressView indicator = (CircleProgressView)findViewById(R.id.goal_indicator);
        params = (RelativeLayout.LayoutParams)indicator.getLayoutParams();
        params.topMargin = heroHeight-params.width-2*params.topMargin;
        indicator.setLayoutParams(params);
        indicator.setValue(0);
        indicator.setAutoTextSize(true);
        indicator.setShowUnit(true);
        indicator.setValueAnimated(0, (int)(100 * Math.random()), 1500);

        TextView title = (TextView)findViewById(R.id.goal_title);
        params = (RelativeLayout.LayoutParams)title.getLayoutParams();
        params.topMargin += heroHeight+CompassUtil.getPixels(this, 1);
        title.setLayoutParams(params);
    }
}
