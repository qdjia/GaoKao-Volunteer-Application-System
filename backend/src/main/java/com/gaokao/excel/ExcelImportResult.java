package com.gaokao.excel;

import java.util.List;
import java.util.UUID;

public record ExcelImportResult(UUID id, boolean success, int rowCount, int createdCount,
                                int updatedCount, List<ExcelIssue> errors) {}
