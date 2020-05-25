package com.themkers.inventario.service.dto;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link com.themkers.inventario.domain.Stock} entity.
 */
public class StockDTO implements Serializable {
    
    private Long id;

    @NotNull
    @Min(value = 0L)
    private Long cantidad;


    private Long productoId;

    private String productoNombre;
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCantidad() {
        return cantidad;
    }

    public void setCantidad(Long cantidad) {
        this.cantidad = cantidad;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public String getProductoNombre() {
        return productoNombre;
    }

    public void setProductoNombre(String productoNombre) {
        this.productoNombre = productoNombre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StockDTO stockDTO = (StockDTO) o;
        if (stockDTO.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), stockDTO.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "StockDTO{" +
            "id=" + getId() +
            ", cantidad=" + getCantidad() +
            ", productoId=" + getProductoId() +
            ", productoNombre='" + getProductoNombre() + "'" +
            "}";
    }
}
