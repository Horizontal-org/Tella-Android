package rs.readahead.washington.mobile.views.adapters;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.ReSubmitFormInstanceEvent;
import rs.readahead.washington.mobile.databinding.SubmittedCollectFormRowBinding;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.util.ViewUtil;
import rs.readahead.washington.mobile.views.fragment.SubmittedFormListListener;

public class CollectSubmittedFormInstanceRecycleViewAdapter extends RecyclerView.Adapter<CollectSubmittedFormInstanceRecycleViewAdapter.ViewHolder> {
    private List<CollectFormInstance> instances = Collections.emptyList();
    private final SubmittedFormListListener listener;

    public CollectSubmittedFormInstanceRecycleViewAdapter(SubmittedFormListListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SubmittedCollectFormRowBinding binding = SubmittedCollectFormRowBinding
                .inflate(LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final CollectFormInstance instance = instances.get(position);

        CollectFormInstanceStatus status = instance.getStatus();

        if (status == CollectFormInstanceStatus.SUBMITTED) {
            holder.setSubmittedIcon();
        } else if (status == CollectFormInstanceStatus.SUBMISSION_ERROR) {
            holder.setSubmitErrorIcon();
        } else if (status == CollectFormInstanceStatus.FINALIZED || // these should not be possible
                status == CollectFormInstanceStatus.SUBMISSION_PENDING ||
                status == CollectFormInstanceStatus.SUBMISSION_PARTIAL_PARTS) {
            holder.setPendingIcon();
        }

        holder.binding.formName.setText(instance.getFormName());
        holder.binding.serverName.setText(instance.getServerName());
        holder.binding.time.setText(new Date(instance.getUpdated()).toString());
        holder.binding.formRow.setOnClickListener(v -> MyApplication.bus().post(new ReSubmitFormInstanceEvent(instance)));
        holder.binding.menu.setOnClickListener(v -> listener.showOptionsBottomSheet(instance));
    }

    @Override
    public int getItemCount() {
        return instances.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setInstances(List<CollectFormInstance> forms) {
        this.instances = forms;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public SubmittedCollectFormRowBinding binding;

        ViewHolder(SubmittedCollectFormRowBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
        }

        private void setSubmittedIcon() {
            Drawable drawable = ViewUtil.getTintedDrawable(binding.formRow.getContext(), R.drawable.ic_check_circle, R.color.wa_green);
            if (drawable != null) {
                binding.icon.setImageDrawable(drawable);
            }
        }

        private void setSubmitErrorIcon() {
            Drawable drawable = ViewUtil.getTintedDrawable(binding.formRow.getContext(), R.drawable.ic_error, R.color.wa_red);
            if (drawable != null) {
                binding.icon.setImageDrawable(drawable);
            }
        }

        private void setPendingIcon() {
            Drawable drawable = ViewUtil.getTintedDrawable(binding.formRow.getContext(), R.drawable.ic_watch_later_black_24dp, R.color.wa_gray);
            if (drawable != null) {
                binding.icon.setImageDrawable(drawable);
            }
        }
    }
}
