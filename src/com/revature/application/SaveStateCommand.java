package com.revature.application;

import java.util.Objects;

import com.revature.DAOService.SaveResult;
import com.revature.cardealer.DealershipApplicationContext;

/** Application use case for persisting the current application state. */
public final class SaveStateCommand {

    private final DealershipApplicationContext context;

    public SaveStateCommand(DealershipApplicationContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public SaveResult execute(String filename) {
        return context.getDataStore().saveData(context.snapshotSource(), filename);
    }
}
