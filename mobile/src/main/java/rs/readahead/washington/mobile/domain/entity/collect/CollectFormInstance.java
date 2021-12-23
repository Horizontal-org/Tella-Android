package rs.readahead.washington.mobile.domain.entity.collect;

import com.hzontal.tella_vault.VaultFile;
import com.hzontal.tella_vault.rx.RxVault;

import org.javarosa.core.model.FormDef;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;

public class CollectFormInstance implements Serializable {
    public static final CollectFormInstance NONE = new CollectFormInstance();

    private long id;
    private long serverId;
    private String serverName;
    private String username;
    private CollectFormInstanceStatus status = CollectFormInstanceStatus.UNKNOWN;
    private long updated;
    private String formID;
    private String version;
    private String formName;
    private String instanceName;
    private FormDef formDef;
    private final Map<String, FormMediaFile> widgetMediaFiles = new HashMap<>();
    private long clonedId; // id of submitted instance we are clone of
    private FormMediaFileStatus formPartStatus = FormMediaFileStatus.UNKNOWN;
    private final Set<String> widgetVaultFileIds = new HashSet<>();

    /**
     * Load this instance widget FormMediaFiles from Vault, if they are
     * found there.
     * @param vault Vault to use for loading.
     */
    public Observable<CollectFormInstance> loadWidgetMediaFiles(RxVault vault) {
        return vault.get(widgetVaultFileIds)
                .flatMapObservable((Function<Set<VaultFile>, ObservableSource<CollectFormInstance>>) vaultFiles -> {
                    widgetMediaFiles.clear();

                    for (VaultFile vaultFile : vaultFiles) {
                        widgetMediaFiles.put(vaultFile.id, FormMediaFile.fromMediaFile(vaultFile));
                    }

                    return Observable.just(CollectFormInstance.this);
                });
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public CollectFormInstanceStatus getStatus() {
        return status;
    }

    public void setStatus(CollectFormInstanceStatus status) {
        this.status = status;
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
        this.updated = updated;
    }

    public String getFormID() {
        return formID;
    }

    public void setFormID(String formID) {
        this.formID = formID;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public FormDef getFormDef() {
        return formDef;
    }

    public void setFormDef(FormDef formDef) {
        this.formDef = formDef;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getFormName() {
        return formName;
    }

    public void setFormName(String formName) {
        this.formName = formName;
    }

    public long getClonedId() {
        return clonedId;
    }

    public void setClonedId(long clonedId) {
        this.clonedId = clonedId;
    }

    public List<FormMediaFile> getWidgetMediaFiles() {
        return new ArrayList<>(widgetMediaFiles.values());
    }

    public void setWidgetMediaFile(String name, FormMediaFile mediaFile) {
        widgetMediaFiles.put(name, mediaFile);
    }

    public FormMediaFile getWidgetMediaFile(String name) {
        return widgetMediaFiles.get(name);
    }

    public void removeWidgetMediaFile(String name) {
        widgetMediaFiles.remove(name);
    }

    public FormMediaFileStatus getFormPartStatus() {
        return formPartStatus;
    }

    public void setFormPartStatus(FormMediaFileStatus formPartStatus) {
        this.formPartStatus = formPartStatus;
    }

    public void addWidgetVaultFileId(String id) {
        widgetVaultFileIds.add(id);
    }

    public Set<String> getWidgetVaultFileIds() {
        return widgetVaultFileIds;
    }
}
