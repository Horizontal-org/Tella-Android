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
import timber.log.Timber;

public class UploadInformationRecycleViewAdapter extends RecyclerView.Adapter<UploadInformationRecycleViewAdapter.ViewHolder> {
    private List<FileUploadInstance> instances = Collections.emptyList();
    private MediaFileUrlLoader glideLoader;
    private UploadInformationInterface uploadInformationInterface;
    private long set;

    public UploadInformationRecycleViewAdapter(Context context, MediaFileHandler mediaFileHandler, @NonNull UploadInformationInterface uploadInformationInterface, long set) {
        this.glideLoader = new MediaFileUrlLoader(context.getApplicationContext(), mediaFileHandler);
        this.uploadInformationInterface = uploadInformationInterface;
        this.set = set;
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

        holder.size.setText(FileUtil.getFileSizeString(instance.getSize()));

        if (instance.getStatus() == ITellaUploadsRepository.UploadStatus.UPLOADED) {
            holder.uploadIndicator.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_check_circle_green));
        } else {
            holder.uploadIndicator.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_stop_circle_outline));
            holder.uploadIndicator.setOnClickListener(v -> uploadInformationInterface.clearUpload(instance.getId()));
        }

        if (instance.getMediaFile() != null) {
            holder.name.setText(instance.getMediaFile().getFileName());
            holder.hash.setText(instance.getMediaFile().getHash());
            holder.type.setText(instance.getMediaFile().getType().name());
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
            holder.mediaView.setAlpha((float) 0.5);
        }
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

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
