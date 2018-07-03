package com.example.luxiaoju.tbs;

import android.app.DownloadManager;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.tencent.smtt.sdk.TbsReaderView;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    public TbsReaderView tbsReaderView = null;
    public RelativeLayout relativeLayout;
    public Button btn_open;
    public String mFileName;
    private String mFileUrl = "http://www.beijing.gov.cn/zhuanti/ggfw/htsfwbxzzt/shxfl/fw/P020150720516332194302.doc";
    private DownloadManager mDownloadManager;
    private DownloadObserver mDownloaObserver;
    private long requestId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        tbsReaderView = new TbsReaderView(this, new TbsReaderView.ReaderCallback() {
            @Override
            public void onCallBackAction(Integer integer, Object o, Object o1) {

            }
        });
        btn_open = findViewById(R.id.btn_open);
        relativeLayout = findViewById(R.id.rl_root);
        relativeLayout.addView(tbsReaderView,new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        //mFileName = parseName(mFileUrl);
        mFileName = "testFile.docx";
        if (isLocalExist()) {
            btn_open.setText("打开文件");
        }

    }

    public void onClickDownload(View v) {
        if(isLocalExist()) {
            btn_open.setVisibility(View.GONE);
            displayFile();
        } else {
            Handler handler = new Handler(getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    startDownload();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tbsReaderView.onStop();
        if(mDownloaObserver != null) {
            getContentResolver().unregisterContentObserver(mDownloaObserver);
        }
    }

    public void startDownload() {
        mDownloaObserver = new DownloadObserver(new Handler());
        getContentResolver().registerContentObserver(Uri.parse("content://downloads/my_downloads"),true,mDownloaObserver);
        mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(mFileUrl));
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,mFileName);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        Handler handler1 = new Handler(getMainLooper());
        handler1.post(new Runnable() {
            @Override
            public void run() {
                requestId = mDownloadManager.enqueue(request);
            }
        });



    }

    public void queryDownloadStatus() {
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(requestId);
        Cursor cursor = null;
        try{
            cursor = mDownloadManager.query(query);
            if(cursor != null && cursor.moveToFirst()) {
                int currentBytes = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                int totalBytes = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                Log.i("downloadUpdate: ", currentBytes + " " + totalBytes + " " + status);
                btn_open.setText("正在下载:" + currentBytes+ "/" + totalBytes);
                if(DownloadManager.STATUS_SUCCESSFUL == status && btn_open.getVisibility() == View.VISIBLE) {
                    btn_open.setVisibility(View.GONE);
                    btn_open.performClick();
                }
            }
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }

    }
    private boolean isLocalExist() {
        return getLocalFile().exists();
    }

    private File getLocalFile() {
       File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), mFileName);
        return file;
    }
    private String parseFormat(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private String parseName(String url) {
        String name = null;
        try {
            name = url.substring(url.lastIndexOf("/") + 1);
        } finally {
            if(TextUtils.isEmpty(name)) {
                name = String.valueOf(System.currentTimeMillis());
            }
        }
        return name;
    }
    private void displayFile() {
        Bundle bundle = new Bundle();
        bundle.putString("filePath",getLocalFile().getPath());
        bundle.putString("tempPath",Environment.getExternalStorageDirectory().getPath());
        boolean result = tbsReaderView.preOpen(mFileName,true);
        if(result) {
            tbsReaderView.openFile(bundle);
        }
    }

    private class DownloadObserver extends ContentObserver {

        private DownloadObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.i("downloadUpdate: ", "onChange(boolean selfChange, Uri uri)");
            queryDownloadStatus();
        }
    }

}
