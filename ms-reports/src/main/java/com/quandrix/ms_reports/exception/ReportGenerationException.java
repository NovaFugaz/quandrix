package com.quandrix.ms_reports.exception;

public class ReportGenerationException extends RuntimeException {
    public ReportGenerationException(String message) {
        super("Error al generar reporte: " + message);
    }
}