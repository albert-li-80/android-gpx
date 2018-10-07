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

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.*;
import com.samsung.android.sdk.accessoryfiletransfer.*;
import com.samsung.android.sdk.accessoryfiletransfer.SAFileTransfer.*;

import java.io.IOException;

public class FileTransferSender extends SAAgent {
    private static final String TAG = "FileTransferSender";
    private static final Class<ServiceConnection> SASOCKET_CLASS = ServiceConnection.class;
    private int trId = -1;
    private int errCode = SAFileTransfer.ERROR_NONE;
    private SAPeerAgent mPeerAgent = null;
    private final IBinder mSenderBinder = new SenderBinder();
    private SAFileTransfer mSAFileTransfer = null;
    // --Commented out by Inspection (6/10/2018 5:48 PM):private SAMessage mMessage = null;
    private EventListener mCallback = null;
    private FileAction mFileAction = null;
    private ServiceConnection mConnection = null;


    public FileTransferSender() {
        super(TAG, SASOCKET_CLASS);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "On Create of Sample FileTransferSender Service");
        mCallback = new EventListener() {
            @Override
            public void onProgressChanged(int transId, int progress) {
                Log.d(TAG, "onProgressChanged : " + progress + " for transaction : " + transId);
                if (mFileAction != null) {
                    mFileAction.onFileActionProgress(progress);
                }
            }

            @Override
            public void onTransferCompleted(int transId, String fileName, int errorCode) {
                errCode = errorCode;
                Log.d(TAG, "onTransferCompleted: tr id : " + transId + " file name : " + fileName + " error : "
                            + errorCode);
                if (errorCode == SAFileTransfer.ERROR_NONE) {
                    mFileAction.onFileActionTransferComplete();
                } else {
                    mFileAction.onFileActionError();
                }
            }

            @Override
            public void onTransferRequested(int id, String fileName) {
                // No use at sender side
            }

            @Override
            public void onCancelAllCompleted(int errorCode) {
                if (errorCode == SAFileTransfer.ERROR_NONE) {
                    mFileAction.onFileActionCancelAllComplete();
                }
                else if (errorCode == SAFileTransfer.ERROR_TRANSACTION_NOT_FOUND) {
                    Toast.makeText(getBaseContext(), "onCancelAllCompleted : ERROR_TRANSACTION_NOT_FOUND.", Toast.LENGTH_SHORT).show();
                }
                else if (errorCode == SAFileTransfer.ERROR_NOT_SUPPORTED) {
                    Toast.makeText(getBaseContext(), "onCancelAllCompleted : ERROR_NOT_SUPPORTED.", Toast.LENGTH_SHORT).show();
                }
                Log.e(TAG, "onCancelAllCompleted: Error Code " + errorCode);
            }
        };

        SAft saft = new SAft();
        SA mAccessory = new SA();

        try {
            saft.initialize(this);
            mAccessory.initialize(this);
        } catch (SsdkUnsupportedException e) {
            if (e.getType() == SsdkUnsupportedException.DEVICE_NOT_SUPPORTED) {
                Toast.makeText(getBaseContext(), "Cannot initialize, DEVICE_NOT_SUPPORTED", Toast.LENGTH_SHORT).show();
            } else if (e.getType() == SsdkUnsupportedException.LIBRARY_NOT_INSTALLED) {
                Toast.makeText(getBaseContext(), "Cannot initialize, LIBRARY_NOT_INSTALLED.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getBaseContext(), "Cannot initialize, UNKNOWN.", Toast.LENGTH_SHORT).show();
            }
            e.printStackTrace();
            return;
        } catch (Exception e1) {
            Toast.makeText(getBaseContext(), "Cannot initialize, SAft.", Toast.LENGTH_SHORT).show();
            e1.printStackTrace();
            return;
        }
        mSAFileTransfer = new SAFileTransfer(FileTransferSender.this, mCallback);

    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mSenderBinder;
    }

    @Override
    public void onDestroy() {
        try {
            mSAFileTransfer.close();
            mSAFileTransfer = null;
        } catch (RuntimeException e) {
            Log.e(TAG, e.getMessage());
        }
        super.onDestroy();
        Log.i(TAG, "FileTransferSender Service is Stopped.");
    }

