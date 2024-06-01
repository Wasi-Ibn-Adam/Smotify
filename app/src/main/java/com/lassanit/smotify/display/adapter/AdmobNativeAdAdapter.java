package com.lassanit.smotify.display.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.lassanit.smotify.R;
import com.lassanit.smotify.bases.SharedBase;
import com.lassanit.smotify.classes.RecyclerViewAdapterWrapper;

public class AdmobNativeAdAdapter extends RecyclerViewAdapterWrapper {

    public static final String TYPE_SMALL = "small";
    public static final String TYPE_MEDIUM = "medium";

    private static final int TYPE_FB_NATIVE_ADS = 900;
    private static final int DEFAULT_AD_ITEM_INTERVAL = 4;


    private final Param mParam;

    private AdmobNativeAdAdapter(Param param) {
        super(param.adapter);
        this.mParam = param;

        assertConfig();
        setSpanAds();
    }

    private void assertConfig() {
        if (mParam.gridLayoutManager != null) {
            //if user set span ads
            int nCol = mParam.gridLayoutManager.getSpanCount();
            if (mParam.adItemInterval % nCol != 0) {
                throw new IllegalArgumentException(String.format("The adItemInterval (%d) is not divisible by number of columns in GridLayoutManager (%d)", mParam.adItemInterval, nCol));
            }
        }
    }

    private int convertAdPosition2OrgPosition(int position) {
        return position - (position + 1) / (mParam.adItemInterval + 1);
    }




