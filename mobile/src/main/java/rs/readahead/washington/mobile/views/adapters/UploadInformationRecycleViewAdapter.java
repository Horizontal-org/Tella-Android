package rs.readahead.washington.mobile.views.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.lzyzsd.circleprogress.DonutProgress;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.FileUploadInstance;
import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.repository.ITellaUploadsRepository;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.MediaFileUrlLoader;
import rs.readahead.washington.mobile.presentation.entity.MediaFileLoaderModel;
import rs.readahead.washington.mobile.util.FileUtil;
import rs.readahead.washington.mobile.util.ViewUtil;

public class UploadInformationRecycleViewAdapter extends RecyclerView.Adapter<UploadInformationRecycleViewAdapter.ViewHolder> {
    private List<FileUploadInstance> instances = Collections.emptyList();
    private MediaFileUrlLoader glideLoader;
    private UploadInformationInterface uploadInformationInterface;

    public UploadInformationRecycleViewAdapter(Context context, MediaFileHandler mediaFileHandler, @NonNull UploadInformationInterface uploadInformationInterface) {
        this.glideLoader = new MediaFileUrlLoader(context.getApplicationContext(), mediaFileHandler);
        this.uploadInformationInterface = uploadInformationInterface;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.upload_information_instance_row, parent, false);
        return new UploadInformationRecycleViewAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final FileUploadInstance instance = instances.get(position);

        final Context context = holder.name.getContext();

        holder.size.setText(String.format(context.getString(R.string.file_size), FileUtil.getFileSizeString(instance.getSize())));

        if (instance.getMediaFile() != null) {
            holder.name.setText(instance.getMediaFile().getFileName());
            holder.hash.setText(String.format("%s: %s", context.getString(R.string.filehash), instance.getMediaFile().getHash()));
            holder.type.setText(String.format("%s: %s", context.getString(R.string.file_type), instance.getMediaFile().getType().name()));
            if (instance.getMediaFile().getType() == MediaFile.Type.IMAGE || instance.getMediaFile().getType() == MediaFile.Type.VIDEO) {
                Glide.with(holder.mediaView.getContext())
                        .using(glideLoader)
                        .load(new MediaFileLoaderModel(instance.getMediaFile(), MediaFileLoaderModel.LoadType.THUMBNAIL))
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(holder.mediaView);
            } else if (instance.getMediaFile().getType() == MediaFile.Type.AUDIO) {
                Drawable drawable = VectorDrawableCompat.create(context.getResources(),
                        R.drawable.ic_mic_gray, null);
                holder.mediaView.setImageDrawable(drawable);
            }
        } else {
            holder.mediaView.setImageDrawable(context.getResources().getDrawable(R.drawable.uploaded_empty_file));
            holder.mediaView.setAlpha((float) 0.4);
        }

        ViewUtil.setGrayScale(holder.mediaView);

        if (instance.getStatus() == ITellaUploadsRepository.UploadStatus.UPLOADED) {
            holder.uploadIndicator.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_check_circle_green));
            holder.uploadIndicator.setPadding(0, 0, 0, 0);
            holder.donutProgress.setVisibility(View.GONE);
            ViewUtil.setColored(holder.mediaView);
        } else {
            int progress = (int) (instance.getUploaded() * 100 / instance.getSize());
            holder.donutProgress.setVisibility(View.VISIBLE);
            holder.donutProgress.setProgress(progress);
            holder.uploadIndicator.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_stop_black_24dp));
            holder.uploadIndicator.setPadding(15, 15, 15, 15);
            holder.donutProgress.setOnClickListener(v -> uploadInformationInterface.clearUpload(instance.getId()));
        }

        holder.mediaView.setOnClickListener(v -> uploadInformationInterface.onMediaViewItemClicked(instance.getMediaFile()));
    }

    @Override
    public int getItemCount() {
        return instances.size();
    }

    public void setInstances(List<FileUploadInstance> instances) {
        this.instances = instances;
        notifyDataSetChanged();
    }

    public interface UploadInformationInterface {
        void clearUpload(final long id);
        void onMediaViewItemClicked(MediaFile mediaFile);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.instanceRow)
        ViewGroup instanceRow;
        @BindView(R.id.mediaView)
        ImageView mediaView;
        @BindView(R.id.name)
        TextView name;
        @BindView(R.id.type)
        TextView type;
        @BindView(R.id.size)
        TextView size;
        @BindView(R.id.hash)
        TextView hash;
        @BindView(R.id.uploadIndicator)
        ImageView uploadIndicator;
        @BindView(R.id.donut_progress)
        DonutProgress donutProgress;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
