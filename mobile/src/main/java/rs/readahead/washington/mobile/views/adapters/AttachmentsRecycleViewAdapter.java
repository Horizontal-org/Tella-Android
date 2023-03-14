package rs.readahead.washington.mobile.views.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hzontal.tella_vault.VaultFile;
import com.hzontal.utils.MediaFile;

import java.util.ArrayList;
import java.util.List;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.presentation.entity.MediaFilesData;
import rs.readahead.washington.mobile.presentation.entity.ViewType;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.views.interfaces.IAttachmentsMediaHandler;
import rs.readahead.washington.mobile.databinding.CardAttachmentMediaFileBinding;


public class AttachmentsRecycleViewAdapter extends RecyclerView.Adapter<AttachmentsRecycleViewAdapter.ViewHolder> {
    private List<VaultFile> attachments = new ArrayList<>();
    private final IAttachmentsMediaHandler attachmentsMediaHandler;
    protected ViewType type;
    private CardAttachmentMediaFileBinding itemBinding;


    public AttachmentsRecycleViewAdapter(Context context, IAttachmentsMediaHandler attachmentsMediaHandler,
                                         MediaFileHandler mediaFileHandler, ViewType type) {
        this.attachmentsMediaHandler = attachmentsMediaHandler;
        this.type = type;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        itemBinding = CardAttachmentMediaFileBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(itemBinding, type);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final VaultFile vaultFile = attachments.get(position);

        holder.setRemoveButton();

        holder.setMetadataIcon(vaultFile);

        if (MediaFile.INSTANCE.isImageFileType(vaultFile.mimeType)) {
            holder.showImageInfo();
            Glide.with(itemBinding.mediaView.getContext())
                    .load(vaultFile.thumb)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(itemBinding.mediaView);
        } else if (MediaFile.INSTANCE.isAudioFileType(vaultFile.mimeType)) {
            holder.showAudioInfo(vaultFile);
            Drawable drawable = VectorDrawableCompat.create(holder.itemView.getContext().getResources(),
                    R.drawable.ic_mic_gray, null);
            itemBinding.mediaView.setImageDrawable(drawable);
        } else if (MediaFile.INSTANCE.isVideoFileType(vaultFile.mimeType)) {
            holder.showVideoInfo(vaultFile);
            Glide.with(itemBinding.mediaView.getContext())
                    .load(vaultFile.thumb)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(itemBinding.mediaView);
        }

        itemBinding.mediaView.setOnClickListener(v -> attachmentsMediaHandler.playMedia(vaultFile));

        itemBinding.removeFile.setOnClickListener(view -> {
            removeAttachment(vaultFile);
            attachmentsMediaHandler.onRemoveAttachment(vaultFile);
        });
    }

    @Override
    public int getItemCount() {
        return attachments.size();
    }

    public void setAttachments(@NonNull List<VaultFile> attachments) {
        this.attachments = attachments;
        notifyDataSetChanged();
    }

    public void prependAttachment(@NonNull VaultFile vaultFile) {
        if (attachments.contains(vaultFile)) {
            return;
        }

        attachments.add(0, vaultFile);
        notifyItemInserted(0);
    }

    public void appendAttachment(@NonNull VaultFile vaultFile) {
        if (attachments.contains(vaultFile)) {
            return;
        }

        attachments.add(vaultFile);
        notifyItemInserted(attachments.size() - 1);
    }

    public void removeAttachment(@NonNull VaultFile vaultFile) {
        int position = attachments.indexOf(vaultFile);

        if (position == -1) {
            return;
        }

        attachments.remove(vaultFile);
        notifyItemRemoved(position);
    }

    public MediaFilesData getAttachments() {
        return new MediaFilesData(attachments);
    }

    public void clearAttachments() {
        attachments.clear();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardAttachmentMediaFileBinding itemBinding;

        private final ViewType type;

        public ViewHolder(CardAttachmentMediaFileBinding itemBinding, ViewType type) {
            super(itemBinding.getRoot());
            this.type = type;
            this.itemBinding = itemBinding;
        }

        void showVideoInfo(VaultFile vaultFile) {
            itemBinding.audioInfo.setVisibility(View.GONE);
            itemBinding.videoInfo.setVisibility(View.VISIBLE);
            if (vaultFile.duration > 0) {
                itemBinding.videoDuration.setText(getDuration(vaultFile));
                itemBinding.videoDuration.setVisibility(View.VISIBLE);
            } else {
                itemBinding.videoDuration.setVisibility(View.INVISIBLE);
            }
        }

        void showAudioInfo(VaultFile vaultFile) {
            itemBinding.videoInfo.setVisibility(View.GONE);
            itemBinding.audioInfo.setVisibility(View.VISIBLE);
            if (vaultFile.duration > 0) {
                itemBinding.audioDuration.setText(getDuration(vaultFile));
                itemBinding.audioDuration.setVisibility(View.VISIBLE);
            } else {
                itemBinding.audioDuration.setVisibility(View.INVISIBLE);
            }
        }

        void showImageInfo() {
            itemBinding.videoInfo.setVisibility(View.GONE);
            itemBinding.audioInfo.setVisibility(View.GONE);
        }

        void setRemoveButton() {
            itemBinding.removeFile.setVisibility(type == ViewType.PREVIEW ? View.GONE : View.VISIBLE);
        }

        private String getDuration(VaultFile vaultFile) {
            return Util.getShortVideoDuration((int) (vaultFile.duration / 1000));
        }

        void setMetadataIcon(VaultFile vaultFile) {
            if (vaultFile.metadata != null) {
                itemBinding.metadataIcon.setVisibility(View.VISIBLE);
            } else {
                itemBinding.metadataIcon.setVisibility(View.GONE);
            }
        }
    }
}