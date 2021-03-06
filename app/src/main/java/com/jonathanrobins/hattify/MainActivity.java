package com.jonathanrobins.hattify;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import android.media.FaceDetector;
import android.media.FaceDetector.Face;


public class MainActivity extends ActionBarActivity {

    private Button cameraButton;
    private Button galleryButton;
    private TextView titleText;
    private final int REQUEST_IMAGE_CAPTURE = 1;
    private final int SELECT_PICTURE = 2;
    private String selectedImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //hides various bars
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        //references
        cameraButton = (Button) findViewById(R.id.cameraButton);
        galleryButton = (Button) findViewById(R.id.galleryButton);
        titleText = (TextView) findViewById(R.id.titleText);

        //typeface
        Typeface typeface = Typeface.createFromAsset(getAssets(), "SEASRN__.ttf");
        titleText.setTypeface(typeface);

        //on touch listeners for buttons
        cameraButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(getTempFile(getApplicationContext())));
                        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                        MainActivity.this.overridePendingTransition(android.R.anim.slide_out_right, android.R.anim.slide_in_left);
                    }
                }
                return true;
            }
        });

        galleryButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
                    }
                }
                return true;
            }
        });

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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Bitmap bitmap;
            switch (requestCode) {
                //camera
                case REQUEST_IMAGE_CAPTURE:
                    final File file = getTempFile(this);
                    try {
                        //retrieve bitmap for picture taken
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.fromFile(file));
                        //get exif data and orientation to determine auto-rotation for picture
                        ExifInterface exif = new ExifInterface("" + file);
                        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

                        //rotate picture certain # of degrees depending on orientation then sets new matrix
                        bitmap = rotateBitmap(bitmap, orientation);

                        Bitmap faceBitmap = bitmap.copy(Bitmap.Config.RGB_565, true);
                        bitmap.recycle();

                        faceBitmap = addHats(faceBitmap);

                        Intent i = new Intent(getBaseContext(), PictureActivity.class);
                        //assigns to global bitmap variable then goes to intent
                        GlobalClass.bitmap = faceBitmap;
                        startActivity(i);
                        finish();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                //gallery
                case SELECT_PICTURE:
                    Uri selectedImageUri = data.getData();
                    if (Build.VERSION.SDK_INT < 19) {
                        try {
                            selectedImagePath = getPath(selectedImageUri);
                            bitmap = BitmapFactory.decodeFile(selectedImagePath);

                            String path = getImagePathForRotation(selectedImageUri);
                            ExifInterface exif = new ExifInterface(path);

                            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

                            //rotate picture certain # of degrees depending on orientation then sets new matrix
                            bitmap = rotateBitmap(bitmap, orientation);

                            Bitmap faceBitmap = bitmap.copy(Bitmap.Config.RGB_565, true);
                            bitmap.recycle();

                            faceBitmap = addHats(faceBitmap);

                            Intent i = new Intent(getBaseContext(), PictureActivity.class);
                            //assigns to global bitmap variable then goes to intent
                            GlobalClass.bitmap = faceBitmap;
                            startActivity(i);
                            finish();

                        }
                        catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    else {
                        ParcelFileDescriptor parcelFileDescriptor;
                        try {
                            parcelFileDescriptor = getContentResolver().openFileDescriptor(selectedImageUri, "r");
                            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                            bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                            parcelFileDescriptor.close();

                            String path = getImagePathForRotation(selectedImageUri);

                            ExifInterface exif = new ExifInterface(path);
                            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

                            //rotate picture certain # of degrees depending on orientation then sets new matrix
                            bitmap = rotateBitmap(bitmap, orientation);

                            Bitmap faceBitmap = bitmap.copy(Bitmap.Config.RGB_565, true);
                            bitmap.recycle();

                            faceBitmap = addHats(faceBitmap);

                            Intent i = new Intent(getBaseContext(), PictureActivity.class);
                            //assigns to global bitmap variable then goes to intent
                            GlobalClass.bitmap = faceBitmap;
                            startActivity(i);
                            finish();

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }

            }
        }
    }

    private Bitmap rotateBitmap(Bitmap originalBitmap, int orientation){
        Matrix matrix = new Matrix();

        switch (orientation) {
            case 0:
                originalBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
                break;
            case 1:
                originalBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                originalBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                originalBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                originalBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
                break;
        }

        return originalBitmap;
    }

    private Bitmap addHats(Bitmap faceBitmap) {
        int width = faceBitmap.getWidth();
        int height = faceBitmap.getHeight();

        Face[] detectedFaces = new FaceDetector.Face[20];
        FaceDetector faceDetector = new FaceDetector(width, height, 20);
        int NUMBER_OF_FACE_DETECTED = faceDetector.findFaces(faceBitmap, detectedFaces);

        System.out.println(NUMBER_OF_FACE_DETECTED);

        Canvas canvas = new Canvas(faceBitmap);
        canvas.drawBitmap(faceBitmap, 0, 0, null);

        Bitmap hat;
        for (int count = 0; count < NUMBER_OF_FACE_DETECTED; count++) {
            Face face = detectedFaces[count];
            PointF midPoint = new PointF();
            face.getMidPoint(midPoint);
            float eyeDistance = face.eyesDistance();
            //canvas.drawRect(midPoint.x - eyeDistance, midPoint.y - eyeDistance, midPoint.x + eyeDistance, midPoint.y + eyeDistance, myPaint);

            //generate random hat to be placed
            Random rand = new Random();
            int randomNumber = rand.nextInt((15 - 0) + 1) + 0;
            String stringVar = "hat" + randomNumber;
            int resID = getResources().getIdentifier(stringVar, "drawable", MainActivity.this.getPackageName());
            hat = BitmapFactory.decodeResource(getResources(), resID);

            Matrix matrix = new Matrix();
            float scaleWidth = (float) eyeDistance / (float) (hat.getWidth() / 2.8);
            float scaleHeight = (float) eyeDistance / (float) (hat.getHeight() / 2.8);
            matrix.postScale(scaleWidth, scaleHeight);
            matrix.postTranslate(midPoint.x - (eyeDistance + (eyeDistance / 2)), midPoint.y - (int) (eyeDistance * 3.5));
            canvas.drawBitmap(hat, matrix, null);
        }
        return faceBitmap;
    }

    private File getTempFile(Context context) {
        //it will return /sdcard/image.tmp
        final File path = new File(Environment.getExternalStorageDirectory(), context.getPackageName());
        if (!path.exists()) {
            path.mkdir();
        }
        return new File(path, "image.tmp");
    }

    public String getPath(Uri uri) {
        if( uri == null ) {
            return null;
        }
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if( cursor != null ){
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        return uri.getPath();
    }

    public String getImagePathForRotation(Uri uri){
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":")+1);
        cursor.close();

        cursor = getContentResolver().query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        return path;
    }
}
