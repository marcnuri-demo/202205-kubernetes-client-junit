package com.marcnuri.demo.kubernetes.client.junit;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class KubernetesClientImplementation {
  @Id
  @GeneratedValue
  public Integer id;

  public String name;

  public KubernetesClientImplementation() {
  }

  public KubernetesClientImplementation(String name) {
    this.name = name;
  }
}
