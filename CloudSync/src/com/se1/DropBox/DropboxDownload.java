package com.se1.DropBox;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxInputStream;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.TokenPair;
import com.se1.main.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


public class DropboxDownload extends SherlockFragment implements OnItemClickListener {
    private DropboxAPI<AndroidAuthSession> mApi;
    private String DIR = "/";
    private ArrayList<Entry> files;
    private ArrayList<String> dir;
    private boolean isItemClicked = false;
    // , onResume = false;
    private ListView lvDropboxDownloadFilesList;

    private Context mContext;
    private ImageView mView;
    private Drawable mDrawable;
    private FileOutputStream mFos;

    private Long mFileLen;
    // private Button btnDropboxDownloadDone;
    private ProgressDialog pd;
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (msg.what == 0) {
                lvDropboxDownloadFilesList.setAdapter(new DownloadFileAdapter(
                        getActivity(), files));
                pd.dismiss();
            } else if (msg.what == 1) {
                Toast.makeText(getActivity(),
                        "File save at " + msg.obj.toString(), Toast.LENGTH_LONG)
                        .show();
            }
        };
    };
    public  void setDataFromLogin( DropboxAPI<AndroidAuthSession> api) {
        mApi = api;
        //setLoggedIn(true);


    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("in dropbox Download","inside");
        View v =inflater.inflate(R.layout.dropboxdownload,container,false);
        v.setFocusableInTouchMode(true);
        v.requestFocus();
        v.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // Log.i(tag, "keyCode: " + keyCode);
                if( keyCode == KeyEvent.KEYCODE_BACK ) {
                    Log.d("back", "onKey Back listener is working!!!");
                    if (isItemClicked) {
                        if (DIR.length() == 0) {
                            // logOut();
                            getActivity().setResult(getActivity().RESULT_OK);
                            //super.getActivity().onBackPressed();
                        } else {
                            DIR = DIR.substring(0, DIR.lastIndexOf('/'));
                            setLoggedIn(true);

                        }
                    } else {
                        getActivity().setResult(getActivity().RESULT_OK);
                        //super.().onBackPressed();
                    }
                    // /getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    return true;
                } else {
                    return false;
                }
            }
        });
        mContext =getActivity().getApplicationContext();
        mView = (ImageView)v.findViewById(R.id.image_view);
        lvDropboxDownloadFilesList = (ListView) v.findViewById(R.id.lvDropboxDownloadFilesList);
        AndroidAuthSession session = buildSession();
        mApi = new DropboxAPI<AndroidAuthSession>(session);

        checkAppKeySetup();
        // setLoggedIn(false);
        if (!Constants.mLoggedIn)
            mApi.getSession().startAuthentication(getActivity());

        lvDropboxDownloadFilesList.setOnItemClickListener(this);
        return v;
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

        Entry fileSelected = files.get(arg2);
        if (fileSelected.isDir) {
            isItemClicked = true;
            DIR = dir.get(arg2);
            setLoggedIn(true);
        } else {

            downloadDropboxFile(fileSelected);
            // getIntent().getStringExtra("fileParentPath"));
        }
    }

    // @Override
    // public void onClick(View v) {
    // if (v == btnDropboxDownloadDone) {
    // setResult(RESULT_OK);
    // this.finish();
    // }
    // }

    private void checkAppKeySetup() {
        if (Constants.DROPBOX_APP_KEY.startsWith("CHANGE")
                || Constants.DROPBOX_APP_SECRET.startsWith("CHANGE")) {
            showToast("You must apply for an app key and secret from developers.dropbox.com, and add them to the DBRoulette ap before trying it.");
            getActivity().finish();
            return;
        }
        Intent testIntent = new Intent(Intent.ACTION_VIEW);
        String scheme = "db-" + Constants.DROPBOX_APP_KEY;
        String uri = scheme + "://" + AuthActivity.AUTH_VERSION + "/test";
        testIntent.setData(Uri.parse(uri));
        PackageManager pm = getActivity().getPackageManager();
        if (0 == pm.queryIntentActivities(testIntent, 0).size()) {
            showToast("URL scheme in your app's "
                    + "manifest is not set up correctly. You should have a "
                    + "com.dropbox.client2.android.AuthActivity with the "
                    + "scheme: " + scheme);
            getActivity().finish();
        }
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG);
        error.show();
    }

    private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(Constants.DROPBOX_APP_KEY,
                Constants.DROPBOX_APP_SECRET);
        AndroidAuthSession session;

        String[] stored = getKeys();
        if (stored != null) {
            AccessTokenPair accessToken = new AccessTokenPair(stored[0],
                    stored[1]);
            session = new AndroidAuthSession(appKeyPair, Constants.ACCESS_TYPE,
                    accessToken);
        } else {
            session = new AndroidAuthSession(appKeyPair, Constants.ACCESS_TYPE);
        }

        return session;
    }

    public void setLoggedIn(final boolean loggedIn) {
        pd = ProgressDialog.show(getActivity(), null,
                "Retrieving data...");
        new Thread(new Runnable() {

            @Override
            public void run() {
                Constants.mLoggedIn = loggedIn;
                if (loggedIn) {
                    int i = 0;
                    com.dropbox.client2.DropboxAPI.Entry dirent;
                    try {
                        dirent = mApi.metadata(DIR, 1000, null, true, null);
                        files = new ArrayList<com.dropbox.client2.DropboxAPI.Entry>();
                        dir = new ArrayList<String>();
                        for (com.dropbox.client2.DropboxAPI.Entry ent : dirent.contents) {
                            files.add(ent);
                            dir.add(new String(files.get(i++).path));
                        }
                        i = 0;
                        mHandler.sendEmptyMessage(0);
                    } catch (DropboxException e) {
                        e.printStackTrace();
                    }

                }
            }
        }).start();

    }

    @Override
    public void onResume() {

        super.onResume();
        AndroidAuthSession session = mApi.getSession();

        if (session.authenticationSuccessful()) {
            try {
                session.finishAuthentication();

                TokenPair tokens = session.getAccessTokenPair();
                storeKeys(tokens.key, tokens.secret);
                setLoggedIn(true);
            } catch (IllegalStateException e) {
                showToast("Couldn't authenticate with Dropbox:"
                        + e.getLocalizedMessage());
            }
        }
    }

    private void storeKeys(String key, String secret) {
        SharedPreferences prefs =getActivity().getSharedPreferences(
                Constants.ACCOUNT_PREFS_NAME, 0);
        Editor edit = prefs.edit();
        edit.putString(Constants.ACCESS_KEY_NAME, key);
        edit.putString(Constants.ACCESS_SECRET_NAME, secret);
        edit.commit();
    }


    private String[] getKeys() {
        SharedPreferences prefs = getActivity().getSharedPreferences(
                Constants.ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(Constants.ACCESS_KEY_NAME, null);
        String secret = prefs.getString(Constants.ACCESS_SECRET_NAME, null);
        if (key != null && secret != null) {
            String[] ret = new String[2];
            ret[0] = key;
            ret[1] = secret;
            return ret;
        } else {
            return null;
        }
    }

    private boolean downloadDropboxFile(Entry fileSelected) {// , String
        // localFilePath)
        // {
        File dir = new File(Utils.getPath());
        if (!dir.exists())
            dir.mkdirs();
        if((fileSelected.mimeType).equalsIgnoreCase("image/png") || ( fileSelected.mimeType).equalsIgnoreCase("image/jpeg") ) {
            try {
                File localFile = new File(dir + "/" + fileSelected.fileName());
                if (!localFile.exists()) {
                    localFile.createNewFile();
                    copy(fileSelected, localFile,dir,"image/png");
                } else {
                    showFileExitsDialog(fileSelected, localFile,dir,"image/png");


                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if((fileSelected.mimeType).equalsIgnoreCase("application/pdf"))
        {
            try {
                File localFile = new File(dir + "/" + fileSelected.fileName());
                if (!localFile.exists()) {
                    localFile.createNewFile();
                    copy(fileSelected, localFile,dir,"application/pdf");
                } else {
                    showFileExitsDialog(fileSelected, localFile,dir,"application/pdf");

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        else if((fileSelected.mimeType).equalsIgnoreCase("text/plain"))
        {
            try {
                File localFile = new File(dir + "/" + fileSelected.fileName());
                if (!localFile.exists()) {
                    localFile.createNewFile();
                    copy(fileSelected, localFile,dir,"text/plain");
                } else {
                    showFileExitsDialog(fileSelected, localFile,dir,"text/plain");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
        {
            try {
                File localFile = new File(dir + "/" + fileSelected.fileName());

                if (!localFile.exists()) {
                    localFile.createNewFile();
                    copy(fileSelected, localFile,dir,"NA");
                } else {
                    showFileExitsDialog(fileSelected, localFile,dir,"NA");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }



    private void openFile(File dir,final Entry fileSelected,String type) {
        File file = new File(dir + "/" + fileSelected.fileName());
        Uri localpath = Uri.fromFile(file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(localpath,type);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            mContext.startActivity(intent);
        }
        catch (ActivityNotFoundException e) {
            Log.d("Error","No Application Available to View this file");
        }
    }
	/*copy file from dropbox to local directory*/

    private void copy(final Entry fileSelected, final File localFile, final File dir,String type) {
        final ProgressDialog pd = ProgressDialog.show(getActivity(),
                "Downloading...", "Please wait...");
        Log.d("fileSelected.path","fileSelected.path"+fileSelected.path);
        new Thread(new Runnable() {

            @Override
            public void run() {
                BufferedInputStream br = null;
                BufferedOutputStream bw = null;
                DropboxInputStream fd;
                try {
                    Log.d("mApi","mApi is=="+mApi);
                    fd = mApi.getFileStream(fileSelected.path,null);
                           // localFile.getPath());
                    br = new BufferedInputStream(fd);
                    bw = new BufferedOutputStream(new FileOutputStream(
                            localFile));

                    byte[] buffer = new byte[4096];
                    int read;
                    while (true) {
                        read = br.read(buffer);
                        if (read <= 0) {
                            break;
                        }
                        bw.write(buffer, 0, read);
                    }
                    pd.dismiss();
                    Message message = new Message();
                    message.obj = localFile.getAbsolutePath();
                    message.what = 1;
                    mHandler.sendMessage(message);
                    if((fileSelected.mimeType).equalsIgnoreCase("image/png"))
                        openFile(dir,fileSelected,"image/png");
                    else if((fileSelected.mimeType).equalsIgnoreCase("image/jpeg"))
                        openFile(dir,fileSelected,"image/jpeg");
                    else if((fileSelected.mimeType).equalsIgnoreCase("image/jpg"))
                        openFile(dir,fileSelected,"image/jpg");
                    else if((fileSelected.mimeType).equalsIgnoreCase("text/plain"))
                        openFile(dir,fileSelected,"text/plain");
                    else if((fileSelected.mimeType).equalsIgnoreCase("application/pdf"))
                        openFile(dir,fileSelected,"application/pdf");
                    else
                    {
                        showToast("Could not open this file type");
                    }
                } catch (DropboxException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (bw != null) {
                        try {
                            bw.close();
                            if (br != null) {
                                br.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
        }).start();

    }

    private void showFileExitsDialog(final Entry fileSelected,
                                     final File localFile,final File dir, final String type) {
        AlertDialog.Builder alertBuilder = new Builder(getActivity());
        alertBuilder.setMessage(Constants.OVERRIDEMSG);
        alertBuilder.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        copy(fileSelected, localFile,dir,type);
                    }
                });
        alertBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if((fileSelected.mimeType).equalsIgnoreCase("image/png"))
                            openFile(dir,fileSelected,"image/png");
                        else if((fileSelected.mimeType).equalsIgnoreCase("image/jpeg"))
                            openFile(dir,fileSelected,"image/jpeg");
                        else if((fileSelected.mimeType).equalsIgnoreCase("image/jpg"))
                            openFile(dir,fileSelected,"image/jpg");
                        else if((fileSelected.mimeType).equalsIgnoreCase("text/plain"))
                            openFile(dir,fileSelected,"text/plain");
                        else if((fileSelected.mimeType).equalsIgnoreCase("application/pdf"))
                            openFile(dir,fileSelected,"application/pdf");
                        else
                        {
                            showToast("Could not open this file type");
                        }
                    }
                });

        alertBuilder.create().show();

    }




}
