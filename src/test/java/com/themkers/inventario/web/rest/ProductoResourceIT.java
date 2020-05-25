package com.themkers.inventario.web.rest;

import com.themkers.inventario.InventariomicroservicioApp;
import com.themkers.inventario.domain.Producto;
import com.themkers.inventario.repository.ProductoRepository;
import com.themkers.inventario.service.ProductoService;
import com.themkers.inventario.service.dto.ProductoDTO;
import com.themkers.inventario.service.mapper.ProductoMapper;
import com.themkers.inventario.service.dto.ProductoCriteria;
import com.themkers.inventario.service.ProductoQueryService;

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
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@link ProductoResource} REST controller.
 */
@SpringBootTest(classes = InventariomicroservicioApp.class)

@AutoConfigureMockMvc
@WithMockUser
public class ProductoResourceIT {

    private static final String DEFAULT_NOMBRE = "AAAAAAAAAA";
    private static final String UPDATED_NOMBRE = "BBBBBBBBBB";

    private static final BigDecimal DEFAULT_PRECIO = new BigDecimal(1);
    private static final BigDecimal UPDATED_PRECIO = new BigDecimal(2);
    private static final BigDecimal SMALLER_PRECIO = new BigDecimal(1 - 1);

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProductoMapper productoMapper;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ProductoQueryService productoQueryService;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restProductoMockMvc;

    private Producto producto;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Producto createEntity(EntityManager em) {
        Producto producto = new Producto()
            .nombre(DEFAULT_NOMBRE)
            .precio(DEFAULT_PRECIO);
        return producto;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Producto createUpdatedEntity(EntityManager em) {
        Producto producto = new Producto()
            .nombre(UPDATED_NOMBRE)
            .precio(UPDATED_PRECIO);
        return producto;
    }

    @BeforeEach
    public void initTest() {
        producto = createEntity(em);
    }

    @Test
    @Transactional
    public void createProducto() throws Exception {
        int databaseSizeBeforeCreate = productoRepository.findAll().size();

        // Create the Producto
        ProductoDTO productoDTO = productoMapper.toDto(producto);
        restProductoMockMvc.perform(post("/api/productos")
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(productoDTO)))
            .andExpect(status().isCreated());

        // Validate the Producto in the database
        List<Producto> productoList = productoRepository.findAll();
        assertThat(productoList).hasSize(databaseSizeBeforeCreate + 1);
        Producto testProducto = productoList.get(productoList.size() - 1);
        assertThat(testProducto.getNombre()).isEqualTo(DEFAULT_NOMBRE);
        assertThat(testProducto.getPrecio()).isEqualTo(DEFAULT_PRECIO);
    }

    @Test
    @Transactional
    public void createProductoWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = productoRepository.findAll().size();

        // Create the Producto with an existing ID
        producto.setId(1L);
        ProductoDTO productoDTO = productoMapper.toDto(producto);

