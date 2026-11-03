package expense_tracker_api;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
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
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("lucas", "abc123")
				.getForEntity("/expenses/99", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		Number id = documentContext.read("$.id");
		assertThat(id).isEqualTo(99);

		String description = documentContext.read("$.description");
		assertThat(description).isEqualTo("Compra supermercado");
	}

	@Test
	void shouldNotReturnAnExpenseWithAnUnknownId() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("lucas", "abc123")
				.getForEntity("/expenses/1000", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isBlank();
	}

	@Test
	@DirtiesContext
	void shouldCreateANewExpense() {
		Expense newExpense = new Expense(null, "Billete de bus",
				new BigDecimal("1.50"), LocalDate.of(2026, 11, 4), "Transporte", null);

		ResponseEntity<Void> createResponse = restTemplate
				.withBasicAuth("lucas", "abc123")
				.postForEntity("/expenses", newExpense, Void.class);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		URI locationOfNewExpense = createResponse.getHeaders().getLocation();
		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("lucas", "abc123")
				.getForEntity(locationOfNewExpense, String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
		Number id = documentContext.read("$.id");
		String description = documentContext.read("$.description");
		Double amount = documentContext.read("$.amount");

		assertThat(id).isNotNull();
		assertThat(description).isEqualTo("Billete de bus");
		assertThat(amount).isEqualTo(1.50);

		String owner = documentContext.read("$.owner");
		assertThat(owner).isEqualTo("lucas");
	}

	@Test
	void shouldReturnAllExpensesWhenListIsRequested() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("lucas", "abc123")
				.getForEntity("/expenses", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		int expenseCount = documentContext.read("$.length()");
		assertThat(expenseCount).isEqualTo(3);

		JSONArray ids = documentContext.read("$..id");
		assertThat(ids).containsExactlyInAnyOrder(99, 100, 101);

		JSONArray amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactlyInAnyOrder(45.30, 12.00, 150.00);
	}

	@Test
	void shouldReturnAPageOfExpenses() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("lucas", "abc123")
				.getForEntity("/expenses?page=0&size=1", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(1);
	}

	@Test
	void shouldReturnASortedPageOfExpenses() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("lucas", "abc123")
				.getForEntity("/expenses?page=0&size=1&sort=amount,desc", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray read = documentContext.read("$[*]");
		assertThat(read.size()).isEqualTo(1);

		double amount = documentContext.read("$[0].amount");
		assertThat(amount).isEqualTo(150.00);
	}

	@Test
	void shouldReturnASortedPageOfExpensesWithNoParametersAndUseDefaultValues() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("lucas", "abc123")
				.getForEntity("/expenses", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(3);

		// Por defecto: del gasto más reciente al más antiguo
		JSONArray ids = documentContext.read("$..id");
		assertThat(ids).containsExactly(99, 100, 101);
	}

	@Test
	void shouldNotReturnAnExpenseWhenUsingBadCredentials() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("BAD-USER", "abc123")
				.getForEntity("/expenses/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		response = restTemplate
				.withBasicAuth("lucas", "BAD-PASSWORD")
				.getForEntity("/expenses/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldRejectUsersWhoAreNotExpenseOwners() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("noowner", "qrs456")
				.getForEntity("/expenses/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void shouldNotAllowAccessToExpensesTheyDoNotOwn() {
		// El gasto 102 es de "ana": lucas no debe poder verlo
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("lucas", "abc123")
				.getForEntity("/expenses/102", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}
