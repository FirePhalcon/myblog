package it.course.myblog;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class MyblogApplicationTests {

	@Test
	@DisplayName("Context Loads")
	void contextLoads() {
	}
	
	
//	@Test
//	void testFails() {
//		fail("Not yet implemented");
//	}
	
	@Test
	@DisplayName("Compare string 1")
	public void compareString() {
		String a = "alfa";
		String b = "alfa";
		
		assertTrue(a.equals(b), () -> "Strings should be equal");
	}
	
	@Test
	@DisplayName("Compare string 2")
	public void compareString2() {
		String a = "alfa";
		String b = "beta";
		
		assertFalse(a.equals(b), () -> "Strings should not be equal");
	}
	
	public List<String> list(){
		List<String> list = new ArrayList<String>();
		list.add("1");
		list.add("2");
		list.add("3");
		list.add("4");
		list.add("5");
		list.add("6");
		list.add("7");
		list.add("8");
		list.add("9");
		
		return list;
	}
	
	@Test
	@DisplayName("List")
	public void listTest() {

		
		assertAll(
				() -> assertEquals(9, list().size()),
				() -> assertTrue(list().contains("1")),
				() -> assertFalse(list().contains("10"))
				);
	}
	
	@Test
	@DisplayName("List 2")
	public void listTest2() {
		List<String> list = list();
		
		assertAll(
				() -> {
					assertNotNull(list);
					assertAll(
						() -> assertEquals(9, list.size()),
						() -> assertTrue(list.contains("2")),
						() -> assertFalse(list.contains("10"))
						);
					}
				);
	}
	
	@Test
	@DisplayName("List 3")
	public void listTest3() {
		List<String> listA = list();
		
		List<String> listB = list();
		//listB.add("0");
		
		assertAll(
				() -> assertLinesMatch(listA, listB),
				() -> assertLinesMatch(listA, listA.subList(0, listA.size()))
				);
	}
	
	@Test
	@DisplayName("Array 1")
	public void arrayTest() {
		
		String[] arrayA = {"W", "X", "Y"};
		String[] arrayB = {"W", "X", "Y"};
//		String[] arrayB = {"W", "Y", "X"};
//		String[] arrayC = {"Q", "F", "K"};
		
		assertArrayEquals(arrayA, arrayB);
//		assertArrayEquals(arrayA, arrayC);
	}
	
	@Test
	@DisplayName("timeout")
	public void timeout() {
		Boolean x = assertTimeout(Duration.ofSeconds(1), ()-> {
			TimeUnit.MILLISECONDS.sleep(500);
			return true;
			});
		assertTrue(x);
	 }
}
