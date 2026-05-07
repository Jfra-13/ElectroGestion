package com.jfra_13.grupos_electrogenos.model.dto;

import java.util.List;

/**
 * Contrato estándar para respuestas paginadas usado por controllers.
 * Contiene content y metadatos de paginación.
 */
public class PaginatedResponseDTO<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public PaginatedResponseDTO() {}

    public PaginatedResponseDTO(List<T> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}

