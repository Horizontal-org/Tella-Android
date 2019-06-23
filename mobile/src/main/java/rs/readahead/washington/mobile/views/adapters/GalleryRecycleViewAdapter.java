package rs.readahead.washington.mobile.views.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.MediaFileUrlLoader;
import rs.readahead.washington.mobile.presentation.entity.MediaFileLoaderModel;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.views.interfaces.IGalleryMediaHandler;


public class GalleryRecycleViewAdapter extends RecyclerView.Adapter<GalleryRecycleViewAdapter.ViewHolder> {
    private List<MediaFile> files = new ArrayList<>();
    private MediaFileUrlLoader glideLoader;
    private IGalleryMediaHandler galleryMediaHandler;
    private Set<MediaFile> selected;
    private int cardLayoutId;
    private boolean selectable;
    private boolean singleSelection;


    public GalleryRecycleViewAdapter(Context context, IGalleryMediaHandler galleryMediaHandler,
                                     MediaFileHandler mediaFileHandler, @LayoutRes int cardLayoutId) {
        this(context, galleryMediaHandler, mediaFileHandler, cardLayoutId, true, false);
    }

    public GalleryRecycleViewAdapter(Context context, IGalleryMediaHandler galleryMediaHandler,
                                     MediaFileHandler mediaFileHandler, @LayoutRes int cardLayoutId,
                                     boolean selectable,
                                     boolean singleSelection) {
        this.glideLoader = new MediaFileUrlLoader(context.getApplicationContext(), mediaFileHandler);
        this.galleryMediaHandler = galleryMediaHandler;
        this.selected = new LinkedHashSet<>();
        this.cardLayoutId = cardLayoutId;
        this.selectable = selectable;
        this.singleSelection = singleSelection;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(cardLayoutId, parent,false);
        return new ViewHolder(v, this.selectable);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final MediaFile mediaFile = files.get(position);

        checkItemState(holder, mediaFile);

        holder.maybeShowMetadataIcon(mediaFile);

        if (mediaFile.getType() == MediaFile.Type.IMAGE) {
            holder.showImageInfo();
            Glide.with(holder.mediaView.getContext())
                    .using(glideLoader)
                    .load(new MediaFileLoaderModel(mediaFile, MediaFileLoaderModel.LoadType.THUMBNAIL))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(holder.mediaView);
        } else if (mediaFile.getType() == MediaFile.Type.AUDIO) {
            holder.showAudioInfo(mediaFile);
            Drawable drawable = VectorDrawableCompat.create(holder.itemView.getContext().getResources(),
                    R.drawable.ic_mic_gray, null);
            holder.mediaView.setImageDrawable(drawable);
        } else if (mediaFile.getType() == MediaFile.Type.VIDEO) {
            holder.showVideoInfo(mediaFile);
            Glide.with(holder.mediaView.getContext())
                    .using(glideLoader)
                    .load(new MediaFileLoaderModel(mediaFile, MediaFileLoaderModel.LoadType.THUMBNAIL))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(holder.mediaView);
        }

        holder.mediaView.setOnClickListener(v -> galleryMediaHandler.playMedia(mediaFile));

        holder.checkBox.setOnClickListener(v -> checkboxClickHandler(holder, mediaFile));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public void setFiles(List<MediaFile> files) {
        this.files = files;
        notifyDataSetChanged();
    }

    public List<MediaFile> getSelectedMediaFiles() {
        List<MediaFile> selectedList = new ArrayList<>(selected.size());
        selectedList.addAll(selected);

        return selectedList;
    }

    public void clearSelected() {
        selected.clear();
        galleryMediaHandler.onSelectionNumChange(selected.size());
        notifyDataSetChanged();
    }

    public void deselectMediaFile(@NonNull MediaFile mediaFile) {
        if (!selected.contains(mediaFile)) {
            return;
        }

        selected.remove(mediaFile);
        notifyItemChanged(files.indexOf(mediaFile));
    }

    public void selectMediaFile(@NonNull MediaFile mediaFile) {
        if (selected.contains(mediaFile)) {
            return;
        }

        selected.add(mediaFile);
        notifyItemChanged(files.indexOf(mediaFile));
    }

    private void checkboxClickHandler(ViewHolder holder, MediaFile mediaFile) {
        if (selected.contains(mediaFile)) {
            selected.remove(mediaFile);
            galleryMediaHandler.onMediaDeselected(mediaFile);
        } else {
            if (singleSelection) {
                removeAllSelections();
            }

            selected.add(mediaFile);
            galleryMediaHandler.onMediaSelected(mediaFile);
        }

        checkItemState(holder, mediaFile);
        galleryMediaHandler.onSelectionNumChange(selected.size());
    }

    private void removeAllSelections() {
        for (MediaFile selection: selected) {
            deselectMediaFile(selection);
            galleryMediaHandler.onMediaDeselected(selection);
        }
    }

    private void checkItemState(ViewHolder holder, MediaFile mediaFile) {
        boolean checked = selected.contains(mediaFile);
        holder.selectionDimmer.setVisibility(checked ? View.VISIBLE : View.GONE);
        holder.checkBox.setImageResource(checked ? R.drawable.ic_check_box_on : R.drawable.ic_check_box_off);
    }

    public void setSelectedMediaFiles(@NonNull List<MediaFile> selectedMediaFiles) {
        selected.addAll(selectedMediaFiles);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.mediaView)
        ImageView mediaView;
        @BindView(R.id.checkBox)
        ImageView checkBox;
        @BindView(R.id.videoInfo)
        ViewGroup videoInfo;
        @BindView(R.id.videoDuration)
        TextView videoDuration;
        @BindView(R.id.audioInfo)
        ViewGroup audioInfo;
        @BindView(R.id.audioDuration)
        TextView audioDuration;
        @BindView(R.id.selectionDimmer)
        View selectionDimmer;
        @BindView(R.id.checkboxOuter)
        View checkboxOuter;
        @BindView(R.id.metadata_icon)
        ImageView metadataIcon;


        public ViewHolder(View itemView, boolean selectable) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            maybeEnableCheckBox(selectable);
        }

        void showVideoInfo(MediaFile mediaFile) {
            audioInfo.setVisibility(View.GONE);
            videoInfo.setVisibility(View.VISIBLE);
            if (mediaFile.getDuration() > 0) {
                videoDuration.setText(getDuration(mediaFile));
                videoDuration.setVisibility(View.VISIBLE);
            } else {
                videoDuration.setVisibility(View.INVISIBLE);
            }
        }

        void showAudioInfo(MediaFile mediaFile) {
            videoInfo.setVisibility(View.GONE);
            audioInfo.setVisibility(View.VISIBLE);
            if (mediaFile.getDuration() > 0) {
                audioDuration.setText(getDuration(mediaFile));
                audioDuration.setVisibility(View.VISIBLE);
            } else {
                audioDuration.setVisibility(View.INVISIBLE);
            }
        }

        void showImageInfo() {
            videoInfo.setVisibility(View.GONE);
            audioInfo.setVisibility(View.GONE);
        }

        private String getDuration(MediaFile mediaFile) {
            return Util.getVideoDuration((int) (mediaFile.getDuration() / 1000));
        }

        void maybeShowMetadataIcon(MediaFile mediaFile) {
            if (mediaFile.getMetadata() != null) {
                metadataIcon.setVisibility(View.VISIBLE);
            } else {
                metadataIcon.setVisibility(View.GONE);
            }
        }

        void maybeEnableCheckBox(boolean selectable) {
            checkBox.setEnabled(selectable);
        }
    }
}
