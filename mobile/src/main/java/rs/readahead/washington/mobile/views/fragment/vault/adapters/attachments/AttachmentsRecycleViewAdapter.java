package rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.bumptech.glide.Glide;
import com.hzontal.tella_vault.VaultFile;
import com.hzontal.utils.MediaFile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.VaultFileUrlLoader;
import rs.readahead.washington.mobile.presentation.entity.VaultFileLoaderModel;
import rs.readahead.washington.mobile.util.DateUtil;
import rs.readahead.washington.mobile.databinding.ItemVaultAttachmentGridBinding;
import rs.readahead.washington.mobile.databinding.ItemVaultAttachmentHorBinding;

public class AttachmentsRecycleViewAdapter extends RecyclerView.Adapter<AttachmentsRecycleViewAdapter.ViewHolder> {
    private List<VaultFile> files = new ArrayList<>();
    //private final VaultFileUrlLoader glideLoader;
    private final IGalleryVaultHandler galleryMediaHandler;
    private final Set<VaultFile> selected;
    private boolean selectable;
    private boolean isMoveMode;
    private final boolean singleSelection;
    private GridLayoutManager layoutManager;
    private ViewBinding binding;


    public AttachmentsRecycleViewAdapter(Context context, IGalleryVaultHandler galleryMediaHandler,
                                         MediaFileHandler mediaFileHandler, GridLayoutManager layoutManager) {
        this(context, galleryMediaHandler, mediaFileHandler, layoutManager, false, false, false);
    }

