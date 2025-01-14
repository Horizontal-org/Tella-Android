package org.horizontal.tella.mobile.domain.repository;

import androidx.annotation.NonNull;

import com.hzontal.tella_vault.VaultFile;

import java.util.Collection;

import io.reactivex.Completable;


public interface IMediaFileRepository {
    Completable registerFormAttachments(@NonNull Collection<VaultFile> mediaFile);
}
