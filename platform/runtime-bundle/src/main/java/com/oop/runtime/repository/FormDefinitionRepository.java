package com.oop.runtime.repository;

import com.oop.runtime.model.FormDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FormDefinitionRepository extends JpaRepository<FormDefinition, Long> {

  Optional<FormDefinition> findTopByFormKeyOrderByVersionDesc(String formKey);

  List<FormDefinition> findByFormKeyOrderByVersionDesc(String formKey);

  Optional<FormDefinition> findByFormKeyAndVersion(String formKey, Integer version);
}
