package com.example.regulafaceapidemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.ui.AppBarConfiguration;

import com.example.regulafaceapidemo.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.regula.facesdk.FaceSDK;
import com.regula.facesdk.configuration.FaceCaptureConfiguration;
import com.regula.facesdk.enums.ImageType;
import com.regula.facesdk.model.MatchFacesImage;
import com.regula.facesdk.model.results.matchfaces.MatchFacesSimilarityThresholdSplit;
import com.regula.facesdk.request.MatchFacesRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_1 = 1;
    private static final int PICK_IMAGE_2 = 2;


    ImageView imageView1;
    ImageView imageView2;

    Button buttonMatch;
    Button buttonLiveness;
    Button buttonClear;

    TextView textViewSimilarity;
    TextView textViewLiveness;

    Uri imageUri;

    Uri img1URL = null;
    Uri img2URl = null;
    Bitmap image1 = null;
    Bitmap image2 = null;

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    FirebaseDatabase fdb;
    DatabaseReference dbRef;
    StorageReference storRef;
    FirebaseStorage fbStor;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT > 9){
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        fdb = FirebaseDatabase.getInstance();
        dbRef = fdb.getReference("ImageURLs");
        fbStor = FirebaseStorage.getInstance();
        storRef = fbStor.getReference();

        imageView1 = findViewById(R.id.iv1);
        imageView1.getLayoutParams().height = 400;
        imageView2 = findViewById(R.id.iv2);
        imageView2.getLayoutParams().height = 400;
        buttonMatch = findViewById(R.id.buttonMatch);
        buttonClear = findViewById(R.id.buttonClear);
        textViewLiveness = findViewById(R.id.textViewLiveness);
        textViewSimilarity = findViewById(R.id.textViewSimilarity);

        imageView1.setOnClickListener(v -> {
            showMenu(imageView1, PICK_IMAGE_1);
        });
        imageView2.setOnClickListener(v -> {
            showMenu(imageView2, PICK_IMAGE_2);
        });

        buttonClear.setOnClickListener(v -> {
            img1URL = null;
            img2URl = null;
            image1 = null;
            image2 = null;
            imageView1.setImageDrawable(null);
            imageView2.setImageDrawable(null);
            textViewSimilarity.setText("Similarity: Null");

        });

        buttonMatch.setOnClickListener(v -> {
            try {
                URL i1 = new URL(img1URL.toString());
                image1 = BitmapFactory.decodeStream(i1.openConnection().getInputStream());
                URL i2 = new URL(img2URl.toString());
                image2 = BitmapFactory.decodeStream(i2.openConnection().getInputStream());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Bitmap img1 = null;

            if (image1 != null && image2 != null){
                textViewSimilarity.setText("Processing...");

                matchFaces(image1, image2);
                buttonMatch.setEnabled(false);
            }
        });

    }

    private void matchFaces(Bitmap i1, Bitmap i2) {
        List<MatchFacesImage> iList = new ArrayList<>();

        iList.add(new MatchFacesImage(i1, (ImageType) imageView1.getTag(), true));
        iList.add(new MatchFacesImage(i2, (ImageType) imageView2.getTag(), true));

        MatchFacesRequest mReq = new MatchFacesRequest(iList);

        FaceSDK.Instance().matchFaces(mReq, mfRes -> {
            MatchFacesSimilarityThresholdSplit split =
                    new MatchFacesSimilarityThresholdSplit(mfRes.getResults(), 0.75d);

            if (split.getMatchedFaces().size() > 0){
                double sim = split.getMatchedFaces().get(0).getSimilarity();
                textViewSimilarity.setText("Similarity: " + sim*100);
            } else {
                textViewSimilarity.setText("Similarity: null");
            }

            buttonMatch.setEnabled(true);
            buttonClear.setEnabled(true);
        });

        textViewLiveness.setText(img1URL.toString() + "\n" + img2URl.toString());
    }

    private Bitmap getImageBitmap(ImageView v) {
        v.invalidate();
        BitmapDrawable d = (BitmapDrawable) v.getDrawable();
        return d.getBitmap();
    }

    private void showMenu(ImageView v, int i) {
        PopupMenu pMenu = new PopupMenu(MainActivity.this, v);
        pMenu.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()){
                case R.id.gallery:
                    openGallery(i);
                    return true;
                case R.id.camera:
                    startFaceCaptureActivity(v);
                    return true;
                default:
                    return false;
            }
        });
        pMenu.getMenuInflater().inflate(R.menu.menu, pMenu.getMenu());
        pMenu.show();
    }

    private void startFaceCaptureActivity(ImageView v) {
        FaceCaptureConfiguration config = new FaceCaptureConfiguration.Builder().setCameraSwitchEnabled(true).build();

        FaceSDK.Instance().presentFaceCaptureActivity(MainActivity.this, config, res ->{
            if (res.getImage() == null) return;

            v.setImageBitmap(res.getImage().getBitmap());
            v.setTag(ImageType.LIVE);
        });
    }

    private void openGallery(int i) {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, i);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK)
            return;



        imageUri = data.getData();
        textViewSimilarity.setText("Similarity: null");

        ImageView imageView = null;

        if (requestCode == PICK_IMAGE_1)
            imageView = imageView1;
        else if (requestCode == PICK_IMAGE_2)
            imageView = imageView2;

        if (imageView == null)
            return;

        imageView.setImageURI(imageUri);
        imageView.setTag(ImageType.PRINTED);

        Long timestamp = System.currentTimeMillis()/1000;
        String imgID = "IMG" + timestamp.toString();

        StorageReference imgRef = storRef.child("images/" + imgID);

        imgRef.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(MainActivity.this, "Image uploaded", Toast.LENGTH_LONG).show();
                imgRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        if (img1URL == null){
                            img1URL =uri;
                            dbRef.child(imgID).setValue(img1URL.toString());
                        } else {
                            img2URl =uri;
                            dbRef.child(imgID).setValue(img2URl.toString());
                        }

                    }
                });
            }
        });
    }

}