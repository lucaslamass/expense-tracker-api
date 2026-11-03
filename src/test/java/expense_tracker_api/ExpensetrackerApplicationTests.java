package expense_tracker_api;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.Map;

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

	@Test
	@DirtiesContext
	void shouldUpdateAnExistingExpense() {
		Expense expenseUpdate = new Expense(null, "Compra supermercado grande",
				new BigDecimal("60.00"), LocalDate.of(2026, 11, 3), "Comida", null);
		HttpEntity<Expense> request = new HttpEntity<>(expenseUpdate);

		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("lucas", "abc123")
				.exchange("/expenses/99", HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("lucas", "abc123")
				.getForEntity("/expenses/99", String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
		Number id = documentContext.read("$.id");
		String description = documentContext.read("$.description");
		Double amount = documentContext.read("$.amount");
		assertThat(id).isEqualTo(99);
		assertThat(description).isEqualTo("Compra supermercado grande");
		assertThat(amount).isEqualTo(60.00);
	}

	@Test
	void shouldNotUpdateAnExpenseThatDoesNotExist() {
		Expense unknownExpense = new Expense(null, "No existe",
				new BigDecimal("10.00"), LocalDate.of(2026, 11, 3), "Comida", null);
		HttpEntity<Expense> request = new HttpEntity<>(unknownExpense);

		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("lucas", "abc123")
				.exchange("/expenses/99999", HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotUpdateAnExpenseThatIsOwnedBySomeoneElse() {
		Expense anaExpense = new Expense(null, "Gasolina",
				new BigDecimal("1.00"), LocalDate.of(2026, 11, 2), "Transporte", null);
		HttpEntity<Expense> request = new HttpEntity<>(anaExpense);

		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("lucas", "abc123")
				.exchange("/expenses/102", HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	@DirtiesContext
	void shouldDeleteAnExistingExpense() {
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("lucas", "abc123")
				.exchange("/expenses/99", HttpMethod.DELETE, null, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("lucas", "abc123")
				.getForEntity("/expenses/99", String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotDeleteAnExpenseThatDoesNotExist() {
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("lucas", "abc123")
				.exchange("/expenses/99999", HttpMethod.DELETE, null, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotAllowDeletionOfExpensesTheyDoNotOwn() {
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("lucas", "abc123")
				.exchange("/expenses/102", HttpMethod.DELETE, null, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

		// El gasto de ana sigue existiendo
		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("ana", "xyz789")
				.getForEntity("/expenses/102", String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void shouldRejectAnExpenseWithInvalidData() {
		Expense invalidExpense = new Expense(null, "",
				new BigDecimal("-5.00"), LocalDate.of(2026, 11, 4), "Comida", null);

		ResponseEntity<String> response = restTemplate
				.withBasicAuth("lucas", "abc123")
				.postForEntity("/expenses", invalidExpense, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		String descriptionError = documentContext.read("$.errors.description");
		String amountError = documentContext.read("$.errors.amount");
		assertThat(descriptionError).isNotBlank();
		assertThat(amountError).isNotBlank();
	}

	@Test
	void shouldReturnASummaryOfTheUsersExpenses() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("lucas", "abc123")
				.getForEntity("/expenses/summary", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		double total = documentContext.read("$.total");
		assertThat(total).isEqualTo(207.30);

		double food = documentContext.read("$.byCategory.Comida");
		assertThat(food).isEqualTo(45.30);

		// La gasolina es de ana: no debe aparecer en el resumen de lucas
		Map<String, Object> byCategory = documentContext.read("$.byCategory");
		assertThat(byCategory).hasSize(3).doesNotContainKey("Transporte");
	}
}
