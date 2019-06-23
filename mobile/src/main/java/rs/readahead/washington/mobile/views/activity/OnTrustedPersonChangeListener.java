package rs.readahead.washington.mobile.views.activity;


import rs.readahead.washington.mobile.domain.entity.TrustedPerson;

public interface OnTrustedPersonChangeListener {

    void onContactEdited(TrustedPerson trustedPerson);

    void onContactDeleted(int id);
}