    public AttachmentsRecycleViewAdapter(Context context, IGalleryVaultHandler galleryMediaHandler,
                                         MediaFileHandler mediaFileHandler, GridLayoutManager layoutManager,
                                         boolean selectable,
                                         boolean singleSelection,
                                         boolean isMoveMode) {
       // this.glideLoader = new VaultFileUrlLoader(context.getApplicationContext(), mediaFileHandler);
        this.galleryMediaHandler = galleryMediaHandler;
        this.selected = new LinkedHashSet<>();
        this.selectable = selectable;
        this.singleSelection = singleSelection;
        this.layoutManager = layoutManager;
        this.isMoveMode = isMoveMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ViewType.SMALL.ordinal()) {
            binding = ItemVaultAttachmentGridBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new GridViewHolder((ItemVaultAttachmentGridBinding)binding, this.selectable);
        } else {
            binding = ItemVaultAttachmentHorBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ListViewHolder((ItemVaultAttachmentHorBinding)binding, this.selectable);
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
        onMoreSelected(holder, vaultFile);
        holder.maybeShowMetadataIcon(vaultFile);
        holder.showFileInfo(vaultFile);
        holder.maybeEnableCheckBox(selectable);

        if (vaultFile.mimeType != null) {
            if (MediaFile.INSTANCE.isImageFileType(vaultFile.mimeType)) {
                holder.mediaView.setVisibility(View.VISIBLE);
             /*   Glide.with(holder.mediaView.getContext())
                        .using(glideLoader)
                        .load(new VaultFileLoaderModel(vaultFile, VaultFileLoaderModel.LoadType.THUMBNAIL))
                        .signature(messageDigest -> { })
                        .into(holder.mediaView);*/
            } else if (MediaFile.INSTANCE.isAudioFileType(vaultFile.mimeType)) {
                holder.showAudioInfo();
            } else if (MediaFile.INSTANCE.isVideoFileType(vaultFile.mimeType)) {
                holder.showVideoInfo();
               /* Glide.with(holder.mediaView.getContext())
                        .using(glideLoader)
                        .load(new VaultFileLoaderModel(vaultFile, VaultFileLoaderModel.LoadType.THUMBNAIL))
                        .signature(messageDigest -> { })
                        .into(holder.mediaView);*/
            } else {
                holder.showDocumentInfo();
            }
        } else {
            if (VaultFile.Type.fromValue(vaultFile.type.getValue()) == VaultFile.Type.DIRECTORY) {
                holder.showFolderInfo();
            }
        }

        if (holder instanceof ListViewHolder) {
            ((ListViewHolder) holder).setFileDateTextView(vaultFile);
        }
        handleClickMode(holder,vaultFile);
        setMoveModeView(holder);
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

    public void setSelectedMediaFiles(@NonNull List<VaultFile> selectedMediaFiles) {
        selected.addAll(selectedMediaFiles);
        notifyDataSetChanged();
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

    public void enableSelectMode(Boolean selectable) {
        this.selectable = selectable;
        notifyDataSetChanged();
    }

    public void enableMoveMode(Boolean isMoveMode){
        this.isMoveMode = isMoveMode;
        notifyDataSetChanged();
    }

    private void setMoveModeView(ViewHolder holder){
        if (isMoveMode){
            holder.more.setVisibility(View.GONE);
            holder.checkBox.setVisibility(View.GONE);
        }
    }

    public void handleClickMode(ViewHolder holder, VaultFile vaultFile){
        if (isMoveMode){
            if (vaultFile.type == VaultFile.Type.DIRECTORY){
                holder.itemView.setOnClickListener(v -> galleryMediaHandler.playMedia(vaultFile));
            }else {
                holder.itemView.setOnClickListener(null);
            }
        }else {
            if (selectable) {
                holder.itemView.setOnClickListener(v -> checkboxClickHandler(holder, vaultFile));
            } else {
                holder.itemView.setOnClickListener(v -> galleryMediaHandler.playMedia(vaultFile));
            }
        }
    }

    private void removeAllSelections() {
        for (VaultFile selection : selected) {
            deselectMediaFile(selection);
            galleryMediaHandler.onMediaDeselected(selection);
        }
    }

    public void selectAll(){
        for (VaultFile selection : files) {
            selectMediaFile(selection);
            galleryMediaHandler.onMediaSelected(selection);
        }
    }

    private void onMoreSelected(ViewHolder holder, VaultFile vaultFile) {
        holder.more.setOnClickListener(v -> {
            galleryMediaHandler.onMoreClicked(vaultFile);
        });
    }

    private void checkItemState(ViewHolder holder, VaultFile vaultFile) {
        boolean checked = selected.contains(vaultFile);
        // holder.selectionDimmer.setVisibility(checked ? View.VISIBLE : View.GONE);
        holder.itemView.setBackgroundColor(checked ? ContextCompat.getColor(holder.itemView.getContext(),R.color.wa_white_16) :
                isMoveMode ? ContextCompat.getColor(holder.itemView.getContext(),R.color.wa_white_12)
                    : ContextCompat.getColor(holder.itemView.getContext(),R.color.space_cadet)
                );
        holder.checkBox.setImageResource(checked ? R.drawable.ic_check_box_on : R.drawable.ic_check_box_off);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mediaView;
        TextView fileName;
        ImageView more;
        ImageView icAttachmentImg;
        ImageView checkBox;

        public ViewHolder(ViewBinding binding, boolean selectable) {
            super(binding.getRoot());
            if (binding instanceof ItemVaultAttachmentGridBinding){
                icAttachmentImg = ((ItemVaultAttachmentGridBinding) binding).icAttachmentImg;
                more = ((ItemVaultAttachmentGridBinding) binding).more;
                fileName =  ((ItemVaultAttachmentGridBinding) binding).fileNameTextView;
                mediaView = ((ItemVaultAttachmentGridBinding) binding).attachmentImg;
                checkBox = ((ItemVaultAttachmentGridBinding) binding).checkboxTypeSingle;
            } else {
                icAttachmentImg = ((ItemVaultAttachmentHorBinding) binding).icAttachmentImg;
                more = ((ItemVaultAttachmentHorBinding) binding).more;
                fileName =  ((ItemVaultAttachmentHorBinding) binding).fileNameTextView;
                mediaView = ((ItemVaultAttachmentHorBinding) binding).attachmentImg;
                checkBox = ((ItemVaultAttachmentHorBinding) binding).checkboxTypeSingle;
            }
        }

        void showVideoInfo() {
            icAttachmentImg.setBackgroundResource(R.drawable.ic_play);
            mediaView.setVisibility(View.VISIBLE);
        }

        void showAudioInfo() {
            icAttachmentImg.setBackgroundResource(R.drawable.ic_audio_w_small);
            mediaView.setVisibility(View.INVISIBLE);
        }

        void showDocumentInfo() {
            icAttachmentImg.setBackgroundResource(R.drawable.ic_document_24px_filled);
            mediaView.setVisibility(View.INVISIBLE);
        }

        void maybeShowMetadataIcon(VaultFile vaultFile) {

        }

        void showFolderInfo() {
            icAttachmentImg.setBackgroundResource(R.drawable.ic_folder_24px);
            mediaView.setVisibility(View.INVISIBLE);
        }

        void showFileInfo(VaultFile vaultFile) {
            fileName.setText(vaultFile.name);
        }

        void maybeEnableCheckBox(boolean selectable) {
            checkBox.setVisibility(selectable ? View.VISIBLE : View.GONE);
            more.setVisibility(selectable ? View.GONE : View.VISIBLE);
            checkBox.setEnabled(selectable);
        }
    }

    static class GridViewHolder extends ViewHolder {
        ItemVaultAttachmentGridBinding binding;

        public GridViewHolder(ItemVaultAttachmentGridBinding gridBinding, boolean selectable) {
            super(gridBinding, selectable);
            this.binding = gridBinding;
        }
    }

    static class ListViewHolder extends ViewHolder {
        ItemVaultAttachmentHorBinding horBinding;

        public ListViewHolder(ItemVaultAttachmentHorBinding horBinding, boolean selectable) {
            super(horBinding, selectable);
            this.horBinding = horBinding;
        }

        public void setFileDateTextView(VaultFile vaultFile) {
            horBinding.fileDateTextView.setText(DateUtil.getDate(vaultFile.created));
        }
    }
}
