package org.horizontal.tella.mobile.data.entity;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

import java.util.List;


@Root(strict = false)
@Namespace(reference = "http://openrosa.org/xforms/xformsList")
public class XFormsEntity {
    @ElementList(inline = true, empty = false, type = XFormEntity.class, required = false)
    public List<XFormEntity> xforms;
}
