package rs.readahead.washington.mobile.domain.entity.collect;


public class CollectInstanceVaultFile {
    private long id;
    private long instanceId;
    private String vaultFileId;
    private int status;

    public CollectInstanceVaultFile(long id, long instanceId, String vaultFileId, int status) {
        this.id = id;
        this.instanceId = instanceId;
        this.vaultFileId = vaultFileId;
        this.status = status;
    }

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }

    public long getInstanceId() {
        return instanceId;
    }
    public void setInstanceId(long instanceId) {
        this.instanceId = instanceId;
    }

    public String getVaultFileId() {
        return vaultFileId;
    }
    public void setVaultFileId(String id) {
        this.vaultFileId = id;
    }

    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }
}
