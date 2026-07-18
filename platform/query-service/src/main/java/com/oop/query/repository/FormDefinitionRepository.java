package com.oop.query.repository;

import com.oop.query.model.FormDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FormDefinitionRepository extends JpaRepository<FormDefinition, Long> {

  Optional<FormDefinition> findTopByFormKeyOrderByVersionDesc(String formKey);
}
