package com.chtrembl.petstore.pet.data;

import com.chtrembl.petstore.pet.model.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet, Long> {

}
