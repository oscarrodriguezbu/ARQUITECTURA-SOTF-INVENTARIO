package com.themkers.inventario.web.rest;

import com.themkers.inventario.InventariomicroservicioApp;
import com.themkers.inventario.domain.Stock;
import com.themkers.inventario.domain.Producto;
import com.themkers.inventario.repository.StockRepository;
import com.themkers.inventario.service.StockService;
import com.themkers.inventario.service.dto.StockDTO;
import com.themkers.inventario.service.mapper.StockMapper;
import com.themkers.inventario.service.dto.StockCriteria;
import com.themkers.inventario.service.StockQueryService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@link StockResource} REST controller.
 */
@SpringBootTest(classes = InventariomicroservicioApp.class)

@AutoConfigureMockMvc
@WithMockUser
public class StockResourceIT {

    private static final Long DEFAULT_CANTIDAD = 0L;
    private static final Long UPDATED_CANTIDAD = 1L;
    private static final Long SMALLER_CANTIDAD = 0L - 1L;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private StockService stockService;

    @Autowired
    private StockQueryService stockQueryService;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restStockMockMvc;

    private Stock stock;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Stock createEntity(EntityManager em) {
        Stock stock = new Stock()
            .cantidad(DEFAULT_CANTIDAD);
        return stock;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Stock createUpdatedEntity(EntityManager em) {
        Stock stock = new Stock()
            .cantidad(UPDATED_CANTIDAD);
        return stock;
    }

    @BeforeEach
    public void initTest() {
        stock = createEntity(em);
    }

    @Test
    @Transactional
    public void createStock() throws Exception {
        int databaseSizeBeforeCreate = stockRepository.findAll().size();

        // Create the Stock
        StockDTO stockDTO = stockMapper.toDto(stock);
        restStockMockMvc.perform(post("/api/stocks")
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(stockDTO)))
            .andExpect(status().isCreated());

        // Validate the Stock in the database
        List<Stock> stockList = stockRepository.findAll();
        assertThat(stockList).hasSize(databaseSizeBeforeCreate + 1);
        Stock testStock = stockList.get(stockList.size() - 1);
        assertThat(testStock.getCantidad()).isEqualTo(DEFAULT_CANTIDAD);
    }

    @Test
    @Transactional
    public void createStockWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = stockRepository.findAll().size();

        // Create the Stock with an existing ID
        stock.setId(1L);
        StockDTO stockDTO = stockMapper.toDto(stock);

