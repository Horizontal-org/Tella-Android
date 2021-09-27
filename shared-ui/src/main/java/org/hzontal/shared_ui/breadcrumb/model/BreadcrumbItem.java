package org.hzontal.shared_ui.breadcrumb.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class BreadcrumbItem implements IBreadcrumbItem<Item> {

	private int mSelectedIndex = -1;
	private List<Item> mItems;

	public BreadcrumbItem(@NonNull List<Item> items) {
		this(items, 0);
	}

	public BreadcrumbItem(@NonNull List<Item> items, int selectedIndex) {
		if (!items.isEmpty()) {
			this.mItems = items;
			this.mSelectedIndex = selectedIndex;
		} else {
			throw new IllegalArgumentException("Items shouldn't be null empty.");
		}
	}

    private BreadcrumbItem(Parcel in) {
        mSelectedIndex = in.readInt();
        mItems = in.createTypedArrayList(Item.CREATOR);
    }

    public static final Parcelable.Creator<BreadcrumbItem> CREATOR = new Creator<BreadcrumbItem>() {
        @Override
        public BreadcrumbItem createFromParcel(Parcel in) {
            return new BreadcrumbItem(in);
        }

        @Override
        public BreadcrumbItem[] newArray(int size) {
            return new BreadcrumbItem[size];
        }
    };

	@Override
	public void setSelectedItem(@NonNull Item selectedItem) {
		this.mSelectedIndex = mItems.indexOf(selectedItem);
		if (mSelectedIndex == -1) {
			throw new IllegalArgumentException("This item does not exist in items.");
		}
	}

    @Override
	public void setSelectedIndex(int selectedIndex) {
		this.mSelectedIndex = selectedIndex;
	}

    @Override
	public int getSelectedIndex() {
		return this.mSelectedIndex;
	}

    @Override
	public @NonNull Item getSelectedItem() {
		return this.mItems.get(getSelectedIndex());
	}

    @Override
	public boolean hasMoreSelect() {
		return this.mItems.size() > 1;
	}

    @Override
	public void setItems(@NonNull List<Item> items) {
		this.setItems(items, 0);
	}

    @Override
	public void setItems(@NonNull List<Item> items, int selectedIndex) {
		if (!items.isEmpty()) {
			this.mItems = items;
			this.mSelectedIndex = selectedIndex;
		} else {
			throw new IllegalArgumentException("Items shouldn\'t be null empty.");
		}
	}

    @Override
	public @NonNull List<Item> getItems() {
		return mItems;
	}

	/**
	 * Create a simple BreadcrumbItem with single item
	 *
	 * @param item Item
	 * @return Simple BreadcrumbItem
	 * @see BreadcrumbItem
	 */
	public static BreadcrumbItem createSimpleItem(@NonNull Item item) {
		return new BreadcrumbItem(Collections.singletonList(item));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BreadcrumbItem other = (BreadcrumbItem ) obj;
		if (mSelectedIndex != other.mSelectedIndex)
			return false;
		if (mItems.size() != other.mItems.size())
			return false;
		for (int i = 0; i < mItems.size(); i++) {
			if (!mItems.get(i).equals(other.mItems.get(i))) {
				return false;
			}
		}
		return true;
	}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mSelectedIndex);
        dest.writeArray(mItems.toArray());
    }

    @NonNull
    @Override
    public Iterator iterator() {
        return mItems.iterator();
    }
}
