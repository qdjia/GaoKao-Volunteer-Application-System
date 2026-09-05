package com.gaokao.excel;

public record ExcelIssue(String sheet, int row, String field, String message) {}
