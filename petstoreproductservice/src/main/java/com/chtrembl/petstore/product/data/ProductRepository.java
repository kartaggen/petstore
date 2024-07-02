package com.chtrembl.petstore.product.data;

import com.chtrembl.petstore.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

}
