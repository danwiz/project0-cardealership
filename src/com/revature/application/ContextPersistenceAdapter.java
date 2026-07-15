package com.revature.application;

import java.util.Objects;

import com.revature.DAOService.LoadResult;
import com.revature.DAOService.SaveResult;
import com.revature.cardealer.Data;
import com.revature.cardealer.DealershipApplicationContext;
import com.revature.cardealer.RehydratedApplicationState;

/** Binds persistence and runtime activation to the current application context. */
public final class ContextPersistenceAdapter implements ApplicationPorts.Persistence {
    private final DealershipApplicationContext context;

    public ContextPersistenceAdapter(DealershipApplicationContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override public SaveResult save(Data source, String filename) {
        return context.getDataStore().saveData(source, filename);
    }

    @Override public LoadResult load(String filename) {
        return context.getDataStore().loadData(filename);
    }

    @Override public Data snapshotSource() { return context.snapshotSource(); }

    @Override
    public void activateLoadedSnapshot(LoadResult result) {
        if (!result.isSuccess() || !result.getSnapshot().isPresent()) {
            throw new IllegalArgumentException("successful load result with snapshot is required");
        }
        RehydratedApplicationState replacement = context.getSnapshotRehydrator()
                .rehydrate(result.getSnapshot().get());
        context.replaceRuntime(replacement);
    }
}