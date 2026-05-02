package com.example.schedulemanager.dto;

import java.util.ArrayList;
import java.util.List;

public class ScheduleCsvImportResult {
    private int totalRows;
    private int validRows;
    private int insertedRows;
    private List<ScheduleCsvImportError> errors = new ArrayList<>();

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getValidRows() {
        return validRows;
    }

    public void setValidRows(int validRows) {
        this.validRows = validRows;
    }

    public int getInsertedRows() {
        return insertedRows;
    }

    public void setInsertedRows(int insertedRows) {
        this.insertedRows = insertedRows;
    }

    public List<ScheduleCsvImportError> getErrors() {
        return errors;
    }

    public void setErrors(List<ScheduleCsvImportError> errors) {
        this.errors = errors;
    }
}
