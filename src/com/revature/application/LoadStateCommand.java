package com.revature.application;

import java.util.Objects;

import com.revature.DAOService.LoadResult;
import com.revature.cardealer.DealershipApplicationContext;
import com.revature.cardealer.RehydratedApplicationState;

/** Application use case for loading, validating, and activating persisted state. */
public final class LoadStateCommand {

    private final DealershipApplicationContext context;

    public LoadStateCommand(DealershipApplicationContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public LoadResult execute(String filename) {
        LoadResult result = context.getDataStore().loadData(filename);
        if (!result.isSuccess()) {
            return result;
        }
        try {
            RehydratedApplicationState replacement = context.getSnapshotRehydrator()
                    .rehydrate(result.getSnapshot().get());
            context.replaceRuntime(replacement);
            return result;
        } catch (IllegalArgumentException exception) {
            return LoadResult.failure(LoadResult.Status.INVALID_CONTENT, exception.getMessage());
        }
    }
}
