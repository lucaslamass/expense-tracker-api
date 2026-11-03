package expense_tracker_api;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ExpensetrackerApplicationTests {

	@Autowired
	TestRestTemplate restTemplate;

	@Test
	void shouldReturnAnExpenseWhenDataIsSaved() {
		ResponseEntity<String> response = restTemplate.getForEntity("/expenses/99", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		Number id = documentContext.read("$.id");
		assertThat(id).isEqualTo(99);

		String description = documentContext.read("$.description");
		assertThat(description).isEqualTo("Compra supermercado");
	}

	@Test
	void shouldNotReturnAnExpenseWithAnUnknownId() {
		ResponseEntity<String> response = restTemplate.getForEntity("/expenses/1000", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isBlank();
	}

	@Test
	@DirtiesContext
	void shouldCreateANewExpense() {
		Expense newExpense = new Expense(null, "Billete de bus",
				new BigDecimal("1.50"), LocalDate.of(2026, 11, 4), "Transporte");

		ResponseEntity<Void> createResponse = restTemplate.postForEntity("/expenses", newExpense, Void.class);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		URI locationOfNewExpense = createResponse.getHeaders().getLocation();
		ResponseEntity<String> getResponse = restTemplate.getForEntity(locationOfNewExpense, String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
		Number id = documentContext.read("$.id");
		String description = documentContext.read("$.description");
		Double amount = documentContext.read("$.amount");

		assertThat(id).isNotNull();
		assertThat(description).isEqualTo("Billete de bus");
		assertThat(amount).isEqualTo(1.50);
	}
}
