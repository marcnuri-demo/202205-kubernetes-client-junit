package com.marcnuri.demo.kubernetes.client.junit;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.schema.Action;
import picocli.CommandLine;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.stream.Stream;

@SuppressWarnings({"java:S115", "java:S106", "unused"})
@CommandLine.Command(
  name = "pgtest",
  description = "PostgreSQL database test",
  mixinStandardHelpOptions = true
)
public class Main implements Callable<Integer> {

  enum Command {create, list, delete}
  @CommandLine.Option(names = {"-U", "--url"}, description = "The database JDBC connection URL", required = true)
  private String jdbcUrl;

  @CommandLine.Option(names = {"-u", "--user"}, description = "The database user name", required = true)
  private String user;

  @CommandLine.Option(names = {"-p", "--password"}, description = "The database user password", required = true)
  private String password;

  @CommandLine.Parameters(index = "0", description = "The command to execute, one of: create, list, delete")
  private Command command;

  @Override
  public Integer call() {
    java.util.logging.Logger.getLogger("org.hibernate").setLevel(Level.SEVERE);
    try (var sessionFactory = initConfiguration().buildSessionFactory();
         var session = sessionFactory.openSession()) {
      session.beginTransaction();
      switch (command) {
        case create -> {
          System.out.println("Creating sample Kubernetes Client implementations...");
          Stream.of("Fabric8 Kubernetes Client", "YAKC", "Kubernetes Client Java")
            .forEach(name -> {
              final var kc = new KubernetesClientImplementation(name);
              session.persist(kc);
              System.out.printf("Created '%s' with id '%s'%n", kc.name, kc.id);
            });
        }
        case list -> {
          System.out.println("Listing Kubernetes Client implementations:");
          session.createQuery("SELECT kc FROM KubernetesClientImplementation kc",
            KubernetesClientImplementation.class).list()
            .forEach(kcs -> System.out.printf(" - %s\t%s%n", kcs.id, kcs.name));
        }
        case delete -> {
          System.out.println("Deleting all Kubernetes Client implementations...");
          int count = session.createQuery("DELETE FROM KubernetesClientImplementation", null)
            .executeUpdate();
          System.out.printf("Deleted %s Kubernetes Client implementations%n", count);
        }
      }
      session.getTransaction().commit();
    }
    return null;
  }

  private Configuration initConfiguration() {
    final var ret = new Configuration();
    ret.setProperty(AvailableSettings.URL, jdbcUrl + "?sslmode=disable&ssl=false");
    ret.setProperty(AvailableSettings.USER, user);
    ret.setProperty(AvailableSettings.PASS, password);
    ret.setProperty(AvailableSettings.HBM2DDL_AUTO, Action.UPDATE.getExternalHbm2ddlName());
    ret.setProperty(AvailableSettings.C3P0_MIN_SIZE, "1");
    ret.setProperty(AvailableSettings.C3P0_MAX_SIZE, "2");
    ret.setProperty(AvailableSettings.C3P0_TIMEOUT, "60");

    Stream.of(KubernetesClientImplementation.class).forEach(ret::addAnnotatedClass);
    return ret;
  }

  public static void main(String... args) {
    main(System::exit, args);
  }

  public static void main(Exitable exitable, String... args) {
    exitable.exit(new CommandLine(new Main()).execute(args));
  }

  @FunctionalInterface
  interface Exitable {
    void exit(int status);
  }

}
