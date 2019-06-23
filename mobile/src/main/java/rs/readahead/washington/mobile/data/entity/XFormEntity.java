package rs.readahead.washington.mobile.data.entity;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;


@Root(name = "xform", strict = false)
public class XFormEntity {
    @Element(name = "formID")
    public String formID;

    @Element(name = "name")
    public String name;

    @Element(name = "version", required = false) // Apollo non-compliant
    public String version;

    @Element(name = "hash")
    public String hash;

    @Element(name = "downloadUrl")
    public String downloadUrl;

    @Element(name = "descriptionText", required = false)
    public String descriptionText;

    @Element(name = "descriptionUrl", required = false)
    public String descriptionUrl;

    @Element(name = "manifestUrl", required = false)
    public String manifestUrl;
}