    @Override
    protected void onFindPeerAgentsResponse(SAPeerAgent[] peerAgents, int result) {
        if (peerAgents != null) {
            for(SAPeerAgent peerAgent : peerAgents)
                mPeerAgent = peerAgent;
            connect();
        } else {
            Log.e(TAG, "No peer Agent found:" + result);
//            Toast.makeText(getBaseContext(), "No peer agent found.", Toast.LENGTH_SHORT).show();
            super.findPeerAgents();
        }
    }

    @Override
    protected void onPeerAgentsUpdated(SAPeerAgent[] peerAgents, int result) {
        Log.d(TAG, "Peer agent updated- result: "+ result + " trId: "+ trId);
        for(SAPeerAgent peerAgent : peerAgents)
            mPeerAgent = peerAgent;
        if (result == SAAgent.PEER_AGENT_UNAVAILABLE) {
            if (errCode != SAFileTransfer.ERROR_CONNECTION_LOST) {
                try {
                    cancelFileTransfer(trId);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
//                    Toast.makeText(getBaseContext(), "IllegalArgumentException", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onServiceConnectionResponse(SAPeerAgent peerAgent, SASocket socket, int result) {
        Log.i(TAG, "onServiceConnectionResponse: result - " + result);
        if (socket == null) {
            if (result == SAAgent.CONNECTION_ALREADY_EXIST) {
//                Toast.makeText(getBaseContext(), "CONNECTION_ALREADY_EXIST", Toast.LENGTH_SHORT).show();
            } else {
//                Toast.makeText(getBaseContext(), "Connection could not be made. Please try again", Toast.LENGTH_SHORT).show();
                if (mPeerAgent != null) {
//                    super.findPeerAgents();
                    requestServiceConnection(mPeerAgent);
                } else {
                    super.findPeerAgents();
//                    Toast.makeText(getBaseContext(), "No peer agent found yet. Please try again", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(getBaseContext(), "Connection established for FT", Toast.LENGTH_SHORT).show();
            mFileAction.onFileServiceConnected();
            mConnection = (ServiceConnection) socket;
        }
    }

    public void connect() {
        if (mPeerAgent != null) {
            requestServiceConnection(mPeerAgent);
        } else {
            super.findPeerAgents();
//            Toast.makeText(getBaseContext(), "No peer agent found yet. Please try again", Toast.LENGTH_SHORT).show();
        }
    }

    public int sendFile(String mSelectedFileName) {
        if (mSAFileTransfer != null && mPeerAgent != null) {
            trId = mSAFileTransfer.send(mPeerAgent, mSelectedFileName);
            return trId;
        } else {
//            Toast.makeText(getBaseContext(), "Peer could not be found. Try again.", Toast.LENGTH_SHORT).show();
            findPeerAgents();
            return -1;
        }
    }

    public void sendData(String message) {
        if (mConnection != null) {
            final String my_message = message;
            new Thread(new Runnable() {
                public void run() {
                    try {
                        mConnection.send(getServiceChannelId(0), my_message.getBytes());
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }).start();
        }
        return;
    }

    public void cancelFileTransfer(int transId) {
        if (mSAFileTransfer != null) {
            mSAFileTransfer.cancel(transId);
        }
    }

    public void cancelAllTransactions()
    {
        if (mSAFileTransfer != null) {
            mSAFileTransfer.cancelAll();
        }
    }

    public void registerFileAction(FileAction action) {
        this.mFileAction = action;
    }

    public class SenderBinder extends Binder {
        public FileTransferSender getService() {
            return FileTransferSender.this;
        }
    }

    public class ServiceConnection extends SASocket {
        public ServiceConnection() {
            super(ServiceConnection.class.getName());
        }

        @Override
        protected void onServiceConnectionLost(int reason) {
            Log.e(TAG, "onServiceConnectionLost: reason-" + reason);
            if (mSAFileTransfer != null) {
                mFileAction.onFileActionError();
            }
            mPeerAgent = null;
            mFileAction.onFileServiceConnectionLost();
        }

        @Override
        public void onReceive(int channelId, byte[] data) {
        }

        @Override
        public void onError(int channelId, String errorMessage, int errorCode) {
        }
    }

    public interface FileAction {
        void onFileActionError();

        void onFileActionProgress(long progress);

        void onFileActionTransferComplete();

        void onFileActionCancelAllComplete();

        void onFileServiceConnected();

        void onFileServiceConnectionLost();
    }
}
