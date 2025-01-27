package org.horizontal.tella.mobile.domain.entity.collect;


public class OpenRosaPartResponse extends OpenRosaResponse {
    private String partName;


    @SuppressWarnings("unused")
    private OpenRosaPartResponse() {}

    public OpenRosaPartResponse(String partName) {
        this.partName = partName;
    }

    public String getPartName() {
        return partName;
    }
}
