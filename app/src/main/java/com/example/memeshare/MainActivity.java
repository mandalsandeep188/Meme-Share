package com.example.memeshare;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private Api api;
    Retrofit retrofit;
    ImageView memeImage;
    Button nextButton;
    ImageButton shareButton;
    Button prevButton;
    Toolbar toolbar;
    ProgressBar progressBar;
    Bitmap bitmap;
    ArrayList<Bitmap> memes;
    int i;
    int size;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        retrofit = new Retrofit.Builder()
                .baseUrl(Api.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(Api.class);
        memeImage = findViewById(R.id.meme);
        nextButton = findViewById(R.id.next);
        shareButton = findViewById(R.id.share);
        prevButton = findViewById(R.id.prev);
        progressBar = findViewById(R.id.progressBar);
        memes = new ArrayList<>();
        i = 0;
        fetchMeme();

        nextButton.setOnClickListener(view -> fetchMeme());
        prevButton.setOnClickListener(view -> prevMeme());
        shareButton.setOnClickListener(view -> shareMeme());


    }

    //fetching meme using Retrofit
    public void fetchMeme(){
        //Checking if new meme is need to fetch
        if(i+1 < memes.size() && memes.get(i+1) != null && !memes.get(i+1).isRecycled()){
            Bitmap b = memes.get(i+1);
            memeImage.setImageBitmap(b);
            bitmap = b;
            i++;
            return;
        }
        Call<Meme> call = api.getMeme();

        //fetching new meme
        call.enqueue(new Callback<Meme>() {
            @Override
            public void onResponse(Call<Meme> call, Response<Meme> response) {
                Meme meme = response.body();
                assert meme != null;
                progressBar.setVisibility(View.VISIBLE);
                //Using Glide to load resource from url
                Glide.with(MainActivity.this)
                        .load(meme.url)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                Toast.makeText(MainActivity.this, "Error! Retry", Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.INVISIBLE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                progressBar.setVisibility(View.INVISIBLE);
                                bitmap = drawableToBitmap(resource);
                                memes.add(bitmap);
                                size = memes.size();
                                i=size-1;
                                return false;
                            }
                        })
                        .into(memeImage);
            }

            @Override
            public void onFailure(Call<Meme> call, Throwable t) {
                Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Fetching previously shown meme if it is not recycled
    private void prevMeme() {
        if(i-1 >=0 && memes.get(i-1) != null && !memes.get(i-1).isRecycled()){
            Bitmap b = memes.get(i-1);
            memeImage.setImageBitmap(b);
            bitmap = b;
            i--;
        }
        else{
            Toast.makeText(this, "No more previous meme!", Toast.LENGTH_SHORT).show();
        }
    }

    //Sharing meme by Intent
    private void shareMeme() {
        try{
            //Preparing image file to share
            File file = new File(this.getExternalCacheDir(),"meme.jpg");
            FileOutputStream fOut = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,fOut);
            fOut.flush();
            fOut.close();
            file.setReadable(true,false);
            Uri imageUri = FileProvider.getUriForFile(
                    MainActivity.this,
                    "com.example.memeshare.provider", //(use your app signature + ".provider" )
                    file);
            //Share Intent
            final Intent share = new Intent(Intent.ACTION_SEND);
            share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            share.putExtra(Intent.EXTRA_TEXT,"Check out this meme!");
            share.putExtra(Intent.EXTRA_STREAM, imageUri);
            share.setType("image/*");
            startActivity(Intent.createChooser(share,"Share meme via"));
        }
        catch (Exception e){
            Log.d("MainActivity", "shareMeme: "+e.getMessage());
        }
    }

    //Convert downloaded image from Glide to shareable bitmap format
    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}