package com.marcnuri.demo.kubernetes.client.junit;

import io.fabric8.junit.jupiter.api.KubernetesTest;
import io.fabric8.junit.jupiter.api.LoadKubernetesManifests;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.PortForward;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@KubernetesTest
@LoadKubernetesManifests("/postgres-pod.yaml")
class RoundTripIT {

  private static KubernetesClient kc;
  private static LocalPortForward portForward;
  private ByteArrayOutputStream out;

  @BeforeAll
  static void beforeAll() {
    kc.pods().withName("postgresql").waitUntilReady(30, TimeUnit.SECONDS);
    portForward = kc.pods().withName("postgresql").portForward(5432);
    assertThat(portForward)
      .hasFieldOrPropertyWithValue("alive", true)
      .extracting(PortForward::errorOccurred).isEqualTo(false);
  }

  @AfterAll
  static void afterAll() throws IOException {
    portForward.close();
  }

  @BeforeEach
  void setUp() {
    out = new ByteArrayOutputStream();
    System.setOut(new PrintStream(out, true));
    System.setErr(new PrintStream(out, true));
  }

  @AfterEach
  void tearDown() throws IOException {
    out.close();
  }

  @Test
  void requiredOptionsMissing() {
    Main.main(status -> {});
    assertThat(out.toString(StandardCharsets.UTF_8)).contains("Missing required options");
  }

  @Test
  void requiredParameterMissing() {
    Main.main(status -> {}, "-u", "postgres", "-p", "postgres", "-U", "jdbc:xxx//");
    assertThat(out.toString(StandardCharsets.UTF_8)).contains("Missing required parameter");
  }

  @Test
  void create() {
    Main.main(status -> {}, "-u", "postgres", "-p", "postgres",
      "-U", String.format("jdbc:postgresql://localhost:%d/postgres", portForward.getLocalPort()), "create");
    assertThat(out.toString(StandardCharsets.UTF_8))
      .containsSequence("Creating sample Kubernetes Client implementations...")
      .matches("[\\s\\S]*Created 'Fabric8 Kubernetes Client' with id '\\d+'[\\s\\S]*");
  }

  @Test
  void list() {
    Main.main(status -> {}, "-u", "postgres", "-p", "postgres",
      "-U", String.format("jdbc:postgresql://localhost:%d/postgres", portForward.getLocalPort()), "create");
    Main.main(status -> {}, "-u", "postgres", "-p", "postgres",
      "-U", String.format("jdbc:postgresql://localhost:%d/postgres", portForward.getLocalPort()), "list");
    assertThat(out.toString(StandardCharsets.UTF_8))
      .containsSubsequence("Listing Kubernetes Client implementations:",
        "Fabric8 Kubernetes Client");
  }

  @Test
  void delete() {
    Main.main(status -> {}, "-u", "postgres", "-p", "postgres",
      "-U", String.format("jdbc:postgresql://localhost:%d/postgres", portForward.getLocalPort()), "create");
    Main.main(status -> {}, "-u", "postgres", "-p", "postgres",
      "-U", String.format("jdbc:postgresql://localhost:%d/postgres", portForward.getLocalPort()), "delete");
    assertThat(out.toString(StandardCharsets.UTF_8))
      .containsSequence("Deleting all Kubernetes Client implementations...")
      .matches("[\\s\\S]*Deleted \\d+ Kubernetes Client implementations[\\s\\S]*");
  }

}
