package com.oop.runtime.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "form_definition", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"form_key", "version"})
})
public class FormDefinition {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "form_key", nullable = false)
  private String formKey;

  @Column(name = "version", nullable = false)
  private Integer version;

  @Column(name = "name")
  private String name;

  @Column(name = "schema_json", nullable = false, columnDefinition = "TEXT")
  private String schemaJson;

  @Column(name = "deployed_at", nullable = false)
  private Instant deployedAt;

  @Column(name = "deployed_by")
  private String deployedBy;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getFormKey() { return formKey; }
  public void setFormKey(String formKey) { this.formKey = formKey; }

  public Integer getVersion() { return version; }
  public void setVersion(Integer version) { this.version = version; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getSchemaJson() { return schemaJson; }
  public void setSchemaJson(String schemaJson) { this.schemaJson = schemaJson; }

  public Instant getDeployedAt() { return deployedAt; }
  public void setDeployedAt(Instant deployedAt) { this.deployedAt = deployedAt; }

  public String getDeployedBy() { return deployedBy; }
  public void setDeployedBy(String deployedBy) { this.deployedBy = deployedBy; }
}
