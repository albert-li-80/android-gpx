/*    
 * Copyright (c) 2015 Samsung Electronics Co., Ltd. All rights reserved. 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that 
 * the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice, 
 *       this list of conditions and the following disclaimer. 
 *     * Redistributions in binary form must reproduce the above copyright notice, 
 *       this list of conditions and the following disclaimer in the documentation and/or 
 *       other materials provided with the distribution. 
 *     * Neither the name of Samsung Electronics Co., Ltd. nor the names of its contributors may be used to endorse
 *       or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.biamsolution.android.gpxroutetracker.filesender;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.biamsolution.android.gpxroutetracker.filesender.FileTransferSender.FileAction;
import com.biamsolution.android.gpxroutetracker.filesender.FileTransferSender.SenderBinder;
import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FileTransferSenderActivity extends AppCompatActivity implements OnClickListener {
    private static final String TAG = "FileTranSenderActivity";
    private static final String SRC_PATH =
                Environment.getExternalStorageDirectory().getAbsolutePath() + "/src.aaa";
    private Button mBtnSend;
    private Button mBtnExit;
    private Button mBtnConn;
    private Button mBtnCancel;
    private Button mBtnCancelAll;
    private ProgressBar mSentProgressBar;

    private Button mBtnPick;
    private String srcPath;
    private boolean isConnected = false;

    private Context mCtxt;
    private String mDirPath;
    private long currentTransId;
    private long mFileSize;
    private List<Long> mTransactions = new ArrayList<Long>();
    private FileTransferSender mSenderService;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Service disconnected");
            mSenderService = null;
        }

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder) {
            Log.d(TAG, "Service connected");
            mSenderService = ((SenderBinder) binder).getService();
            mSenderService.registerFileAction(getFileAction());

            mSenderService.connect();
            mBtnConn.setClickable(false);
            mBtnConn.getBackground().setColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_ATOP);
            mBtnConn.setText(R.string.connecting);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.info_id:
                Intent intent = new Intent(this, HelpActivity.class);
                startActivity(intent);
                return true;
            default:
                return true;
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ft_sender_activity);

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        mCtxt = getApplicationContext();
        mBtnSend = (Button) findViewById(R.id.Send);
        mBtnSend.setOnClickListener(this);
        mBtnSend.setClickable(false);
        mBtnSend.getBackground().setColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_ATOP);

        mBtnExit = (Button) findViewById(R.id.Exit);
        mBtnExit.setOnClickListener(this);

        mBtnConn = (Button) findViewById(R.id.connectButton);
        mBtnConn.setOnClickListener(this);
//        mBtnCancel = (Button) findViewById(R.id.cancel);
//        mBtnCancel.setOnClickListener(this);
//        mBtnCancelAll = (Button) findViewById(R.id.cancelAll);
//        mBtnCancelAll.setOnClickListener(this);

        mBtnPick = (Button) findViewById(R.id.Pick);
        mBtnPick.setOnClickListener(this);

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(mCtxt, " No SDCARD Present", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            mDirPath = Environment.getExternalStorageDirectory() + File.separator + "FileTransferSender";
            File file = new File(mDirPath);
            if (file.mkdirs()) {
                Toast.makeText(mCtxt, " Stored in " + mDirPath, Toast.LENGTH_LONG).show();
            }
        }
        mCtxt.bindService(new Intent(getApplicationContext(), FileTransferSender.class),
                    this.mServiceConnection, Context.BIND_AUTO_CREATE);
        mSentProgressBar = (ProgressBar) findViewById(R.id.fileTransferProgressBar);
        mSentProgressBar.setMax(100);
    }

    public void onDestroy() {
        getApplicationContext().unbindService(mServiceConnection);
        super.onDestroy();
    }

    public void onError(MediaRecorder mr, int what, int extra) {
        Toast.makeText(mCtxt, " MAX SERVER DIED ", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    // for Android before 2.0, just in case
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onClick(View v) {
        if (v.equals(mBtnSend)) {
            File file = new File(srcPath);
            mFileSize = file.length();
            Toast.makeText(mCtxt, "Sending File: " + srcPath + " size " + mFileSize + " bytes", Toast.LENGTH_SHORT).show();
            if (isSenderServiceBound()) {
                try {
                    int trId = mSenderService.sendFile(srcPath);
                    mTransactions.add((long) trId);
                    currentTransId = trId;

                    // message
                    EditText mRouteName = findViewById(R.id.routeName);
                    String route = mRouteName.getText().toString();
                    if ((route == null) || (route.equals("")))
                        route = "GPX Route";
                    mSenderService.sendData(route);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    Toast.makeText(mCtxt, "IllegalArgumentException", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (v.equals(mBtnCancel)) {
            if (mSenderService != null) {
                try {
                    mSenderService.cancelFileTransfer((int) currentTransId);
                    mTransactions.remove(currentTransId);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    Toast.makeText(mCtxt, "IllegalArgumentException", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(mCtxt, "no binding to service", Toast.LENGTH_SHORT).show();
            }
        } else if (v.equals(mBtnCancelAll)) {
            if (mSenderService != null) {
                mSenderService.cancelAllTransactions();
                mTransactions.clear();
            } else {
                Toast.makeText(mCtxt, "no binding to service", Toast.LENGTH_SHORT).show();
            }
        } else if (v.equals(mBtnConn)) {
            if (mSenderService != null) {
                mSenderService.connect();
                mBtnConn.setClickable(false);
                mBtnConn.getBackground().setColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_ATOP);
                mBtnConn.setText(R.string.connecting);

            } else {
                Toast.makeText(getApplicationContext(), "Service not Bound", Toast.LENGTH_SHORT).show();
            }
        } else if (v.equals(mBtnPick)) {
            checkPermissionsAndOpenFilePicker();
        } else if (v.equals(mBtnExit)) {
            finish();
        }
    }

    public static final int PERMISSIONS_REQUEST_CODE = 0;

    private void checkPermissionsAndOpenFilePicker() {
        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                showError();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSIONS_REQUEST_CODE);
            }
        } else {
            openFilePicker();
        }
    }

    private void showError() {
        Toast.makeText(this, "Allow external storage reading", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFilePicker();
                } else {
                    showError();
                }
            }
        }
    }

    private void openFilePicker() {
        new MaterialFilePicker()
                .withActivity(this)
                .withRequestCode(1000)
                .withFilter(Pattern.compile(".*\\.gpx$")) // Filtering files and directories by file name using regexp
                .withFilterDirectories(false) // Set directories filterable (false by default)
                .withHiddenFiles(true) // Show hidden files and folders
                .start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1000 && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            // Do anything with file
            mBtnPick.setText("File Selected:\n" + filePath);
            mBtnPick.setTextSize(10);
//            mTextView.setText(filePath);
            srcPath = filePath;

            if (isConnected) {
                mBtnSend.setClickable(true);
                mBtnSend.getBackground().clearColorFilter();
            }
        }
    }



    private FileAction getFileAction() {
        return new FileAction() {

            @Override
            public void onFileServiceConnected() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBtnConn.setClickable(false);
                        mBtnConn.getBackground().setColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_ATOP);
                        mBtnConn.setText(R.string.connected);
                        isConnected = true;

                        if (srcPath != null) {
                            mBtnSend.setClickable(true);
                            mBtnSend.getBackground().clearColorFilter();
                        }
                    }
                });
            }

            @Override
            public void onFileServiceConnectionLost() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBtnConn.setClickable(true);
                        mBtnConn.setText(R.string.seek_and_connect);
                        mBtnConn.getBackground().clearColorFilter();
                        isConnected = false;
                        mBtnSend.setClickable(false);
                        mBtnSend.getBackground().setColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_ATOP);
                    }
                });
            }

            @Override
            public void onFileActionError() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSentProgressBar.setProgress(0);
                        mTransactions.remove(currentTransId);
//                        Toast.makeText(mCtxt, "Error", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFileActionProgress(final long progress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSentProgressBar.setProgress((int) progress);
                    }
                });
            }

            @Override
            public void onFileActionTransferComplete() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSentProgressBar.setProgress(0);
                        mTransactions.remove(currentTransId);
                        Toast.makeText(mCtxt, "Transfer Completed!", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFileActionCancelAllComplete() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSentProgressBar.setProgress(0);
                        mTransactions.remove(currentTransId);
                    }
                });
            }
        };
    }

    private boolean isSenderServiceBound() {
        return this.mSenderService != null;
    }
}
