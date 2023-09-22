package com.example.appsnake;



import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.appsnake.ml.Model;

import org.tensorflow.lite.DataType;

import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {


    DrawerLayout drawerLayout;
    ImageView menu;
    LinearLayout home, settings, about;


    //add
    TextView result, confidence;
    ImageView imageView;
    Button picture, gallery;
    int imageSize = 224;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    drawerLayout = findViewById(R.id.drawerLayout);
    menu = findViewById(R.id.menu);
    home = findViewById(R.id.home);
    about = findViewById(R.id.about);
    settings = findViewById(R.id.settings);



    //add
     result = findViewById(R.id.result);
     confidence = findViewById(R.id.confidence);
     imageView = findViewById(R.id.imageView);
     picture = findViewById(R.id.button);

     gallery = findViewById(R.id.button2);

     picture.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
             // Launch camera if we have permission
             if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                 Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                 startActivityForResult(cameraIntent, 1);
             } else {
                 //Request camera permission if we don't have it.
                 requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
             }
         }
     });

     //add
     gallery.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
             Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
             startActivityForResult(cameraIntent,1);
         }
     });




    menu.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            openDrawer(drawerLayout);
        }
    });
    home.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            recreate();
        }
    });
    settings.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            redirectActivity(MainActivity.this, SettingsActivity.class);
        }
    });

    about.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            redirectActivity(MainActivity.this, AboutActivity.class);
        }
    });


    }
    public static void openDrawer(DrawerLayout drawerLayout){
        drawerLayout.openDrawer(GravityCompat.START);
    }
    public static void closeDrawer(DrawerLayout drawerLayout){
        if (drawerLayout.isDrawerOpen(GravityCompat.START));{
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }
    public static void redirectActivity(Activity activity, Class secondActivity){
        Intent intent = new Intent(activity, secondActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();

    }


    //add
    public void classifyImage(Bitmap image) {
        try {
            Model model = Model.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4*imageSize*imageSize*3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int [] intValues = new int[imageSize*imageSize];
            image.getPixels(intValues,0,image.getWidth(),0,0,image.getWidth(),image.getHeight());
            int pixel = 0;
            for(int i = 0; i < imageSize; i++){
                for(int j = 0; j < imageSize; j++){
                    int val = intValues[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF)*(1.f/255.f));
                    byteBuffer.putFloat(((val >> 8) & 0xFF)*(1.f/255.f));
                    byteBuffer.putFloat((val & 0xFF)*(1.f/255.f));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            int maxPos = 0;
            float maxConfidence = 0;
            for(int i = 0; i < confidences.length; i++){
                if(confidences[i] > maxConfidence){
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }

            String[] classes = {"Monocled_cobra", "King_cobra","Burmese_python","Python_reticulatus","Mangrove_snake","Ptyas_korros","Copperhead_racer"};

            result.setText(classes[maxPos]);

            String s = "";
            for(int i = 0; i < classes.length; i++){
                s += String.format("%s: %.1f%%\n", classes[i], confidences[i] * 100);
            }

            confidence.setText(s);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == 1) {
            Bitmap imageBitmap = null;
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                try {
                    imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (data != null && data.getExtras() != null) {
                imageBitmap = (Bitmap) data.getExtras().get("data");

            }

            if (imageBitmap != null) {
                // Resize image to 224x224
                Bitmap resizedBitmap = ThumbnailUtils.extractThumbnail(imageBitmap, imageSize, imageSize);

                // Classify image
                classifyImage(resizedBitmap);

                ImageView imageView = findViewById(R.id.imageView);
                imageView.setImageBitmap(resizedBitmap);
            }
        }
    }






    @Override
    protected void onPause() {
        super.onPause();
        closeDrawer(drawerLayout);
    }
}
