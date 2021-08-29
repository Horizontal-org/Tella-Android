package rs.readahead.washington.mobile.util.helper;

import com.hzontal.tella_vault.VaultFile;

import java.io.File;
import java.util.Comparator;

public enum OrderType {

	NAME,
	/**
	 * Last modified is the first
	 */
	DATE,
	/**
	 * Smaller size will be in the first place
	 */
	SIZE;

	public Comparator<VaultFile> getComparator() {
		switch (ordinal()) {
		case 0: // name
			return (lhs, rhs) -> lhs.name.compareTo(rhs.name);
		case 1: // date
			return (lhs, rhs) -> (int) (rhs.created - lhs.created);
		case 2: // size
			return (lhs, rhs) -> (int) (lhs.size - rhs.size);
		default:
			break;
		}
		return null;
	}
}