        // An entity with an existing ID cannot be created, so this API call must fail
        restProductoMockMvc.perform(post("/api/productos")
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(productoDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Producto in the database
        List<Producto> productoList = productoRepository.findAll();
        assertThat(productoList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void getAllProductos() throws Exception {
        // Initialize the database
        productoRepository.saveAndFlush(producto);

        // Get all the productoList
        restProductoMockMvc.perform(get("/api/productos?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(producto.getId().intValue())))
            .andExpect(jsonPath("$.[*].nombre").value(hasItem(DEFAULT_NOMBRE)))
            .andExpect(jsonPath("$.[*].precio").value(hasItem(DEFAULT_PRECIO.intValue())));
    }
    
    @Test
    @Transactional
    public void getProducto() throws Exception {
        // Initialize the database
        productoRepository.saveAndFlush(producto);

        // Get the producto
        restProductoMockMvc.perform(get("/api/productos/{id}", producto.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(producto.getId().intValue()))
            .andExpect(jsonPath("$.nombre").value(DEFAULT_NOMBRE))
            .andExpect(jsonPath("$.precio").value(DEFAULT_PRECIO.intValue()));
    }


    @Test
    @Transactional
    public void getProductosByIdFiltering() throws Exception {
        // Initialize the database
        productoRepository.saveAndFlush(producto);

        Long id = producto.getId();

        defaultProductoShouldBeFound("id.equals=" + id);
        defaultProductoShouldNotBeFound("id.notEquals=" + id);

        defaultProductoShouldBeFound("id.greaterThanOrEqual=" + id);
        defaultProductoShouldNotBeFound("id.greaterThan=" + id);

        defaultProductoShouldBeFound("id.lessThanOrEqual=" + id);
        defaultProductoShouldNotBeFound("id.lessThan=" + id);
    }


    @Test
    @Transactional
    public void getAllProductosByNombreIsEqualToSomething() throws Exception {
        // Initialize the database
        productoRepository.saveAndFlush(producto);

        // Get all the productoList where nombre equals to DEFAULT_NOMBRE
        defaultProductoShouldBeFound("nombre.equals=" + DEFAULT_NOMBRE);

        // Get all the productoList where nombre equals to UPDATED_NOMBRE
        defaultProductoShouldNotBeFound("nombre.equals=" + UPDATED_NOMBRE);
    }

    @Test
    @Transactional
    public void getAllProductosByNombreIsNotEqualToSomething() throws Exception {
        // Initialize the database
        productoRepository.saveAndFlush(producto);

        // Get all the productoList where nombre not equals to DEFAULT_NOMBRE
        defaultProductoShouldNotBeFound("nombre.notEquals=" + DEFAULT_NOMBRE);

        // Get all the productoList where nombre not equals to UPDATED_NOMBRE
        defaultProductoShouldBeFound("nombre.notEquals=" + UPDATED_NOMBRE);
    }

    @Test
    @Transactional
    public void getAllProductosByNombreIsInShouldWork() throws Exception {
        // Initialize the database
        productoRepository.saveAndFlush(producto);

        // Get all the productoList where nombre in DEFAULT_NOMBRE or UPDATED_NOMBRE
        defaultProductoShouldBeFound("nombre.in=" + DEFAULT_NOMBRE + "," + UPDATED_NOMBRE);

        // Get all the productoList where nombre equals to UPDATED_NOMBRE
        defaultProductoShouldNotBeFound("nombre.in=" + UPDATED_NOMBRE);
    }

    @Test
    @Transactional
    public void getAllProductosByNombreIsNullOrNotNull() throws Exception {
        // Initialize the database
        productoRepository.saveAndFlush(producto);

        // Get all the productoList where nombre is not null
        defaultProductoShouldBeFound("nombre.specified=true");

        // Get all the productoList where nombre is null
        defaultProductoShouldNotBeFound("nombre.specified=false");
    }
                @Test
    @Transactional
    public void getAllProductosByNombreContainsSomething() throws Exception {
        // Initialize the database
        productoRepository.saveAndFlush(producto);

        // Get all the productoList where nombre contains DEFAULT_NOMBRE
        defaultProductoShouldBeFound("nombre.contains=" + DEFAULT_NOMBRE);

        // Get all the productoList where nombre contains UPDATED_NOMBRE
        defaultProductoShouldNotBeFound("nombre.contains=" + UPDATED_NOMBRE);
    }

    @Test
    @Transactional
    public void getAllProductosByNombreNotContainsSomething() throws Exception {
        // Initialize the database
        productoRepository.saveAndFlush(producto);

        // Get all the productoList where nombre does not contain DEFAULT_NOMBRE
        defaultProductoShouldNotBeFound("nombre.doesNotContain=" + DEFAULT_NOMBRE);

        // Get all the productoList where nombre does not contain UPDATED_NOMBRE
        defaultProductoShouldBeFound("nombre.doesNotContain=" + UPDATED_NOMBRE);
    }


    @Test
    @Transactional
    public void getAllProductosByPrecioIsEqualToSomething() throws Exception {
        // Initialize the database
        productoRepository.saveAndFlush(producto);

        // Get all the productoList where precio equals to DEFAULT_PRECIO
        defaultProductoShouldBeFound("precio.equals=" + DEFAULT_PRECIO);

        // Get all the productoList where precio equals to UPDATED_PRECIO
        defaultProductoShouldNotBeFound("precio.equals=" + UPDATED_PRECIO);
    }

    @Test
    @Transactional
    public void getAllProductosByPrecioIsNotEqualToSomething() throws Exception {
        // Initialize the database
        productoRepository.saveAndFlush(producto);

        // Get all the productoList where precio not equals to DEFAULT_PRECIO
        defaultProductoShouldNotBeFound("precio.notEquals=" + DEFAULT_PRECIO);

        // Get all the productoList where precio not equals to UPDATED_PRECIO
        defaultProductoShouldBeFound("precio.notEquals=" + UPDATED_PRECIO);
    }

    @Test
    @Transactional
    public void getAllProductosByPrecioIsInShouldWork() throws Exception {
        // Initialize the database
        productoRepository.saveAndFlush(producto);

        // Get all the productoList where precio in DEFAULT_PRECIO or UPDATED_PRECIO
        defaultProductoShouldBeFound("precio.in=" + DEFAULT_PRECIO + "," + UPDATED_PRECIO);

        // Get all the productoList where precio equals to UPDATED_PRECIO
        defaultProductoShouldNotBeFound("precio.in=" + UPDATED_PRECIO);
    }

    @Test
    @Transactional
    public void getAllProductosByPrecioIsNullOrNotNull() throws Exception {
        // Initialize the database
        productoRepository.saveAndFlush(producto);

        // Get all the productoList where precio is not null
        defaultProductoShouldBeFound("precio.specified=true");

        // Get all the productoList where precio is null
        defaultProductoShouldNotBeFound("precio.specified=false");
    }

    @Test
    @Transactional
    public void getAllProductosByPrecioIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        productoRepository.saveAndFlush(producto);

        // Get all the productoList where precio is greater than or equal to DEFAULT_PRECIO
        defaultProductoShouldBeFound("precio.greaterThanOrEqual=" + DEFAULT_PRECIO);

        // Get all the productoList where precio is greater than or equal to UPDATED_PRECIO
        defaultProductoShouldNotBeFound("precio.greaterThanOrEqual=" + UPDATED_PRECIO);
    }

    @Test
    @Transactional
    public void getAllProductosByPrecioIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        productoRepository.saveAndFlush(producto);

        // Get all the productoList where precio is less than or equal to DEFAULT_PRECIO
        defaultProductoShouldBeFound("precio.lessThanOrEqual=" + DEFAULT_PRECIO);

        // Get all the productoList where precio is less than or equal to SMALLER_PRECIO
        defaultProductoShouldNotBeFound("precio.lessThanOrEqual=" + SMALLER_PRECIO);
    }

    @Test
    @Transactional
    public void getAllProductosByPrecioIsLessThanSomething() throws Exception {
        // Initialize the database
        productoRepository.saveAndFlush(producto);

        // Get all the productoList where precio is less than DEFAULT_PRECIO
        defaultProductoShouldNotBeFound("precio.lessThan=" + DEFAULT_PRECIO);

        // Get all the productoList where precio is less than UPDATED_PRECIO
        defaultProductoShouldBeFound("precio.lessThan=" + UPDATED_PRECIO);
    }

    @Test
    @Transactional
    public void getAllProductosByPrecioIsGreaterThanSomething() throws Exception {
        // Initialize the database
        productoRepository.saveAndFlush(producto);

        // Get all the productoList where precio is greater than DEFAULT_PRECIO
        defaultProductoShouldNotBeFound("precio.greaterThan=" + DEFAULT_PRECIO);

        // Get all the productoList where precio is greater than SMALLER_PRECIO
        defaultProductoShouldBeFound("precio.greaterThan=" + SMALLER_PRECIO);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultProductoShouldBeFound(String filter) throws Exception {
        restProductoMockMvc.perform(get("/api/productos?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(producto.getId().intValue())))
            .andExpect(jsonPath("$.[*].nombre").value(hasItem(DEFAULT_NOMBRE)))
            .andExpect(jsonPath("$.[*].precio").value(hasItem(DEFAULT_PRECIO.intValue())));

        // Check, that the count call also returns 1
        restProductoMockMvc.perform(get("/api/productos/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultProductoShouldNotBeFound(String filter) throws Exception {
        restProductoMockMvc.perform(get("/api/productos?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restProductoMockMvc.perform(get("/api/productos/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }


    @Test
    @Transactional
    public void getNonExistingProducto() throws Exception {
        // Get the producto
        restProductoMockMvc.perform(get("/api/productos/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateProducto() throws Exception {
        // Initialize the database
        productoRepository.saveAndFlush(producto);

        int databaseSizeBeforeUpdate = productoRepository.findAll().size();

        // Update the producto
        Producto updatedProducto = productoRepository.findById(producto.getId()).get();
        // Disconnect from session so that the updates on updatedProducto are not directly saved in db
        em.detach(updatedProducto);
        updatedProducto
            .nombre(UPDATED_NOMBRE)
            .precio(UPDATED_PRECIO);
        ProductoDTO productoDTO = productoMapper.toDto(updatedProducto);

        restProductoMockMvc.perform(put("/api/productos")
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(productoDTO)))
            .andExpect(status().isOk());

        // Validate the Producto in the database
        List<Producto> productoList = productoRepository.findAll();
        assertThat(productoList).hasSize(databaseSizeBeforeUpdate);
        Producto testProducto = productoList.get(productoList.size() - 1);
        assertThat(testProducto.getNombre()).isEqualTo(UPDATED_NOMBRE);
        assertThat(testProducto.getPrecio()).isEqualTo(UPDATED_PRECIO);
    }

    @Test
    @Transactional
    public void updateNonExistingProducto() throws Exception {
        int databaseSizeBeforeUpdate = productoRepository.findAll().size();

        // Create the Producto
        ProductoDTO productoDTO = productoMapper.toDto(producto);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restProductoMockMvc.perform(put("/api/productos")
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(productoDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Producto in the database
        List<Producto> productoList = productoRepository.findAll();
        assertThat(productoList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteProducto() throws Exception {
        // Initialize the database
        productoRepository.saveAndFlush(producto);

        int databaseSizeBeforeDelete = productoRepository.findAll().size();

        // Delete the producto
        restProductoMockMvc.perform(delete("/api/productos/{id}", producto.getId())
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Producto> productoList = productoRepository.findAll();
        assertThat(productoList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
