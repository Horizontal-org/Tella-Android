package rs.readahead.washington.mobile.media;

import com.hzontal.tella_vault.VaultFile;

import java.util.ArrayList;
import java.util.List;

import rs.readahead.washington.mobile.MyApplication;

public class FileWalker {
    private final List<VaultFile> filesResult;

    public FileWalker() {
        filesResult = new ArrayList<>();
    }

    public List<VaultFile> walk(VaultFile root) {

        List<VaultFile> files = MyApplication.rxVault.list(root).blockingGet();

        for (VaultFile f : files) {
            if (f.type == VaultFile.Type.DIRECTORY) {
                walk(f);
            } else {
                if (f.type == VaultFile.Type.FILE) {
                    filesResult.add(f);
                }

            }
        }
        return filesResult;
    }

    public List<VaultFile> walkWithDirectories(VaultFile root) {

        List<VaultFile> files = MyApplication.rxVault.list(root).blockingGet();

        for (VaultFile f : files) {
            if (f.type == VaultFile.Type.DIRECTORY) {
                walk(f);
                filesResult.add(f);
            } else {
                if (f.type == VaultFile.Type.FILE) {
                    filesResult.add(f);
                }

            }
        }
        return filesResult;
    }

}