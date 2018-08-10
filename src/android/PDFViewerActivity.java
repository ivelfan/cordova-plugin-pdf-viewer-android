package com.ingensnetworks.plugin;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;

import com.github.barteksc.pdfviewer.PDFView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.UUID;

public class PDFViewerActivity extends Activity {

  private static final String TAG = "PDFViewerActivity";
  private String cancel = "Cancel";
  private String save = "Save";
  private File pdfFile = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    String package_name = getApplication().getPackageName();
    setContentView(getApplication().getResources().getIdentifier("activity_pdfviewer", "layout", package_name));
    Intent intent = getIntent();
    String name = intent.getStringExtra("filename");
    cancel = intent.getStringExtra("cancel");
    save = intent.getStringExtra("save");
    String ok = intent.getStringExtra("ok");
    Integer showButtons = intent.getIntExtra("showButtons", 0);

    ActionBar actionbar = getActionBar();
    actionbar.setDisplayShowHomeEnabled(false);
    actionbar.setTitle("");

    PDFView pdfView = (PDFView) findViewById(getResources().getIdentifier("pdfView", "id", getPackageName()));
    pdfFile = new File(name);

    if (pdfFile.exists()) {
      try {
        pdfView.fromFile(pdfFile)
                .defaultPage(0)
                .enableAntialiasing(true)
                .load();
      } catch (Exception ex) {
        int i = 0;
      }
    }

    Button btnOK = (Button) findViewById(getResources().getIdentifier("btnok", "id", getPackageName()));

    if (showButtons == 1 || showButtons == 2) {
      btnOK.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Intent intent = new Intent();
          intent.putExtra("action", "1");
          setResult(Activity.RESULT_OK, intent);
          finish();
        }
      });
      btnOK.setText(ok);
      btnOK.setVisibility(View.VISIBLE);

      Button btnCancel = (Button) findViewById(getResources().getIdentifier("btncancel", "id", getPackageName()));
      if (showButtons == 2) {
        btnCancel.setText(cancel);
        btnCancel.setVisibility(View.VISIBLE);
        btnCancel.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            onBackPressed();
          }
        });
      }
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(0, 0, 0, cancel).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    menu.add(1, 1, 0, save).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case 0:
        onBackPressed();
        return true;
      case 1:
        saveFile();
        return true;
    }
    return false;
  }

  @Override
  public void onBackPressed() {
    Intent intent = new Intent();
    intent.putExtra("action", "0");
    setResult(Activity.RESULT_OK, intent);
    finish();
  }

  public void saveFile() {
    File downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    File newPdfFile = new File(downloadFolder, UUID.randomUUID() + "-" + pdfFile.getName());

    if (isStoragePermissionGranted()) {
      try {
        copy(pdfFile, newPdfFile);

        DownloadManager downloadManager = (DownloadManager) this.getSystemService(DOWNLOAD_SERVICE);
        String typeMime = getMimeType(newPdfFile);
        if (downloadManager != null && typeMime != null) {
          downloadManager.addCompletedDownload(newPdfFile.getName(), newPdfFile.getName(), true, typeMime, newPdfFile.getAbsolutePath(), newPdfFile.length(), true);
        } else {
          onBackPressed();
        }
      } catch (IOException exception) {
        onBackPressed();
      }
    }
  }

  public  boolean isStoragePermissionGranted() {
    if (Build.VERSION.SDK_INT >= 23) {
      if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
              == PackageManager.PERMISSION_GRANTED) {
        Log.v(TAG,"Permission is granted");
        return true;
      } else {

        Log.v(TAG,"Permission is revoked");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        return false;
      }
    }
    else { //permission is automatically granted on sdk<23 upon installation
      Log.v(TAG,"Permission is granted");
      return true;
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
      Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);

      saveFile();
    }
  }


  public static void copy(File src, File dst) throws IOException {
    InputStream in = new FileInputStream(src);
    try {
      OutputStream out = new FileOutputStream(dst);
      try {
        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
          out.write(buf, 0, len);
        }
      } finally {
        out.close();
      }
    } finally {
      in.close();
    }
  }

  private String getMimeType(File file) {
    URI fileUri = file.toURI();
    String fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileUri
            .toString());
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            fileExtension.toLowerCase());
  }
}
