package rs.readahead.washington.mobile.views.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hzontal.tella_vault.VaultFile;
import com.hzontal.utils.MediaFile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.VaultFileUrlLoader;
import rs.readahead.washington.mobile.presentation.entity.VaultFileLoaderModel;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.views.interfaces.IGalleryVaultHandler;


public class GalleryRecycleViewAdapter extends RecyclerView.Adapter<GalleryRecycleViewAdapter.ViewHolder> {
    private List<VaultFile> files = new ArrayList<>();
    private VaultFileUrlLoader glideLoader;
    private IGalleryVaultHandler galleryMediaHandler;
    private Set<VaultFile> selected;
    private int cardLayoutId;
    private boolean selectable;
    private boolean singleSelection;


    public GalleryRecycleViewAdapter(Context context, IGalleryVaultHandler galleryMediaHandler,
                                     MediaFileHandler mediaFileHandler, @LayoutRes int cardLayoutId) {
        this(context, galleryMediaHandler, mediaFileHandler, cardLayoutId, false, false);
    }

    public GalleryRecycleViewAdapter(Context context, IGalleryVaultHandler galleryMediaHandler,
                                     MediaFileHandler mediaFileHandler, @LayoutRes int cardLayoutId,
                                     boolean selectable,
                                     boolean singleSelection) {
        this.glideLoader = new VaultFileUrlLoader(context.getApplicationContext(), mediaFileHandler);
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
        final VaultFile vaultFile = files.get(position);

        checkItemState(holder, vaultFile);
        holder.maybeShowMetadataIcon(vaultFile);
        holder.showFileInfo(vaultFile);
        holder.maybeEnableCheckBox(selectable);
        if (vaultFile.mimeType == null) return;

        if (MediaFile.INSTANCE.isImageFileType(vaultFile.mimeType)) {
            holder.showImageInfo();
            Glide.with(holder.mediaView.getContext())
                    .using(glideLoader)
                    .load(new VaultFileLoaderModel(vaultFile, VaultFileLoaderModel.LoadType.THUMBNAIL))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(holder.mediaView);
        } else if (MediaFile.INSTANCE.isAudioFileType(vaultFile.mimeType)) {
            holder.showAudioInfo(vaultFile);
            Drawable drawable = VectorDrawableCompat.create(holder.itemView.getContext().getResources(),
                    R.drawable.ic_mic_gray, null);
            holder.mediaView.setImageDrawable(drawable);
        } else if (MediaFile.INSTANCE.isVideoFileType(vaultFile.mimeType)) {
            holder.showVideoInfo(vaultFile);
            Glide.with(holder.mediaView.getContext())
                    .using(glideLoader)
                    .load(new VaultFileLoaderModel(vaultFile, VaultFileLoaderModel.LoadType.THUMBNAIL))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(holder.mediaView);
        }

        holder.mediaView.setOnClickListener(v -> galleryMediaHandler.playMedia(vaultFile));

        holder.checkBox.setOnClickListener(v -> checkboxClickHandler(holder, vaultFile));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public void setFiles(List<VaultFile> files) {
        this.files = files;
        notifyDataSetChanged();
    }

    public List<VaultFile> getSelectedMediaFiles() {
        List<VaultFile> selectedList = new ArrayList<>(selected.size());
        selectedList.addAll(selected);

        return selectedList;
    }

    public void clearSelected() {
        selected.clear();
        galleryMediaHandler.onSelectionNumChange(selected.size());
        notifyDataSetChanged();
    }

    public void deselectMediaFile(@NonNull VaultFile vaultFile) {
        if (!selected.contains(vaultFile)) {
            return;
        }

        selected.remove(vaultFile);
        notifyItemChanged(files.indexOf(vaultFile));
    }

    public void selectMediaFile(@NonNull VaultFile vaultFile) {
        if (selected.contains(vaultFile)) {
            return;
        }

        selected.add(vaultFile);
        notifyItemChanged(files.indexOf(vaultFile));
    }

    private void checkboxClickHandler(ViewHolder holder, VaultFile vaultFile) {
        if (selected.contains(vaultFile)) {
            selected.remove(vaultFile);
            galleryMediaHandler.onMediaDeselected(vaultFile);
        } else {
            if (singleSelection) {
                removeAllSelections();
            }

            selected.add(vaultFile);
            galleryMediaHandler.onMediaSelected(vaultFile);
        }

        checkItemState(holder, vaultFile);
        galleryMediaHandler.onSelectionNumChange(selected.size());
    }

    public void enableSelectMode(Boolean selectable){
        this.selectable = selectable;
        notifyDataSetChanged();
    }

    private void removeAllSelections() {
        for (VaultFile selection: selected) {
            deselectMediaFile(selection);
            galleryMediaHandler.onMediaDeselected(selection);
        }
    }

    private void checkItemState(ViewHolder holder, VaultFile mediaFile) {
        boolean checked = selected.contains(mediaFile);
       // holder.selectionDimmer.setVisibility(checked ? View.VISIBLE : View.GONE);
        holder.checkBox.setImageResource(checked ? R.drawable.ic_check_box_on : R.drawable.ic_check_box_off);
    }

    public void setSelectedMediaFiles(@NonNull List<VaultFile> selectedMediaFiles) {
        selected.addAll(selectedMediaFiles);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.attachmentImg)
        ImageView mediaView;
        @BindView(R.id.checkbox_type_single)
        ImageView checkBox;
        @BindView(R.id.fileNameTextView)
        TextView fileName;
      /*  @BindView(R.id.videoInfo)
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
        ImageView metadataIcon;*/


        public ViewHolder(View itemView, boolean selectable) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void showVideoInfo(VaultFile vaultFile) {
            mediaView.setBackgroundResource(R.drawable.ic_play);
        }

        void showAudioInfo(VaultFile vaultFile) {
            mediaView.setBackgroundResource(R.drawable.ic_audio_w_small);
        }

        void showImageInfo() {
           /* videoInfo.setVisibility(View.GONE);
            audioInfo.setVisibility(View.GONE);*/
        }

        private String getDuration(VaultFile vaultFile) {
            return Util.getVideoDuration((int) (vaultFile.duration / 1000));
        }

        void maybeShowMetadataIcon(VaultFile vaultFile) {
          /*  if (vaultFile.metadata != null) {
                metadataIcon.setVisibility(View.VISIBLE);
            } else {
                metadataIcon.setVisibility(View.GONE);
            }*/
        }

        void showFileInfo(VaultFile vaultFile){
            fileName.setText(vaultFile.name);
        }

        void maybeEnableCheckBox(boolean selectable) {
            checkBox.setVisibility(selectable ? View.VISIBLE : View.GONE);
            checkBox.setEnabled(selectable);
        }
    }
}
