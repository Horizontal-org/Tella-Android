package rs.readahead.washington.mobile.views.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
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
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.MediaFileUrlLoader;
import rs.readahead.washington.mobile.presentation.entity.MediaFilesData;
import rs.readahead.washington.mobile.presentation.entity.MediaFileLoaderModel;
import rs.readahead.washington.mobile.presentation.entity.ViewType;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.views.interfaces.IAttachmentsMediaHandler;


public class AttachmentsRecycleViewAdapter extends RecyclerView.Adapter<AttachmentsRecycleViewAdapter.ViewHolder> {
    private List<MediaFile> attachments = new ArrayList<>();
    private MediaFileUrlLoader glideLoader;
    private IAttachmentsMediaHandler attachmentsMediaHandler;
    protected ViewType type;


    public AttachmentsRecycleViewAdapter(Context context, IAttachmentsMediaHandler attachmentsMediaHandler,
                                         MediaFileHandler mediaFileHandler, ViewType type) {
        this.glideLoader = new MediaFileUrlLoader(context.getApplicationContext(), mediaFileHandler);
        this.attachmentsMediaHandler = attachmentsMediaHandler;
        this.type = type;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_attachment_media_file, parent, false);
        return new ViewHolder(v, type);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final MediaFile mediaFile = attachments.get(position);

        holder.setRemoveButton();

        holder.setMetadataIcon(mediaFile);

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

        holder.mediaView.setOnClickListener(v -> attachmentsMediaHandler.playMedia(mediaFile));

        holder.removeFile.setOnClickListener(view -> {
            removeAttachment(mediaFile);
            attachmentsMediaHandler.onRemoveAttachment(mediaFile);
        });
    }

    @Override
    public int getItemCount() {
        return attachments.size();
    }

    public void setAttachments(@NonNull List<MediaFile> attachments) {
        this.attachments = attachments;
        notifyDataSetChanged();
    }

    public void prependAttachment(@NonNull MediaFile mediaFile) {
        if (attachments.contains(mediaFile)) {
            return;
        }

        attachments.add(0, mediaFile);
        notifyItemInserted(0);
    }

    public void appendAttachment(@NonNull MediaFile mediaFile) {
        if (attachments.contains(mediaFile)) {
            return;
        }

        attachments.add(mediaFile);
        notifyItemInserted(attachments.size() - 1);
    }

    public void removeAttachment(@NonNull MediaFile mediaFile) {
        int position = attachments.indexOf(mediaFile);

        if (position == -1) {
            return;
        }

        attachments.remove(mediaFile);
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
        @BindView(R.id.mediaView)
        ImageView mediaView;
        @BindView(R.id.videoInfo)
        ViewGroup videoInfo;
        @BindView(R.id.videoDuration)
        TextView videoDuration;
        @BindView(R.id.audioInfo)
        ViewGroup audioInfo;
        @BindView(R.id.audioDuration)
        TextView audioDuration;
        @BindView(R.id.remove_file)
        ImageView removeFile;
        @BindView(R.id.metadata_icon)
        ImageView metadataIcon;

        private ViewType type;

        public ViewHolder(View itemView, ViewType type) {
            super(itemView);
            this.type = type;
            ButterKnife.bind(this, itemView);
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

        void setRemoveButton() {
            removeFile.setVisibility(type == ViewType.PREVIEW ? View.GONE : View.VISIBLE);
        }

        private String getDuration(MediaFile mediaFile) {
            return Util.getShortVideoDuration((int) (mediaFile.getDuration() / 1000));
        }

        void setMetadataIcon(MediaFile mediaFile) {
            if (mediaFile.getMetadata() != null) {
                metadataIcon.setVisibility(View.VISIBLE);
            } else {
                metadataIcon.setVisibility(View.GONE);
            }
        }
    }
}