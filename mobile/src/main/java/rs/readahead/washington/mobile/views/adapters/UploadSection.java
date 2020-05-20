package rs.readahead.washington.mobile.views.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;

import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.FileUploadInstance;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.repository.ITellaUploadsRepository;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.MediaFileUrlLoader;
import rs.readahead.washington.mobile.presentation.entity.MediaFileLoaderModel;
import rs.readahead.washington.mobile.util.Util;

public class UploadSection extends Section {
    private List<MediaFile> files = new ArrayList<>();
    private List<FileUploadInstance> instances = new ArrayList<>();
    private int numberOfUploads;
    private long totalSize;
    private long totalUploaded;
    private boolean isUploadFinished;
    private MediaFileUrlLoader glideLoader;
    private long started;
    private StopUploadListener stopUploadListener;

    /**
     * Create a Section object based on {@link SectionParameters}.
     *
     * @param sectionParameters section parameters
     */
    public UploadSection(@NonNull SectionParameters sectionParameters) {
        super(sectionParameters);
    }

    public UploadSection(Context context, MediaFileHandler mediaFileHandler, @NonNull final List<FileUploadInstance> instances, @NonNull StopUploadListener stopUploadListener) {
        super(SectionParameters.builder()
                .itemResourceId(R.layout.upload_section_item)
                .headerResourceId(R.layout.upload_section_header)
                .footerResourceId(R.layout.upload_section_footer)
                .emptyResourceId(R.layout.upload_empty_layout)
                .failedResourceId(R.layout.upload_empty_layout)
                .build());
        this.glideLoader = new MediaFileUrlLoader(context.getApplicationContext(), mediaFileHandler);
        this.stopUploadListener = stopUploadListener;
        this.instances = instances;
        this.numberOfUploads = instances.size();
        this.isUploadFinished = true;
        this.started = instances.get(0).getStarted();
        for (FileUploadInstance instance : instances) {
            this.files.add(instance.getMediaFile());
            totalSize += instance.getSize();
            totalUploaded += instance.getUploaded();
            if (instance.getStatus() != ITellaUploadsRepository.UploadStatus.UPLOADED) {
                this.isUploadFinished = false;
            }
            if (instance.getStarted() < this.started) {
                this.started = instance.getStarted();
            }
        }
    }

    @Override
    public int getContentItemsTotal() {
        return files.size();
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        return new UploadViewHolder(view);
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder vholder, int position) {
        final MediaFile mediaFile = files.get(position);
        UploadViewHolder holder = (UploadViewHolder) vholder;

        if (mediaFile.getType() == MediaFile.Type.IMAGE) {
            Glide.with(holder.mediaView.getContext())
                    .using(glideLoader)
                    .load(new MediaFileLoaderModel(mediaFile, MediaFileLoaderModel.LoadType.THUMBNAIL))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(holder.mediaView);
        } else if (mediaFile.getType() == MediaFile.Type.AUDIO) {
            Drawable drawable = VectorDrawableCompat.create(holder.itemView.getContext().getResources(),
                    R.drawable.ic_mic_gray, null);
            holder.mediaView.setImageDrawable(drawable);
        } else if (mediaFile.getType() == MediaFile.Type.VIDEO) {
            Glide.with(holder.mediaView.getContext())
                    .using(glideLoader)
                    .load(new MediaFileLoaderModel(mediaFile, MediaFileLoaderModel.LoadType.THUMBNAIL))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(holder.mediaView);
        }
    }

    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(final View view) {
        return new HeaderViewHolder(view);
    }

    @Override
    public void onBindHeaderViewHolder(final RecyclerView.ViewHolder holder) {
        final HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
        String started = holder.itemView.getContext().getResources().getString(R.string.started) + ' ' + Util.getDateTimeString(this.started);
        if (isUploadFinished) {
            headerHolder.title.setText(holder.itemView.getContext().getResources().getQuantityString(R.plurals.files_uploaded, numberOfUploads, numberOfUploads));
            headerHolder.clearHistory.setVisibility(View.GONE);
        } else {
            headerHolder.title.setText(holder.itemView.getContext().getResources().getString(R.string.uploading));
            headerHolder.clearHistory.setVisibility(View.VISIBLE);
            headerHolder.clearHistory.setOnClickListener(v -> stopUploadListener.clearScheduled());
        }
        headerHolder.startedText.setText(started);
    }

    @Override
    public RecyclerView.ViewHolder getFooterViewHolder(final View view) {
        return new FooterViewHolder(view);
    }

    @Override
    public void onBindFooterViewHolder(final RecyclerView.ViewHolder holder) {
        final FooterViewHolder footerHolder = (FooterViewHolder) holder;
        footerHolder.title.setText(holder.itemView.getContext().getResources().getString(R.string.more_details));
    }

    static class UploadViewHolder extends RecyclerView.ViewHolder {
        ImageView mediaView;

        UploadViewHolder(View itemView) {
            super(itemView);
            mediaView = itemView.findViewById(R.id.mediaView);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView startedText;
        final ImageView clearHistory;

        HeaderViewHolder(@NonNull final View view) {
            super(view);

            title = view.findViewById(R.id.header_text);
            startedText = view.findViewById(R.id.started_text);
            clearHistory = view.findViewById(R.id.stop_outlined);
        }
    }

    static class FooterViewHolder extends RecyclerView.ViewHolder {
        final TextView title;

        FooterViewHolder(@NonNull final View view) {
            super(view);

            title = view.findViewById(R.id.footer_text);
        }
    }

    public interface StopUploadListener {
        void clearScheduled();
    }
}
