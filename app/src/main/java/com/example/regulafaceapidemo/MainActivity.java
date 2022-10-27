package com.example.regulafaceapidemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.ui.AppBarConfiguration;

import com.example.regulafaceapidemo.databinding.ActivityMainBinding;
import com.regula.facesdk.FaceSDK;
import com.regula.facesdk.configuration.FaceCaptureConfiguration;
import com.regula.facesdk.enums.ImageType;
import com.regula.facesdk.model.MatchFacesImage;
import com.regula.facesdk.model.results.matchfaces.MatchFacesSimilarityThresholdSplit;
import com.regula.facesdk.request.MatchFacesRequest;

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

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView1 = findViewById(R.id.iv1);
        imageView1.getLayoutParams().height = 400;
        imageView2 = findViewById(R.id.iv2);
        imageView2.getLayoutParams().height = 400;
        buttonMatch = findViewById(R.id.buttonMatch);
        buttonClear = findViewById(R.id.buttonClear);

        textViewSimilarity = findViewById(R.id.textViewSimilarity);

        imageView1.setOnClickListener(v -> {
            showMenu(imageView1, PICK_IMAGE_1);
        });
        imageView2.setOnClickListener(v -> {
            showMenu(imageView2, PICK_IMAGE_2);
        });

        buttonClear.setOnClickListener(v -> {
            imageView1.setImageDrawable(null);
            imageView2.setImageDrawable(null);
            textViewSimilarity.setText("Similarity: Null");
        });

        buttonMatch.setOnClickListener(v -> {
            if (imageView1.getDrawable() != null && imageView2.getDrawable() != null){
                textViewSimilarity.setText("Processing...");

                matchFaces(getImageBitmap(imageView1), getImageBitmap(imageView2));
                buttonMatch.setEnabled(false);
            }
        });

        FaceSDK.Instance().setServiceUrl("");

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
    }

}