package org.horizontal.tella.mobile.data.entity;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;


@Root(name = "forms", strict = false)
public class FormsEntity {
    @ElementList(inline = true, empty = false)
    public List<FormEntity> forms;
}
