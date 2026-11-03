package expense_tracker_api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ExpenseJsonTest {

    @Autowired
    private JacksonTester<Expense> json;

    @Test
    void expenseSerializationTest() throws IOException {
        Expense expense = new Expense(99L, "Compra supermercado",
                new BigDecimal("45.30"), LocalDate.of(2026, 11, 3), "Comida", "lucas");

        assertThat(json.write(expense)).isStrictlyEqualToJson("single.json");
        assertThat(json.write(expense)).extractingJsonPathNumberValue("@.id").isEqualTo(99);
        assertThat(json.write(expense)).extractingJsonPathStringValue("@.description")
                .isEqualTo("Compra supermercado");
    }

    @Test
    void expenseDeserializationTest() throws IOException {
        String jsonText = """
                {
                  "id": 99,
                  "description": "Compra supermercado",
                  "amount": 45.30,
                  "date": "2026-11-03",
                  "category": "Comida",
                  "owner": "lucas"
                }
                """;
        Expense expected = new Expense(99L, "Compra supermercado",
                new BigDecimal("45.30"), LocalDate.of(2026, 11, 3), "Comida", "lucas");

        assertThat(json.parse(jsonText)).isEqualTo(expected);
        assertThat(json.parseObject(jsonText).description()).isEqualTo("Compra supermercado");
    }
}