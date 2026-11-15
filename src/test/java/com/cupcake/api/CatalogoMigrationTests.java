package com.cupcake.api;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogoMigrationTests {
    @Test
    void migrationsCarregamCatalogoComImagensExistentesSemReaplicar() throws Exception {
        String url = "jdbc:h2:mem:catalogo_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        var flyway = Flyway.configure().dataSource(url, "sa", "")
            .defaultSchema("cupcake").locations("classpath:db/migration").load();
        flyway.migrate();

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var rows = statement.executeQuery(
                 "SELECT imagem_url, preco, ativo, disponivel FROM cupcake.tb_cupcakes")) {
            int count = 0;
            while (rows.next()) {
                String path = rows.getString("imagem_url");
                assertThat(path).startsWith("/images/cupcakes/");
                assertThat(new ClassPathResource("static" + path).exists()).as(path).isTrue();
                assertThat(rows.getBigDecimal("preco")).isPositive();
                assertThat(rows.getBoolean("ativo")).isTrue();
                assertThat(rows.getBoolean("disponivel")).isTrue();
                count++;
            }
            assertThat(count).isEqualTo(8);
        }
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }
}
