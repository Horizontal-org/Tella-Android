package org.hzontal.shared_ui.dropdownlist;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.proxym.shared.widget.dropdown_list.CustomDropdownItemClickListener;

import org.hzontal.shared_ui.R;
import org.hzontal.shared_ui.buttons.PanelToggleButton;

import java.util.List;
import java.util.Objects;


public class CustomDropdownList extends FrameLayout {

    float scale = getResources().getDisplayMetrics().density;

    private int defaultName = -1;
    private RecyclerView dropDownRV;
    private PanelToggleButton toggleButton;

    public CustomDropdownList(Context context) {
        super(context);
    }

    public CustomDropdownList(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomDropdownList(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);
        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInflater.inflate(R.layout.custom_dropdown_list, this, true);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomDropdownList, defStyleAttr, R.style.BreadcrumbsView);

            defaultName = a.getResourceId(R.styleable.CustomDropdownList_name, -1);

        }
        initViews(context);
    }

    private void initViews(Context context) {
        toggleButton = findViewById(R.id.dropdown_panel);
        dropDownRV = findViewById(R.id.dropdown_recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        onDropDownImageClicked();
        onDropDownTopBottomImageClicked(linearLayoutManager);

        if (defaultName != -1) {
            toggleButton.setText(context.getString(defaultName));
        }
    }

    public void onDropDownImageClicked() {
        toggleButton.setOnStateChangedListener(isOpen -> dropDownRV.setVisibility(isOpen ? VISIBLE : GONE));
    }

    public void onDropDownTopBottomImageClicked(LinearLayoutManager linearLayoutManager) {
        dropDownRV.setLayoutManager(linearLayoutManager);
        toggleButton.setOnStateChangedListener(isOpen -> {

            dropDownRV.setVisibility(isOpen ? VISIBLE : GONE);
            if (dropDownRV.getAdapter() == null) return;
            int totalItemCount = Objects.requireNonNull(dropDownRV.getAdapter()).getItemCount();
            if (totalItemCount <= 0) {
                return;
            }
            int lastVisibleItemIndex = linearLayoutManager.findLastVisibleItemPosition();

            if (lastVisibleItemIndex >= totalItemCount) {
                return;
            }
            linearLayoutManager.smoothScrollToPosition(dropDownRV, null, lastVisibleItemIndex + 1);
        });
    }


    public void setListAdapter(List<DropDownItem> data, CustomDropdownItemClickListener itemClickListener, Context context) {
        DropdownListAdapter dropdownListAdapter = new DropdownListAdapter(data, itemClickListener, context);
        dropDownRV.setAdapter(dropdownListAdapter);
    }

    public void setDefaultName(String name) {
        if (name != null) {
            toggleButton.setText(name);
            toggleButton.setClose();
        }
    }
}