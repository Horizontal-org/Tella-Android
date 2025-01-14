package org.horizontal.tella.mobile.util;

public class IOCipherMountHelper {

    /*private static String TAG = "IOCipherMountHelper";
    private CacheWordHandler mHandler;
    private static VirtualFileSystem mVFS;

    public IOCipherMountHelper(CacheWordHandler cacheWord, VirtualFileSystem vfs) {
        if (cacheWord == null)
            throw new IllegalArgumentException("CacheWordHandler is null");
        mHandler = cacheWord;
        mVFS = vfs;
    }

    public VirtualFileSystem mount(String containerPath) throws IOException {
        if (mVFS.isMounted() && containerPath.equals(mVFS.getContainerPath()))
            return mVFS;
        if (mHandler.isLocked())
            throw new IOException("Database locked. Decryption key unavailable.");

        try {
            mVFS = VirtualFileSystem.get();
            mVFS.mount(containerPath, mHandler.getEncryptionKey());
        } catch (Exception e) {
            Log.e(TAG, "mounting IOCipher failed at " + containerPath);
            throw new IOException(e.getMessage());
        }
        return mVFS;
    }

    public boolean isMounted(){
        return mVFS.isMounted();
    }*/
}
