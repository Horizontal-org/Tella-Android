package rs.readahead.washington.mobile.views.adapters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.DeleteFormInstanceEvent;
import rs.readahead.washington.mobile.bus.event.ShowFormInstanceEntryEvent;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.views.activity.MainActivity;


public class CollectDraftFormInstanceRecycleViewAdapter extends RecyclerView.Adapter<CollectDraftFormInstanceRecycleViewAdapter.ViewHolder> {
    private List<CollectFormInstance> instances = Collections.emptyList();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.draft_collect_form_instance_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final CollectFormInstance instance = instances.get(position);

        final Context context = holder.name.getContext();
        FragmentManager fragmentManager = ((MainActivity) context).getSupportFragmentManager();

        holder.name.setText(instance.getInstanceName());
        holder.organization.setText(instance.getServerName());
        holder.updated.setText(String.format(context.getString(R.string.collect_draft_meta_date_updated),
                Util.getDateTimeString(instance.getUpdated())));
        holder.popupMenu.setOnClickListener(v -> BottomSheetUtils.showEditDeleteMenuSheet(
                fragmentManager,
                instance.getInstanceName(),
                context.getString(R.string.Collect_Action_FillForm),
                context.getString(R.string.action_delete),
                action -> {
                    if (action == BottomSheetUtils.Action.EDIT) {
                        MyApplication.bus().post(new ShowFormInstanceEntryEvent(instance.getId()));
                    }
                    if (action == BottomSheetUtils.Action.DELETE) {
                        deleteForm(instance);
                    }
                },
                context.getString(R.string.Collect_RemoveForm_SheetTitle),
                String.format(context.getResources().getString(R.string.Collect_Subtitle_RemoveForm), instance.getInstanceName()),
                context.getString(R.string.action_remove),
                context.getString(R.string.action_cancel)
        ));
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
        @BindView(R.id.instanceRow)
        ViewGroup instanceRow;
        @BindView(R.id.name)
        TextView name;
        @BindView(R.id.organization)
        TextView organization;
        @BindView(R.id.updated)
        TextView updated;
        @BindView(R.id.popupMenu)
        ImageButton popupMenu;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    private void deleteForm(CollectFormInstance instance){
        MyApplication.bus().post(new DeleteFormInstanceEvent(instance.getId(), instance.getStatus()));
    }
}
