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
    private List<FileUploadInstance> instances;
    private int numberOfUploads;
    private long totalSize;
    private long totalUploaded;
    private boolean isUploadFinished;
    private boolean expanded = false;
    private MediaFileUrlLoader glideLoader;
    private long started;
    private long set;
    private UploadSectionListener uploadSectionListener;
    private FooterViewHolder footer;

    public UploadSection(Context context, MediaFileHandler mediaFileHandler, @NonNull final List<FileUploadInstance> instances, @NonNull UploadSectionListener uploadSectionListener, Long set) {
        super(SectionParameters.builder()
                .itemResourceId(R.layout.upload_section_item)
                .headerResourceId(R.layout.upload_section_header)
                .footerResourceId(R.layout.upload_section_footer)
                .emptyResourceId(R.layout.upload_empty_layout)
                .failedResourceId(R.layout.upload_empty_layout)
                .build());
        this.glideLoader = new MediaFileUrlLoader(context.getApplicationContext(), mediaFileHandler);
        this.uploadSectionListener = uploadSectionListener;
        this.instances = instances;
        this.set = set;
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
        return expanded ? instances.size() : 0;
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        return new UploadViewHolder(view);
    }

    @Override
    public void onBindItemViewHolder(final RecyclerView.ViewHolder vholder, int position) {
        final MediaFile mediaFile = files.get(position);
        UploadViewHolder itemHolder = (UploadViewHolder) vholder;

        if (mediaFile.getType() == MediaFile.Type.IMAGE || mediaFile.getType() == MediaFile.Type.VIDEO) {
            Glide.with(itemHolder.mediaView.getContext())
                    .using(glideLoader)
                    .load(new MediaFileLoaderModel(mediaFile, MediaFileLoaderModel.LoadType.THUMBNAIL))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(itemHolder.mediaView);
        } else if (mediaFile.getType() == MediaFile.Type.AUDIO) {
            Drawable drawable = VectorDrawableCompat.create(itemHolder.itemView.getContext().getResources(),
                    R.drawable.ic_mic_gray, null);
            itemHolder.mediaView.setImageDrawable(drawable);
        }

        itemHolder.itemView.setOnClickListener(v ->
                uploadSectionListener.onItemRootViewClicked(this, itemHolder.getAdapterPosition())
        );
    }

    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(final View view) {
        return new HeaderViewHolder(view);
    }

    @Override
    public void onBindHeaderViewHolder(final RecyclerView.ViewHolder holder) {
        final HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
        String started = holder.itemView.getContext().getResources().getString(R.string.started) + ": " + Util.getDateTimeString(this.started, "dd/MM/yyyy h:mm a");
        if (isUploadFinished) {
            headerHolder.title.setText(holder.itemView.getContext().getResources().getQuantityString(R.plurals.files_uploaded, numberOfUploads, numberOfUploads));
            headerHolder.clearHistory.setVisibility(View.GONE);
        } else {
            headerHolder.title.setText(holder.itemView.getContext().getResources().getString(R.string.uploading));
            headerHolder.clearHistory.setVisibility(View.VISIBLE);
            headerHolder.clearHistory.setOnClickListener(v -> uploadSectionListener.clearScheduled());
        }
        headerHolder.startedText.setText(started);

        headerHolder.itemView.setOnClickListener(v -> {
                    uploadSectionListener.onHeaderRootViewClicked(this);
                    toggleFooter();
                }
        );
    }

    @Override
    public RecyclerView.ViewHolder getFooterViewHolder(final View view) {
        return new FooterViewHolder(view);
    }

    @Override
    public void onBindFooterViewHolder(final RecyclerView.ViewHolder holder) {
        final FooterViewHolder footerHolder = (FooterViewHolder) holder;
        this.footer = footerHolder;
        footerHolder.fTitle.setText(holder.itemView.getContext().getResources().getString(R.string.more_details));
        footerHolder.fTitle.setOnClickListener(v -> uploadSectionListener.showUploadInformation(this.set));
        footerHolder.fTitle.setVisibility(View.GONE);
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
        final TextView fTitle;

        FooterViewHolder(@NonNull final View view) {
            super(view);

            this.fTitle = view.findViewById(R.id.footer_text);
        }
    }

    private void toggleFooter() {
        footer.fTitle.setVisibility(footer.fTitle.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(final boolean expanded) {
        this.expanded = expanded;
    }

    public interface UploadSectionListener {
        void clearScheduled();
        void showUploadInformation(final long set);
        void onHeaderRootViewClicked(@NonNull final UploadSection section);
        void onItemRootViewClicked(@NonNull final UploadSection section, final int itemAdapterPosition);
    }
}
