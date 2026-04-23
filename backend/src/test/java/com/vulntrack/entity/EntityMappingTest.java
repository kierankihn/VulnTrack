package com.vulntrack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityMappingTest {

    @Test
    void cveEntityUsesSafePhysicalColumnNames() throws Exception {
        Table table = CveEntry.class.getAnnotation(Table.class);
        List<String> indexedColumns = Arrays.stream(table.indexes())
            .map(Index::columnList)
            .toList();

        assertTrue(indexedColumns.contains("cve_id"));
        assertTrue(indexedColumns.contains("published_date"));

        Field referencesField = CveEntry.class.getDeclaredField("references");
        Column referencesColumn = referencesField.getAnnotation(Column.class);
        assertEquals("reference_urls", referencesColumn.name());
    }

    @Test
    void assetEntityIndexTargetsRealProjectNameColumn() {
        Table table = Asset.class.getAnnotation(Table.class);
        List<String> indexedColumns = Arrays.stream(table.indexes())
            .map(Index::columnList)
            .toList();

        assertTrue(indexedColumns.contains("project_name"));
    }
}