    @Override
    public int getItemCount() {
        try{
            int realCount = super.getItemCount();
            return realCount + realCount / mParam.adItemInterval;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return super.getItemCount();
    }

    @Override
    public int getItemViewType(int position) {
        try{
            if (isAdPosition(position)) {
                return TYPE_FB_NATIVE_ADS;
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return super.getItemViewType(convertAdPosition2OrgPosition(position));
    }

    private boolean isAdPosition(int position) {
        /*if(position==1|| position==4)return true;*/
        if (!SharedBase.Ads.INSTANCE.isActive())
            return false;
        return (position + 1) % (mParam.adItemInterval + 1) == 0;
    }

    private void onBindAdViewHolder(final RecyclerView.ViewHolder holder) {
        try {
            final AdViewHolder adHolder = (AdViewHolder) holder;
            if (mParam.forceReloadAdOnBind || !adHolder.loaded) {
                AdLoader adLoader = new AdLoader.Builder(adHolder.getContext(), mParam.admobNativeId)
                        .forNativeAd(NativeAd -> {
                        /*NativeTemplateStyle styles = new
                                NativeTemplateStyle.Builder().withMainBackgroundColor().build();*/

                            //adHolder.template.setStyles(styles);

                            Log.e("admobnative", "loaded");
                            NativeTemplateStyle.Builder builder = new NativeTemplateStyle.Builder();
                            builder.withPrimaryTextSize(11f);
                            builder.withSecondaryTextSize(10f);
                            builder.withTertiaryTextSize(06f);
                            builder.withCallToActionTextSize(11f);

                            if (mParam.layout == 0) {
                                adHolder.templatesmall.setVisibility(View.VISIBLE);
                                adHolder.templatesmall.setStyles(builder.build());
                                adHolder.templatesmall.setNativeAd(NativeAd);
                            } else if (mParam.layout == 1) {
                                adHolder.templatemedium.setVisibility(View.VISIBLE);
                                adHolder.templatemedium.setStyles(builder.build());
                                adHolder.templatemedium.setNativeAd(NativeAd);

                            } else {
                                adHolder.templatecustom.setVisibility(View.VISIBLE);
                                adHolder.templatecustom.setStyles(builder.build());
                                adHolder.templatecustom.setNativeAd(NativeAd);
                            }

                            adHolder.loaded = true;

                        })
                        .withAdListener(new AdListener() {
                            @Override
                            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                                super.onAdFailedToLoad(loadAdError);
                                Log.e("admobnative", "error:" + loadAdError);
                                adHolder.adContainer.setVisibility(View.GONE);
                                // Handle the failure by logging, altering the UI, and so on.

                            }


                        })
                        .withNativeAdOptions(new NativeAdOptions.Builder()
                                // Methods in the NativeAdOptions.Builder class can be
                                // used here to specify individual options settings.
                                .build())
                        .build();
                adLoader.loadAd(new AdRequest.Builder().build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        try {
            if (getItemViewType(position) == TYPE_FB_NATIVE_ADS) {
                onBindAdViewHolder(holder);
            } else {
                super.onBindViewHolder(holder, convertAdPosition2OrgPosition(position));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private RecyclerView.ViewHolder onCreateAdViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View adLayoutOutline = inflater
                .inflate(mParam.itemContainerLayoutRes, parent, false);
        ViewGroup vg = adLayoutOutline.findViewById(mParam.itemContainerId);

        LinearLayout adLayoutContent = (LinearLayout) inflater
                .inflate(R.layout.item_admob_native_ad, parent, false);
        vg.addView(adLayoutContent);
        return new AdViewHolder(adLayoutOutline);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_FB_NATIVE_ADS) {
            return onCreateAdViewHolder(parent);
        }
        return super.onCreateViewHolder(parent, viewType);
    }


    private void setSpanAds() {
        try {
            if (mParam.gridLayoutManager == null) {
                return;
            }
            final GridLayoutManager.SpanSizeLookup spl = mParam.gridLayoutManager.getSpanSizeLookup();
            mParam.gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    try {
                        if (isAdPosition(position)) {
                            return spl.getSpanSize(position);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return 1;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static class Param {
        String admobNativeId;
        RecyclerView.Adapter adapter;
        int adItemInterval;
        boolean forceReloadAdOnBind;

        int layout;

        @LayoutRes
        int itemContainerLayoutRes;

        @IdRes
        int itemContainerId;

        GridLayoutManager gridLayoutManager;
    }

    public static class Builder {
        private final Param mParam;

        private Builder(Param param) {
            mParam = param;
        }

        @NonNull
        public static Builder with(String placementId, RecyclerView.Adapter wrapped, String layout) {
            Param param = new Param();
            param.admobNativeId = placementId;
            param.adapter = wrapped;


            if (layout.toLowerCase().equals("small")) {
                param.layout = 0;
            } else if (layout.toLowerCase().equals("medium")) {

                param.layout = 1;
            } else {
                param.layout = 2;

            }

            //default value
            param.adItemInterval = DEFAULT_AD_ITEM_INTERVAL;
            param.itemContainerLayoutRes = R.layout.item_admob_native_ad_outline;
            param.itemContainerId = R.id.ad_container;
            param.forceReloadAdOnBind = true;
            return new Builder(param);
        }

        public Builder adItemInterval(int interval) {
            mParam.adItemInterval = interval;
            return this;
        }

        public Builder adLayout(@LayoutRes int layoutContainerRes, @IdRes int itemContainerId) {
            mParam.itemContainerLayoutRes = layoutContainerRes;
            mParam.itemContainerId = itemContainerId;
            return this;
        }

        public AdmobNativeAdAdapter build() {
            return new AdmobNativeAdAdapter(mParam);
        }

        public Builder enableSpanRow(GridLayoutManager layoutManager) {
            mParam.gridLayoutManager = layoutManager;
            return this;
        }

        public Builder adItemIterval(int i) {
            mParam.adItemInterval = i;
            return this;
        }

        public Builder forceReloadAdOnBind(boolean forced) {
            mParam.forceReloadAdOnBind = forced;
            return this;
        }
    }

    private static class AdViewHolder extends RecyclerView.ViewHolder {

        TemplateView templatesmall, templatemedium, templatecustom;
        LinearLayout adContainer;
        boolean loaded;

        AdViewHolder(View view) {
            super(view);
            templatesmall = view.findViewById(R.id.my_templatesmall);
            templatecustom = view.findViewById(R.id.my_templatecustom);
            templatemedium = view.findViewById(R.id.my_templatemedium);
            loaded = false;
            adContainer = (LinearLayout) view.findViewById(R.id.native_ad_container);
        }

        Context getContext() {
            return adContainer.getContext();
        }

    }
}
