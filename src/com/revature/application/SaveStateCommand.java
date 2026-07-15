package com.revature.application;

import java.util.Objects;

import com.revature.DAOService.SaveResult;
import com.revature.cardealer.DealershipApplicationContext;

public final class SaveStateCommand {
    private final ApplicationPorts.Persistence storage;

    public SaveStateCommand(DealershipApplicationContext context) {
        this(new ContextPersistenceAdapter(context));
    }

    public SaveStateCommand(ApplicationPorts.Persistence storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    public SaveResult execute(String filename) {
        return storage.save(storage.snapshotSource(), filename);
    }
}