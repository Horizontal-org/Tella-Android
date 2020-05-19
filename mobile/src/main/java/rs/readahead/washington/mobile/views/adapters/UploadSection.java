package rs.readahead.washington.mobile.views.adapters;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import timber.log.Timber;

public class UploadSection extends Section {
    private List<MediaFile> files = new ArrayList<>();

    /**
     * Create a Section object based on {@link SectionParameters}.
     *
     * @param sectionParameters section parameters
     */
    public UploadSection(@NonNull SectionParameters sectionParameters) {
        super(sectionParameters);
    }

    public UploadSection(@NonNull final List<MediaFile> files) {
        super(SectionParameters.builder()
                .itemResourceId(R.layout.upload_section_item)
                .headerResourceId(R.layout.upload_section_header)
                .footerResourceId(R.layout.upload_section_footer)
                .emptyResourceId(R.layout.upload_empty_layout)
                .failedResourceId(R.layout.upload_empty_layout)
                .build());
        this.files = files;
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
    public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {
        Timber.d("++++ onBindItemViewHolder position %d", position);
        UploadViewHolder itemHolder = (UploadViewHolder) holder;

        // bind your view here
        itemHolder.mediaView.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.ic_menu_gallery));
    }

    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(final View view) {
        return new HeaderViewHolder(view);
    }

    @Override
    public void onBindHeaderViewHolder(final RecyclerView.ViewHolder holder) {
        final HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
    }

    @Override
    public RecyclerView.ViewHolder getFooterViewHolder(final View view) {
        return new FooterViewHolder(view);
    }

    @Override
    public void onBindFooterViewHolder(final RecyclerView.ViewHolder holder) {
        final FooterViewHolder footerHolder = (FooterViewHolder) holder;
    }

    static class UploadViewHolder extends RecyclerView.ViewHolder {
        ImageView mediaView;

        UploadViewHolder(View itemView) {
            super(itemView);
            mediaView = itemView.findViewById(R.id.mediaView);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        HeaderViewHolder(View itemView) {
            super(itemView);
        }
    }

    static class FooterViewHolder extends RecyclerView.ViewHolder {
        FooterViewHolder(View itemView) {
            super(itemView);
        }
    }
}
