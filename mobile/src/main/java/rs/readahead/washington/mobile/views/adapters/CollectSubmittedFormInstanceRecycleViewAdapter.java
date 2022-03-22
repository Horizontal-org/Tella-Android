package rs.readahead.washington.mobile.views.adapters;

import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.hzontal.shared_ui.submission.SubmittedItem;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.util.ViewUtil;
import rs.readahead.washington.mobile.views.interfaces.ISavedFormsInterface;


public class CollectSubmittedFormInstanceRecycleViewAdapter extends RecyclerView.Adapter<CollectSubmittedFormInstanceRecycleViewAdapter.ViewHolder> {
    private List<CollectFormInstance> instances = Collections.emptyList();
    private final ISavedFormsInterface savedFormsInterface;

    public CollectSubmittedFormInstanceRecycleViewAdapter(ISavedFormsInterface savedFormsInterface) {
        this.savedFormsInterface = savedFormsInterface;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.submitted_collect_form_instance_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final CollectFormInstance instance = instances.get(position);

        holder.setName(instance.getFormName());
        holder.setOrganization(instance.getServerName());
        holder.setDates(instance.getUpdated());

        CollectFormInstanceStatus status = instance.getStatus();

        if (status == CollectFormInstanceStatus.SUBMITTED) {
            holder.setSubmittedIcon();
        } else if (status == CollectFormInstanceStatus.SUBMISSION_ERROR) {
            holder.setSubmitErrorIcon();
        } else if (status == CollectFormInstanceStatus.FINALIZED ||
                status == CollectFormInstanceStatus.SUBMISSION_PENDING ||
                status == CollectFormInstanceStatus.SUBMISSION_PARTIAL_PARTS) {
            holder.setPendingIcon();
        }

        holder.item.popupMenu.setOnClickListener(v -> savedFormsInterface.showFormsMenu(instance));
    }

    @Override
    public int getItemCount() {
        return instances.size();
    }

    public void setInstances(List<CollectFormInstance> forms) {
        this.instances = forms;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.submittedItem)
        SubmittedItem item;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void setDates(long timestamp) {
            item.setUpdated(Util.getElapsedTimeFromTimestamp(timestamp,item.getContext()));
        }

        private void setSubmittedIcon() {
            Drawable drawable = ViewUtil.getTintedDrawable(item.getContext(), R.drawable.ic_check_circle, R.color.wa_green);
            if (drawable != null) {
                item.setIconDrawable(drawable);
            }
        }

        private void setSubmitErrorIcon() {
            Drawable drawable = ViewUtil.getTintedDrawable(item.getContext(), R.drawable.ic_error, R.color.wa_red);
            if (drawable != null) {
                item.setIconDrawable(drawable);
            }
        }

        private void setPendingIcon() {
            Drawable drawable = ViewUtil.getTintedDrawable(item.getContext(), R.drawable.ic_watch_later_black_24dp, R.color.tigers_eye);
            if (drawable != null) {
                item.setIconDrawable(drawable);
            }
        }

        private void setOrganization(String organization) {
            item.setOrganization(organization);
        }

        private void setName(String name) {
            item.setName(name);
        }
    }
}
