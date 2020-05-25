package com.themkers.inventario.service.mapper;


import com.themkers.inventario.domain.*;
import com.themkers.inventario.service.dto.ProductoDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity {@link Producto} and its DTO {@link ProductoDTO}.
 */
@Mapper(componentModel = "spring", uses = {})
public interface ProductoMapper extends EntityMapper<ProductoDTO, Producto> {



    default Producto fromId(Long id) {
        if (id == null) {
            return null;
        }
        Producto producto = new Producto();
        producto.setId(id);
        return producto;
    }
}
