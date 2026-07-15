package com.revature.application;

import java.util.Objects;

import com.revature.DAOService.LoadResult;
import com.revature.cardealer.DealershipApplicationContext;

public final class LoadStateCommand {
    private final ApplicationPorts.Persistence storage;

    public LoadStateCommand(DealershipApplicationContext context) {
        this(new ContextPersistenceAdapter(context));
    }

    public LoadStateCommand(ApplicationPorts.Persistence storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    public LoadResult execute(String filename) {
        LoadResult result = storage.load(filename);
        if (!result.isSuccess()) return result;
        try {
            storage.activateLoadedSnapshot(result);
            return result;
        } catch (IllegalArgumentException exception) {
            return LoadResult.failure(LoadResult.Status.INVALID_CONTENT, exception.getMessage());
        }
    }
}