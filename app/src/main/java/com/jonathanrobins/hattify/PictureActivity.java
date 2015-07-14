package com.jonathanrobins.hattify;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PictureActivity extends ActionBarActivity {
    private Button backButton;
    private Button doneButton;
    private ImageView picture;
    private ImageView saveIcon;
    private ImageView watermark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //hides various bars
        getSupportActionBar().hide();
        setContentView(R.layout.activity_picture);
        //intializes buttons and sets colors
        backButton = (Button) findViewById(R.id.backButton);
        doneButton = (Button) findViewById(R.id.doneButton);
        backButton.setTextColor(Color.parseColor("white"));

        //receives picture and sets it to imageview
        Intent intent = getIntent();
        picture = (ImageView) findViewById(R.id.picture);
        Bitmap bitmap = GlobalClass.bitmap;
        picture.setImageBitmap(bitmap);
        picture.setAdjustViewBounds(true);

        saveIcon = (ImageView) findViewById(R.id.save);
        watermark = (ImageView) findViewById(R.id.watermark);
        watermark.setVisibility(View.INVISIBLE);

        //initializes on-click methods for various buttons
        miscButtonLogic();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up backButton, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //back button override
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            Intent i = new Intent(getBaseContext(), MainActivity.class);
            GlobalClass.didFinishEditing = true;
            startActivity(i);
            this.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void miscButtonLogic() {
        //back button listener
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backDialog();
            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backButton.setVisibility(View.INVISIBLE);
                doneButton.setVisibility(View.INVISIBLE);
                saveIcon.setVisibility(View.INVISIBLE);
                //calls dialog window and save logic
                saveDialog();
            }
        });
    }

    public void saveDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage("Would you like to save this picture?")
                        //yes
                .setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //screenshots current screen with added pepes
                        watermark.setVisibility(View.VISIBLE);
                        watermark.bringToFront();
                        View v = getWindow().getDecorView().getRootView();
                        v.setDrawingCacheEnabled(true);
                        Bitmap bmp = Bitmap.createBitmap(v.getDrawingCache());
                        v.setDrawingCacheEnabled(false);

                        try {

                            String path = Environment.getExternalStorageDirectory()
                                    .toString();
                            File newFolder = new File(path + "/HattifiedPictures");
                            newFolder.mkdirs();
                            OutputStream fOut = null;

                            //get timestamp of picture taken
                            SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss");
                            String timestamp = s.format(new Date());

                            //save image
                            File file = new File(path, "/HattifiedPictures/" + timestamp + ".png");
                            fOut = new FileOutputStream(file);
                            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                            fOut.flush();
                            fOut.close();

                            //refresh galleries and photo apps
                            MediaScannerConnection.scanFile(PictureActivity.this, new String[]{file.getPath()}, new String[]{"image/jpeg"}, null);

                            //final logic for saving picture
                            Toast.makeText(getApplicationContext(),
                                    "Your Hattified picture has been saved!", Toast.LENGTH_LONG)
                                    .show();
                            finish();
                            Intent i = new Intent(getBaseContext(), MainActivity.class);
                            GlobalClass.didFinishEditing = true;
                            startActivity(i);
                            PictureActivity.this.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);

                        } catch (Exception e) {
                            Toast.makeText(
                                    getApplicationContext(),
                                    "Problem to Save the File", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                        //no
                .setNegativeButton("Not yet.", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        backButton.setVisibility(View.VISIBLE);
                        doneButton.setVisibility(View.VISIBLE);
                        saveIcon.setVisibility(View.VISIBLE);
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public void backDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage("Would you like to take another picture?")
                        //yes
                .setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //goes back
                        finish();
                        Intent i = new Intent(getBaseContext(), MainActivity.class);
                        GlobalClass.didFinishEditing = true;
                        startActivity(i);
                        PictureActivity.this.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                        GlobalClass.bitmap = null;
                    }
                })
                        //no
                .setNegativeButton("Not yet.", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //stays
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
