package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {
  @Mock
  NotificationService notificationService;

  @Mock
  UserService userService;

  @InjectMocks
  LibraryManager libraryManager;

  @Test
  void addBook(){
    libraryManager.addBook("book1", 10);
    assertEquals(10, libraryManager.getAvailableCopies("book1"));
  }

  @Test
  void addBookWhenBookAlreadyExists(){
    libraryManager.addBook("book1", 10);
    libraryManager.addBook("book1", 20);
    assertEquals(30, libraryManager.getAvailableCopies("book1"));
  }
  @ParameterizedTest
  @CsvSource({
      "user1, book1",
      "user2, book2"
  })
  void borrowBookAccountIsNotActive(String userId, String bookId){
    when(userService.isUserActive(userId)).thenReturn(false);
    assertFalse(libraryManager.borrowBook(bookId, userId));
  }
  @ParameterizedTest
  @CsvSource({
      "user3, book3",
      "user4, book4"
  })
  void borrowBookNoCopiesAvailable(String userId, String bookId){
    libraryManager.addBook(bookId, 0);
    when(userService.isUserActive(userId)).thenReturn(true);
    assertFalse(libraryManager.borrowBook(bookId, userId));
  }
  @Test
  void borrowBookReductionQuantity(){
    String userId = "user1";
    String bookId = "book1";
    libraryManager.addBook(bookId, 1);
    when(userService.isUserActive(userId)).thenReturn(true);
    libraryManager.borrowBook(bookId, userId);
    assertEquals(0, libraryManager.getAvailableCopies(bookId));
    verify(notificationService, times(1)).notifyUser(userId, "You have borrowed the book: " + bookId);
  }

  @Test
  void returnBookWhenBookNotBorroved(){
    String userId = "user1";
    String bookId = "book1";

    assertFalse(libraryManager.returnBook(bookId,userId));
  }
  @Test
  void returnBookWhenSuccessful() {
    String userId = "user1";
    String bookId = "book1";

    when(userService.isUserActive(userId)).thenReturn(true);
    libraryManager.addBook(bookId, 1);
    libraryManager.borrowBook(bookId, userId);

    assertTrue(libraryManager.returnBook(bookId, userId));
    assertEquals(1, libraryManager.getAvailableCopies(bookId));
  }
  @Test
  void getAvailableCopiesWhenBookNotExists(){
    assertEquals(0, libraryManager.getAvailableCopies("book1"));
  }

  @Test
  void calculateDynamicLateFeeIfOverdueDaysNotPositive(){
    int overdueDays = -2;
    boolean isBestseller = true;
    boolean isPremiumMember = true;
    var exception = assertThrows(
        IllegalArgumentException.class,
        () -> libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember)
    );
    assertEquals("Overdue days cannot be negative.", exception.getMessage());
  }
  @ParameterizedTest
  @CsvSource({
      "2, true, true, 1.2",
      "2, true, false, 1.5",
      "2, false, true, 0.8",
      "2, false, false, 1",
  })
  void calculateDynamicLateFeeIfOverdueDaysPositive(int overdueDays, boolean isBestseller, boolean isPremiumMember, double fee) {
    assertEquals(fee, libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember));
  }
}