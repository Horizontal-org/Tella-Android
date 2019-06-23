package rs.readahead.washington.mobile.data.entity;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;


@Root(name = "form", strict = false)
public class FormEntity {
    @Text
    public String name;

    @Attribute(name = "url")
    public String url;
}
