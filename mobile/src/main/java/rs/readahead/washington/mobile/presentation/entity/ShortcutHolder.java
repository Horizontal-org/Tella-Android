package rs.readahead.washington.mobile.presentation.entity;

import java.util.Collections;
import java.util.List;


public class ShortcutHolder {
    public Shortcut saved = Shortcut.NONE;
    public List<Shortcut> general = Collections.emptyList();
    public List<Shortcut> forms = Collections.emptyList();
    public List<Shortcut> modules = Collections.emptyList();
}
