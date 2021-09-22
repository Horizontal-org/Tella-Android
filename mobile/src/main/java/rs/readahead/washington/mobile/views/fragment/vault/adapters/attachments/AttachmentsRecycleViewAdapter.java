package rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hzontal.tella_vault.VaultFile;
import com.hzontal.utils.MediaFile;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.VaultFileUrlLoader;
import rs.readahead.washington.mobile.presentation.entity.VaultFileLoaderModel;
import rs.readahead.washington.mobile.util.DateUtil;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.util.helper.OrderType;

enum ViewType {
    SMALL,
    DETAILED
}

public class AttachmentsRecycleViewAdapter extends RecyclerView.Adapter<AttachmentsRecycleViewAdapter.ViewHolder> {
    private List<VaultFile> files = new ArrayList<>();
    private VaultFileUrlLoader glideLoader;
    private IGalleryVaultHandler galleryMediaHandler;
    private Set<VaultFile> selected;
    private boolean selectable;
    private boolean singleSelection;
    private GridLayoutManager layoutManager;


    public AttachmentsRecycleViewAdapter(Context context, IGalleryVaultHandler galleryMediaHandler,
                                         MediaFileHandler mediaFileHandler, GridLayoutManager layoutManager) {
        this(context, galleryMediaHandler, mediaFileHandler, layoutManager, false, false);
    }

    public AttachmentsRecycleViewAdapter(Context context, IGalleryVaultHandler galleryMediaHandler,
                                         MediaFileHandler mediaFileHandler,  GridLayoutManager layoutManager,
                                         boolean selectable,
                                         boolean singleSelection) {
        this.glideLoader = new VaultFileUrlLoader(context.getApplicationContext(), mediaFileHandler);
        this.galleryMediaHandler = galleryMediaHandler;
        this.selected = new LinkedHashSet<>();
        this.selectable = selectable;
        this.singleSelection = singleSelection;
        this.layoutManager = layoutManager;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ViewType.SMALL.ordinal()){
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vault_attachment_grid, parent,false);
            return new GridViewHolder(v, this.selectable);
        }else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vault_attachment_hor, parent,false);
            return new ListViewHolder(v, this.selectable);
        }

    }

    @Override
    public int getItemViewType(int position) {
         return layoutManager.getSpanCount() == 1 ? ViewType.DETAILED.ordinal() : ViewType.SMALL.ordinal();
    }

    public void setLayoutManager(GridLayoutManager layoutManager) {
        this.layoutManager = layoutManager;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final VaultFile vaultFile = files.get(position);

        checkItemState(holder, vaultFile);
        onMoreSelected(holder,vaultFile);
        holder.maybeShowMetadataIcon(vaultFile);
        holder.showFileInfo(vaultFile);
        holder.maybeEnableCheckBox(selectable);

        if (vaultFile.mimeType != null){
            if (MediaFile.INSTANCE.isImageFileType(vaultFile.mimeType)) {
                holder.showImageInfo();
                Glide.with(holder.mediaView.getContext())
                        .using(glideLoader)
                        .load(new VaultFileLoaderModel(vaultFile, VaultFileLoaderModel.LoadType.THUMBNAIL))
                        .signature(messageDigest -> { })
                        .into(holder.mediaView);
            } else if (MediaFile.INSTANCE.isAudioFileType(vaultFile.mimeType)) {
                holder.showAudioInfo(vaultFile);
            } else if (MediaFile.INSTANCE.isVideoFileType(vaultFile.mimeType)) {
                holder.showVideoInfo(vaultFile);
                Glide.with(holder.mediaView.getContext())
                        .using(glideLoader)
                        .load(new VaultFileLoaderModel(vaultFile, VaultFileLoaderModel.LoadType.THUMBNAIL))
                        .signature(messageDigest -> { })
                        .into(holder.mediaView);
            }else if (MediaFile.INSTANCE.isTextFileType(vaultFile.mimeType)){
                 holder.showDocumentInfo(vaultFile);
            }
        }else {
            if (VaultFile.Type.fromValue(vaultFile.type.getValue()) == VaultFile.Type.DIRECTORY){
                holder.showFolderInfo();
            }
        }

        if (selectable){
            holder.itemView.setOnClickListener(v -> checkboxClickHandler(holder, vaultFile));
        }else {
            holder.itemView.setOnClickListener(v -> galleryMediaHandler.playMedia(vaultFile));
        }

        if (holder instanceof ListViewHolder) {
            ( (ListViewHolder) holder).setFileDateTextView(vaultFile);
        }
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

    private void onMoreSelected(ViewHolder holder, VaultFile vaultFile){
        holder.more.setOnClickListener(v -> { galleryMediaHandler.onMoreClicked(vaultFile);});
    }

    private void checkItemState(ViewHolder holder, VaultFile vaultFile) {
        boolean checked = selected.contains(vaultFile);
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
        @BindView(R.id.more)
        ImageView more;
        @BindView(R.id.icAttachmentImg)
        ImageView icAttachmentImg;

        public ViewHolder(View itemView, boolean selectable) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void showVideoInfo(VaultFile vaultFile) {
            icAttachmentImg.setBackgroundResource(R.drawable.ic_play);
        }

        void showAudioInfo(VaultFile vaultFile) {
            icAttachmentImg.setBackgroundResource(R.drawable.ic_audio_w_small);
        }

        void showDocumentInfo(VaultFile vaultFile) {
            icAttachmentImg.setBackgroundResource(R.drawable.ic_document_24px_filled);
        }

        void showImageInfo() {

        }

        void maybeShowMetadataIcon(VaultFile vaultFile) {

        }
        void showFolderInfo(){
            icAttachmentImg.setBackgroundResource(R.drawable.ic_folder_24px);
        }

        void showFileInfo(VaultFile vaultFile){
            fileName.setText(vaultFile.name);
        }

        void maybeEnableCheckBox(boolean selectable) {
            checkBox.setVisibility(selectable ? View.VISIBLE : View.GONE);
            checkBox.setEnabled(selectable);
        }
    }

    static class GridViewHolder extends ViewHolder{

        public GridViewHolder(View itemView, boolean selectable) {
            super(itemView, selectable);
        }
    }

    static class ListViewHolder extends ViewHolder{
        @BindView(R.id.fileDateTextView)
        TextView fileDateTextView;

        public ListViewHolder(View itemView, boolean selectable) {
            super(itemView, selectable);
        }

        public void setFileDateTextView(VaultFile vaultFile){
            fileDateTextView.setText(DateUtil.getDate(vaultFile.created));
        }
    }

}
