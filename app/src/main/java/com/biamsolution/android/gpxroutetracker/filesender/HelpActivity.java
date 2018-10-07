package com.biamsolution.android.gpxroutetracker.filesender;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        mToolbar.setTitle(getString(R.string.app_name));
        mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),FileTransferSenderActivity.class));
            }
        });

        TextView htmlTextView = findViewById(R.id.helptextView);
        htmlTextView.setText(Html.fromHtml(getString(R.string.helpText)));
        htmlTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
