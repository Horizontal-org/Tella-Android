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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hzontal.tella_vault.VaultFile;
import com.hzontal.utils.MediaFile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.databinding.CardGalleryAttachmentMediaFileBinding;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.views.interfaces.IGalleryMediaHandler;


public class GalleryRecycleViewAdapter extends RecyclerView.Adapter<GalleryRecycleViewAdapter.ViewHolder> {
    private List<VaultFile> files = new ArrayList<>();
    private IGalleryMediaHandler galleryMediaHandler;
    private Set<VaultFile> selected;
    private int cardLayoutId;
    private boolean selectable;
    private boolean singleSelection;


    public GalleryRecycleViewAdapter(Context context, IGalleryMediaHandler galleryMediaHandler, @LayoutRes int cardLayoutId) {
        this(context, galleryMediaHandler, cardLayoutId, true, false);
    }

    public GalleryRecycleViewAdapter(Context context, IGalleryMediaHandler galleryMediaHandler, @LayoutRes int cardLayoutId,
                                     boolean selectable,
                                     boolean singleSelection) {
        this.galleryMediaHandler = galleryMediaHandler;
        this.selected = new LinkedHashSet<>();
        this.cardLayoutId = cardLayoutId;
        this.selectable = selectable;
        this.singleSelection = singleSelection;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardGalleryAttachmentMediaFileBinding itemBinding = CardGalleryAttachmentMediaFileBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(itemBinding, this.selectable);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final VaultFile vaultFile = files.get(position);

        checkItemState(holder, vaultFile);

        holder.maybeShowMetadataIcon(vaultFile);
        if (vaultFile.mimeType == null) return;

        if (MediaFile.INSTANCE.isImageFileType(vaultFile.mimeType)) {
            holder.showImageInfo();
            Glide.with(holder.binding.mediaView.getContext())
                    .load(vaultFile.thumb)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(holder.binding.mediaView);
        } else if (MediaFile.INSTANCE.isAudioFileType(vaultFile.mimeType)) {
            holder.showAudioInfo(vaultFile);
            Drawable drawable = VectorDrawableCompat.create(holder.itemView.getContext().getResources(),
                    R.drawable.ic_mic_gray, null);
            holder.binding.mediaView.setImageDrawable(drawable);
        } else if (MediaFile.INSTANCE.isVideoFileType(vaultFile.mimeType)) {
            holder.showVideoInfo(vaultFile);
            Glide.with(holder.binding.mediaView.getContext())
                    .load(vaultFile.thumb)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(holder.binding.mediaView);
        }

        holder.binding.mediaView.setOnClickListener(v -> galleryMediaHandler.playMedia(vaultFile));

        holder.binding.checkBox.setOnClickListener(v -> checkboxClickHandler(holder, vaultFile));
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

    private void removeAllSelections() {
        for (VaultFile selection : selected) {
            deselectMediaFile(selection);
            galleryMediaHandler.onMediaDeselected(selection);
        }
    }

    private void checkItemState(ViewHolder holder, VaultFile mediaFile) {
        boolean checked = selected.contains(mediaFile);
        holder.binding.selectionDimmer.setVisibility(checked ? View.VISIBLE : View.GONE);
        holder.binding.checkBox.setImageResource(checked ? R.drawable.ic_check_box_on : R.drawable.ic_check_box_off);
    }

    public void setSelectedMediaFiles(@NonNull List<VaultFile> selectedMediaFiles) {
        selected.addAll(selectedMediaFiles);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardGalleryAttachmentMediaFileBinding binding;

        public ViewHolder(CardGalleryAttachmentMediaFileBinding binding, boolean selectable) {
            super(binding.getRoot());
            this.binding = binding;
            maybeEnableCheckBox(selectable);
        }

        void showVideoInfo(VaultFile vaultFile) {
            binding.audioInfo.setVisibility(View.GONE);
            binding.videoInfo.setVisibility(View.VISIBLE);
            if (vaultFile.duration > 0) {
                binding.videoDuration.setText(getDuration(vaultFile));
                binding.videoDuration.setVisibility(View.VISIBLE);
            } else {
                binding.videoDuration.setVisibility(View.INVISIBLE);
            }
        }

        void showAudioInfo(VaultFile vaultFile) {
            binding.videoInfo.setVisibility(View.GONE);
            binding.audioInfo.setVisibility(View.VISIBLE);
            if (vaultFile.duration > 0) {
                binding.audioDuration.setText(getDuration(vaultFile));
                binding.audioDuration.setVisibility(View.VISIBLE);
            } else {
                binding.audioDuration.setVisibility(View.INVISIBLE);
            }
        }

        void showImageInfo() {
            binding.videoInfo.setVisibility(View.GONE);
            binding.audioInfo.setVisibility(View.GONE);
        }

        private String getDuration(VaultFile vaultFile) {
            return Util.getVideoDuration((int) (vaultFile.duration / 1000));
        }

        void maybeShowMetadataIcon(VaultFile vaultFile) {
            if (vaultFile.metadata != null) {
                binding.metadataIcon.setVisibility(View.VISIBLE);
            } else {
                binding.metadataIcon.setVisibility(View.GONE);
            }
        }

        void maybeEnableCheckBox(boolean selectable) {
            binding.checkBox.setEnabled(selectable);
        }
    }
}
