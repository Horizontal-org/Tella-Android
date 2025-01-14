package org.horizontal.tella.mobile.data.entity;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

import java.util.List;


@Root(name = "OpenRosaResponse", strict = false)
@Namespace(reference = "http://openrosa.org/http/response")
public class OpenRosaResponseEntity {
    public int statusCode;

    @ElementList(inline = true, empty = false, type = MessageEntity.class)
    public List<MessageEntity> messages;

    @Element(required = false) // todo: deal with this, and check if it is ElementList?
    public SubmissionMetadata submissionMetadata;


    @Root(name = "message", strict = false)
    public static class MessageEntity {
        @Attribute(name = "nature", required = false)
        public String nature;

        @Text(required = false)
        public String text;
    }

    @Root(name = "submissionMetadata", strict = false)
    @Namespace(reference = "http://openrosa.org/http/response")
    public static class SubmissionMetadata {
        @Attribute(name = "id", required = false)
        public String id;

        @Attribute(name = "instanceID", required = false)
        public String instanceID;

        @Attribute(name = "submissionDate", required = false)
        public String submissionDate;

        @Attribute(name = "isComplete", required = false)
        public String isComplete;

        @Attribute(name = "markedAsCompleteDate", required = false)
        public String markedAsCompleteDate;
    }
}
