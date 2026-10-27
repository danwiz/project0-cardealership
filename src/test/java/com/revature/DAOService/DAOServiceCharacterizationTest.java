package com.revature.DAOService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.revature.cardealer.Data;

class DAOServiceCharacterizationTest {

    private File fileToDelete;

    @AfterEach
    void cleanUpCreatedFile() {
        if (fileToDelete != null && fileToDelete.exists()) {
            fileToDelete.delete();
        }
    }

    @Test
    @Tag("LEGACY-BEHAVIOR")
    void createDataUsesDefaultFilenameWhenCaptionIsNull() {
        DAOService service = new DAOService();
        Data data = new Data();
        reserveNewTestFile("CarDealer.dat");

        service.createData(data);

        assertTrue(fileToDelete.isFile());
    }

    @Test
    @Tag("LEGACY-BEHAVIOR")
    void createAndReadDataRoundTripUsesCaptionPlusDatExtension() {
        DAOService service = new DAOService();
        String caption = "characterization-" + UUID.randomUUID();
        Data data = new Data();
        data.setCaption(caption);
        reserveNewTestFile(caption + ".dat");

        service.createData(data);
        Data restored = service.readData(caption);

        assertNotNull(restored);
        assertEquals(caption, restored.getCaption());
    }

    @Test
    @Tag("KNOWN-DEFECT")
    void captionThatAlreadyContainsDatProducesDoubleExtension() {
        DAOService service = new DAOService();
        String caption = "characterization-" + UUID.randomUUID() + ".dat";
        Data data = new Data();
        data.setCaption(caption);
        reserveNewTestFile(caption + ".dat");

        service.createData(data);

        assertTrue(fileToDelete.isFile(),
                "DAOService appends .dat even when the caption already contains the extension");
    }

    @Test
    @Tag("KNOWN-DEFECT")
    void readingMissingDataReturnsNullInsteadOfAResultOrDomainError() {
        DAOService service = new DAOService();

        Data result = service.readData("missing-" + UUID.randomUUID());

        assertNull(result);
    }

    private void reserveNewTestFile(String filename) {
        File candidate = new File(filename);
        // Legacy DAOService writes relative to the current directory. Never overwrite or
        // delete a real application's pre-existing saved data while characterizing it.
        Assumptions.assumeFalse(candidate.exists(),
                "Preserving pre-existing data file: " + candidate.getAbsolutePath());
        fileToDelete = candidate;
    }
}
