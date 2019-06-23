package rs.readahead.washington.mobile.data.entity;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;


@Root(name = "xforms-group", strict = false)
public class XFormsGroupEntity {
    @Element(name = "groupID")
    public String groupID;

    @Element(name = "name")
    public String name;

    @Element(name = "listUrl")
    public String listUrl;

    @Element(name = "descriptionText", required = false)
    public String descriptionText;

    @Element(name = "descriptionUrl", required = false)
    public String descriptionUrl;
}
