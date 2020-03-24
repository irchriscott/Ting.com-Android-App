package com.codepipes.ting.imageeditor.editimage.fragment;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.codepipes.ting.R;
import com.codepipes.ting.dialogs.messages.ProgressOverlay;
import com.codepipes.ting.dialogs.messages.TingToast;
import com.codepipes.ting.dialogs.messages.TingToastType;
import com.codepipes.ting.imageeditor.BaseActivity;
import com.codepipes.ting.imageeditor.editimage.EditImageActivity;
import com.codepipes.ting.imageeditor.editimage.ModuleConfig;
import com.codepipes.ting.imageeditor.editimage.adapter.FilterAdapter;
import com.codepipes.ting.imageeditor.editimage.view.imagezoom.ImageViewTouchBase;

import java.util.Objects;

import iamutkarshtiwari.github.io.ananas.editimage.fliter.PhotoProcessing;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class FilterListFragment extends BaseEditFragment {

    public static final int INDEX = ModuleConfig.INDEX_FILTER;
    public static final int NULL_FILTER_INDEX = 0;
    public static final String TAG = FilterListFragment.class.getName();

    private View mainView;
    private Bitmap filterBitmap;
    private Bitmap currentBitmap;
    private ProgressOverlay loadingDialog;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    public static FilterListFragment newInstance() {
        return new FilterListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.image_editor_fragment_edit_image_fliter, null);
        loadingDialog = BaseActivity.getLoadingDialog(getActivity(), R.string.image_editor_loading,
                false);
        return mainView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        RecyclerView filterRecyclerView = mainView.findViewById(R.id.filter_recycler);
        FilterAdapter filterAdapter = new FilterAdapter(this, getContext());
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        filterRecyclerView.setLayoutManager(layoutManager);
        filterRecyclerView.setAdapter(filterAdapter);

        View backBtn = mainView.findViewById(R.id.back_to_main);
        backBtn.setOnClickListener(v -> backToMain());
    }

    @Override
    public void onShow() {
        activity.mode = EditImageActivity.MODE_FILTER;
        activity.filterListFragment.setCurrentBitmap(activity.getMainBit());
        activity.mainImage.setImageBitmap(activity.getMainBit());
        activity.mainImage.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);
        activity.mainImage.setScaleEnabled(false);
        activity.bannerFlipper.showNext();
    }

    @Override
    public void backToMain() {
        currentBitmap = activity.getMainBit();
        filterBitmap = null;
        activity.mainImage.setImageBitmap(activity.getMainBit());
        activity.mode = EditImageActivity.MODE_NONE;
        activity.bottomGallery.setCurrentItem(0);
        activity.mainImage.setScaleEnabled(true);
        activity.bannerFlipper.showPrevious();
    }

    public void applyFilterImage() {
        if (currentBitmap == activity.getMainBit()) {
            backToMain();
        } else {
            activity.changeMainBitmap(filterBitmap, true);
            backToMain();
        }
    }

    @Override
    public void onPause() {
        compositeDisposable.clear();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        tryRecycleFilterBitmap();
        compositeDisposable.dispose();
        super.onDestroy();
    }

    private void tryRecycleFilterBitmap() {
        if (filterBitmap != null && (!filterBitmap.isRecycled())) {
            filterBitmap.recycle();
        }
    }

    public void enableFilter(int filterIndex) {
        if (filterIndex == NULL_FILTER_INDEX) {
            activity.mainImage.setImageBitmap(activity.getMainBit());
            currentBitmap = activity.getMainBit();
            return;
        }

        compositeDisposable.clear();

        Disposable applyFilterDisposable = applyFilter(filterIndex)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(subscriber -> loadingDialog.show(getFragmentManager(), loadingDialog.getTag()))
                .doFinally(() -> loadingDialog.dismiss())
                .subscribe(
                        this::updatePreviewWithFilter,
                        e -> showSaveErrorToast()
                );

        compositeDisposable.add(applyFilterDisposable);
    }

    private void updatePreviewWithFilter(Bitmap bitmapWithFilter) {
        if (bitmapWithFilter == null) return;

        if (filterBitmap != null && (!filterBitmap.isRecycled())) {
            filterBitmap.recycle();
        }

        filterBitmap = bitmapWithFilter;
        activity.mainImage.setImageBitmap(filterBitmap);
        currentBitmap = filterBitmap;
    }

    private void showSaveErrorToast() {
        TingToast tingToast = new TingToast(Objects.requireNonNull(getContext()), "Save Error", TingToastType.ERROR);
        tingToast.showToast(Toast.LENGTH_LONG);
    }

    private Single<Bitmap> applyFilter(int filterIndex) {
        return Single.fromCallable(() -> {

            Bitmap srcBitmap = Bitmap.createBitmap(activity.getMainBit().copy(
                    Bitmap.Config.RGB_565, true));
            return PhotoProcessing.filterPhoto(srcBitmap, filterIndex);
        });
    }

    public void setCurrentBitmap(Bitmap currentBitmap) {
        this.currentBitmap = currentBitmap;
    }
}
