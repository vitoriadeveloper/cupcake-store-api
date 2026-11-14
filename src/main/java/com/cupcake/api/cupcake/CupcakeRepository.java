package com.cupcake.api.cupcake;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CupcakeRepository extends JpaRepository<Cupcake, Long> {
    List<Cupcake> findByAtivoTrueOrderByIdAsc();
}
