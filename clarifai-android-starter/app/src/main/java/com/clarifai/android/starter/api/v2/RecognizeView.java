package com.clarifai.android.starter.api.v2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import clarifai2.api.ClarifaiClient;
import clarifai2.api.ClarifaiResponse;
import clarifai2.api.request.ClarifaiRequest;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.input.image.ClarifaiImage;
import clarifai2.dto.model.Model;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;
import clarifai2.dto.prediction.Prediction;
import com.clarifai.android.starter.api.v2.activity.BaseActivity;
import com.clarifai.android.starter.api.v2.adapter.PredictionResultsAdapter;
import timber.log.Timber;

import org.apache.commons.lang3.ArrayUtils;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A view that recognizes using the given {@link clarifai2.dto.model.Model} and displays the image that was recognized
 * upon, along with the list of recognitions for that image
 */
public class RecognizeView<PREDICTION extends Prediction> extends CoordinatorLayout implements HandlesPickImageIntent {

  private static final String TAG = "CameraActivity";

  public static final Map<String, String> tagCategories;

  static {
    tagCategories = new HashMap<>();
    tagCategories.put("food paper", "compostable");
    tagCategories.put("yard", "compostable");
    tagCategories.put("food", "compostable");
    tagCategories.put("recycling", "recyclable");
    tagCategories.put("container", "recyclable");
    tagCategories.put("bottle", "recyclable");
    tagCategories.put("paper", "recyclable");
    tagCategories.put("metal", "recyclable");
    tagCategories.put("glass", "recyclable");
    tagCategories.put("plastic", "recyclable");
    tagCategories.put("packaging", "trash");
    tagCategories.put("can", "recyclable");
    tagCategories.put("compost", "compostable");
    tagCategories.put("drink", "recyclable");
  }

  @BindView(R.id.resultsList)
  RecyclerView resultsList;

  @BindView(R.id.image)
  ImageView imageView;

  @BindView(R.id.switcher)
  ViewSwitcher switcher;

  @BindView(R.id.fab)
  View fab;

  @BindView(R.id.resultTrashType)
  TextView resultTrashType;

  CSVParser csv = new CSVParser();

  @Nullable
  private Model<PREDICTION> model;

  @NonNull
  private final PredictionResultsAdapter<PREDICTION> tagsAdapter;

  public RecognizeView(Context context, AttributeSet attrs) {
    super(context, attrs);
    inflate(context, R.layout.view_recognize, this);
    ButterKnife.bind(this);

    tagsAdapter = new PredictionResultsAdapter<>();
    resultsList.setLayoutManager(new LinearLayoutManager(context));
    resultsList.setAdapter(tagsAdapter);
  }

  public void setModel(@NonNull Model<PREDICTION> model) {
    this.model = model;
  }

  @Override
  public void onImagePicked(@NonNull final byte[] imageBytes) {
    final Model<PREDICTION> model = this.model;
    if (model == null) {
      throw new IllegalStateException("An image can't be picked before this view has a model set!");
    }
    setBusy(true);
    tagsAdapter.setData();
    new AsyncTask<Void, Void, List<ClarifaiOutput<PREDICTION>>>() {
      @Override
      protected List<ClarifaiOutput<PREDICTION>> doInBackground(Void... params) {
        final ClarifaiResponse<List<ClarifaiOutput<PREDICTION>>> predictions = model.predict()
            .withInputs(ClarifaiInput.forImage(ClarifaiImage.of(imageBytes)))
            .executeSync();
        final ClarifaiResponse<List<ClarifaiOutput<Prediction>>> predictionResults = App.get().clarifaiClient().predict("ab7e8fef3c3343a88ad5841b8a2975ec")
                .withInputs(ClarifaiInput.forImage(ClarifaiImage.of(imageBytes)))
                .executeSync();
        if (predictions.isSuccessful() && predictionResults.isSuccessful()) {
          List<ClarifaiOutput<PREDICTION>> originalList = predictions.get();
          List<ClarifaiOutput<Prediction>> insertList = predictionResults.get();
          Log.i(TAG, "Entering for loop");
          for(int i = 0; i < insertList.get(0).data().size() - 1; i++) {
            Log.i(TAG, "In For loop");
            originalList.get(0).data().add(0,(PREDICTION)insertList.get(0).data().get(i));
          }
          return originalList;
        } else {
          Timber.e("API call to get predictions was not successful. Info: %s", predictions.getStatus().toString());
          return null;
        }
      }

      @Override
      protected void onPostExecute(List<ClarifaiOutput<PREDICTION>> predictions) {
        setBusy(false);
        if (predictions == null || predictions.isEmpty()) {
          Snackbar.make(
              findViewById(R.id.content_root),
              predictions == null ? R.string.error_while_contacting_api : R.string.no_results_from_api,
              Snackbar.LENGTH_INDEFINITE
          ).show();
          return;
        }
        tagsAdapter.setData(predictions.get(0).data());
        String trashType = "trash";
        boolean paper = false;
        int[] category_counter = new int[3];
        for(int i = 0; i < predictions.get(0).data().size(); i++) {
          Concept concept = predictions.get(0).data().get(i).asConcept();
          if(tagCategories.keySet().contains(concept.name())) {
            if(concept.name().equals("compost") || concept.name().equals("can") || concept.name().equals("packaging")) {
              if(concept.value() > 0.40) {
                trashType = tagCategories.get(concept.name());
                break;
              }
            }
            if(concept.value() > 0.9) {
              trashType = tagCategories.get(concept.name());
              break;
            }
          }
        }
        resultTrashType.setText(trashType);
        imageView.setVisibility(VISIBLE);
        imageView.setImageBitmap(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length));
      }
    }.execute();

  }

  @OnClick(R.id.fab)
  void selectImageToUpload() {
    Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
    ClarifaiUtil.unwrapActivity(getContext())
        .startActivityForResult(intent, BaseActivity.TAKE_PICTURE);
  }

  private void setBusy(final boolean busy) {
    ClarifaiUtil.unwrapActivity(getContext()).runOnUiThread(new Runnable() {
      @Override
      public void run() {
        switcher.setDisplayedChild(busy ? 1 : 0);
        imageView.setVisibility(busy ? GONE : VISIBLE);
        fab.setEnabled(!busy);
      }
    });
  }
}
