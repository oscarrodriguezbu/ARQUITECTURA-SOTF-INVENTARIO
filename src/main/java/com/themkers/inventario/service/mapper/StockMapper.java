package com.themkers.inventario.service.mapper;


import com.themkers.inventario.domain.*;
import com.themkers.inventario.service.dto.StockDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity {@link Stock} and its DTO {@link StockDTO}.
 */
@Mapper(componentModel = "spring", uses = {ProductoMapper.class})
public interface StockMapper extends EntityMapper<StockDTO, Stock> {

    @Mapping(source = "producto.id", target = "productoId")
    @Mapping(source = "producto.nombre", target = "productoNombre")
    StockDTO toDto(Stock stock);

    @Mapping(source = "productoId", target = "producto")
    Stock toEntity(StockDTO stockDTO);

    default Stock fromId(Long id) {
        if (id == null) {
            return null;
        }
        Stock stock = new Stock();
        stock.setId(id);
        return stock;
    }
}
