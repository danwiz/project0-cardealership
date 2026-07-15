package com.revature.DAOService;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.revature.cardealer.ApplicationStateSnapshot;
import com.revature.cardealer.Data;

public class DAOService implements DataDAO {

    @Override
    public SaveResult saveData(Data data, String filename) {
        if (data == null || isBlank(filename)) {
            return SaveResult.failure(SaveResult.Status.INVALID_INPUT, filename,
                    "data and filename are required");
        }

        try (FileOutputStream output = new FileOutputStream(filename);
                ObjectOutputStream objectOutput = new ObjectOutputStream(output)) {
            objectOutput.writeObject(data.getSnapshot());
            return SaveResult.success(filename);
        } catch (IOException | RuntimeException exception) {
            return SaveResult.failure(SaveResult.Status.IO_ERROR, filename,
                    exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
        }
    }

    @Override
    public LoadResult loadData(String filename) {
        if (isBlank(filename)) {
            return LoadResult.failure(LoadResult.Status.IO_ERROR, "filename is required");
        }

        try (FileInputStream input = new FileInputStream(filename);
                ObjectInputStream objectInput = new ObjectInputStream(input)) {
            Object value = objectInput.readObject();
            if (!(value instanceof ApplicationStateSnapshot)) {
                return LoadResult.failure(LoadResult.Status.INVALID_CONTENT,
                        "file does not contain an application-state snapshot");
            }

            ApplicationStateSnapshot snapshot = (ApplicationStateSnapshot) value;
            if (snapshot.getVersion() != ApplicationStateSnapshot.CURRENT_VERSION) {
                return LoadResult.failure(LoadResult.Status.UNSUPPORTED_VERSION,
                        "unsupported snapshot version: " + snapshot.getVersion());
            }
            return LoadResult.success(snapshot);
        } catch (FileNotFoundException exception) {
            return LoadResult.failure(LoadResult.Status.NOT_FOUND, "file not found: " + filename);
        } catch (ClassNotFoundException exception) {
            return LoadResult.failure(LoadResult.Status.CLASS_NOT_FOUND, exception.getMessage());
        } catch (IOException exception) {
            return LoadResult.failure(LoadResult.Status.IO_ERROR,
                    exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