        // An entity with an existing ID cannot be created, so this API call must fail
        restStockMockMvc.perform(post("/api/stocks")
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(stockDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Stock in the database
        List<Stock> stockList = stockRepository.findAll();
        assertThat(stockList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void checkCantidadIsRequired() throws Exception {
        int databaseSizeBeforeTest = stockRepository.findAll().size();
        // set the field null
        stock.setCantidad(null);

        // Create the Stock, which fails.
        StockDTO stockDTO = stockMapper.toDto(stock);

        restStockMockMvc.perform(post("/api/stocks")
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(stockDTO)))
            .andExpect(status().isBadRequest());

        List<Stock> stockList = stockRepository.findAll();
        assertThat(stockList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllStocks() throws Exception {
        // Initialize the database
        stockRepository.saveAndFlush(stock);

        // Get all the stockList
        restStockMockMvc.perform(get("/api/stocks?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(stock.getId().intValue())))
            .andExpect(jsonPath("$.[*].cantidad").value(hasItem(DEFAULT_CANTIDAD.intValue())));
    }
    
    @Test
    @Transactional
    public void getStock() throws Exception {
        // Initialize the database
        stockRepository.saveAndFlush(stock);

        // Get the stock
        restStockMockMvc.perform(get("/api/stocks/{id}", stock.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(stock.getId().intValue()))
            .andExpect(jsonPath("$.cantidad").value(DEFAULT_CANTIDAD.intValue()));
    }


    @Test
    @Transactional
    public void getStocksByIdFiltering() throws Exception {
        // Initialize the database
        stockRepository.saveAndFlush(stock);

        Long id = stock.getId();

        defaultStockShouldBeFound("id.equals=" + id);
        defaultStockShouldNotBeFound("id.notEquals=" + id);

        defaultStockShouldBeFound("id.greaterThanOrEqual=" + id);
        defaultStockShouldNotBeFound("id.greaterThan=" + id);

        defaultStockShouldBeFound("id.lessThanOrEqual=" + id);
        defaultStockShouldNotBeFound("id.lessThan=" + id);
    }


    @Test
    @Transactional
    public void getAllStocksByCantidadIsEqualToSomething() throws Exception {
        // Initialize the database
        stockRepository.saveAndFlush(stock);

        // Get all the stockList where cantidad equals to DEFAULT_CANTIDAD
        defaultStockShouldBeFound("cantidad.equals=" + DEFAULT_CANTIDAD);

        // Get all the stockList where cantidad equals to UPDATED_CANTIDAD
        defaultStockShouldNotBeFound("cantidad.equals=" + UPDATED_CANTIDAD);
    }

    @Test
    @Transactional
    public void getAllStocksByCantidadIsNotEqualToSomething() throws Exception {
        // Initialize the database
        stockRepository.saveAndFlush(stock);

        // Get all the stockList where cantidad not equals to DEFAULT_CANTIDAD
        defaultStockShouldNotBeFound("cantidad.notEquals=" + DEFAULT_CANTIDAD);

        // Get all the stockList where cantidad not equals to UPDATED_CANTIDAD
        defaultStockShouldBeFound("cantidad.notEquals=" + UPDATED_CANTIDAD);
    }

    @Test
    @Transactional
    public void getAllStocksByCantidadIsInShouldWork() throws Exception {
        // Initialize the database
        stockRepository.saveAndFlush(stock);

        // Get all the stockList where cantidad in DEFAULT_CANTIDAD or UPDATED_CANTIDAD
        defaultStockShouldBeFound("cantidad.in=" + DEFAULT_CANTIDAD + "," + UPDATED_CANTIDAD);

        // Get all the stockList where cantidad equals to UPDATED_CANTIDAD
        defaultStockShouldNotBeFound("cantidad.in=" + UPDATED_CANTIDAD);
    }

    @Test
    @Transactional
    public void getAllStocksByCantidadIsNullOrNotNull() throws Exception {
        // Initialize the database
        stockRepository.saveAndFlush(stock);

        // Get all the stockList where cantidad is not null
        defaultStockShouldBeFound("cantidad.specified=true");

        // Get all the stockList where cantidad is null
        defaultStockShouldNotBeFound("cantidad.specified=false");
    }

    @Test
    @Transactional
    public void getAllStocksByCantidadIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        stockRepository.saveAndFlush(stock);

        // Get all the stockList where cantidad is greater than or equal to DEFAULT_CANTIDAD
        defaultStockShouldBeFound("cantidad.greaterThanOrEqual=" + DEFAULT_CANTIDAD);

        // Get all the stockList where cantidad is greater than or equal to UPDATED_CANTIDAD
        defaultStockShouldNotBeFound("cantidad.greaterThanOrEqual=" + UPDATED_CANTIDAD);
    }

    @Test
    @Transactional
    public void getAllStocksByCantidadIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        stockRepository.saveAndFlush(stock);

        // Get all the stockList where cantidad is less than or equal to DEFAULT_CANTIDAD
        defaultStockShouldBeFound("cantidad.lessThanOrEqual=" + DEFAULT_CANTIDAD);

        // Get all the stockList where cantidad is less than or equal to SMALLER_CANTIDAD
        defaultStockShouldNotBeFound("cantidad.lessThanOrEqual=" + SMALLER_CANTIDAD);
    }

    @Test
    @Transactional
    public void getAllStocksByCantidadIsLessThanSomething() throws Exception {
        // Initialize the database
        stockRepository.saveAndFlush(stock);

        // Get all the stockList where cantidad is less than DEFAULT_CANTIDAD
        defaultStockShouldNotBeFound("cantidad.lessThan=" + DEFAULT_CANTIDAD);

        // Get all the stockList where cantidad is less than UPDATED_CANTIDAD
        defaultStockShouldBeFound("cantidad.lessThan=" + UPDATED_CANTIDAD);
    }

    @Test
    @Transactional
    public void getAllStocksByCantidadIsGreaterThanSomething() throws Exception {
        // Initialize the database
        stockRepository.saveAndFlush(stock);

        // Get all the stockList where cantidad is greater than DEFAULT_CANTIDAD
        defaultStockShouldNotBeFound("cantidad.greaterThan=" + DEFAULT_CANTIDAD);

        // Get all the stockList where cantidad is greater than SMALLER_CANTIDAD
        defaultStockShouldBeFound("cantidad.greaterThan=" + SMALLER_CANTIDAD);
    }


    @Test
    @Transactional
    public void getAllStocksByProductoIsEqualToSomething() throws Exception {
        // Initialize the database
        stockRepository.saveAndFlush(stock);
        Producto producto = ProductoResourceIT.createEntity(em);
        em.persist(producto);
        em.flush();
        stock.setProducto(producto);
        stockRepository.saveAndFlush(stock);
        Long productoId = producto.getId();

        // Get all the stockList where producto equals to productoId
        defaultStockShouldBeFound("productoId.equals=" + productoId);

        // Get all the stockList where producto equals to productoId + 1
        defaultStockShouldNotBeFound("productoId.equals=" + (productoId + 1));
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultStockShouldBeFound(String filter) throws Exception {
        restStockMockMvc.perform(get("/api/stocks?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(stock.getId().intValue())))
            .andExpect(jsonPath("$.[*].cantidad").value(hasItem(DEFAULT_CANTIDAD.intValue())));

        // Check, that the count call also returns 1
        restStockMockMvc.perform(get("/api/stocks/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultStockShouldNotBeFound(String filter) throws Exception {
        restStockMockMvc.perform(get("/api/stocks?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restStockMockMvc.perform(get("/api/stocks/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }


    @Test
    @Transactional
    public void getNonExistingStock() throws Exception {
        // Get the stock
        restStockMockMvc.perform(get("/api/stocks/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateStock() throws Exception {
        // Initialize the database
        stockRepository.saveAndFlush(stock);

        int databaseSizeBeforeUpdate = stockRepository.findAll().size();

        // Update the stock
        Stock updatedStock = stockRepository.findById(stock.getId()).get();
        // Disconnect from session so that the updates on updatedStock are not directly saved in db
        em.detach(updatedStock);
        updatedStock
            .cantidad(UPDATED_CANTIDAD);
        StockDTO stockDTO = stockMapper.toDto(updatedStock);

        restStockMockMvc.perform(put("/api/stocks")
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(stockDTO)))
            .andExpect(status().isOk());

        // Validate the Stock in the database
        List<Stock> stockList = stockRepository.findAll();
        assertThat(stockList).hasSize(databaseSizeBeforeUpdate);
        Stock testStock = stockList.get(stockList.size() - 1);
        assertThat(testStock.getCantidad()).isEqualTo(UPDATED_CANTIDAD);
    }

    @Test
    @Transactional
    public void updateNonExistingStock() throws Exception {
        int databaseSizeBeforeUpdate = stockRepository.findAll().size();

        // Create the Stock
        StockDTO stockDTO = stockMapper.toDto(stock);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restStockMockMvc.perform(put("/api/stocks")
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(stockDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Stock in the database
        List<Stock> stockList = stockRepository.findAll();
        assertThat(stockList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteStock() throws Exception {
        // Initialize the database
        stockRepository.saveAndFlush(stock);

        int databaseSizeBeforeDelete = stockRepository.findAll().size();

        // Delete the stock
        restStockMockMvc.perform(delete("/api/stocks/{id}", stock.getId())
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Stock> stockList = stockRepository.findAll();
        assertThat(stockList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
