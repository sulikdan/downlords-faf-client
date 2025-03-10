package com.faforever.client.query;

import com.faforever.client.i18n.I18n;
import com.faforever.client.test.PlatformTest;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.properties.concrete.StringProperty;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.MenuButton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CategoryFilterControllerTest extends PlatformTest {

  private final String propertyName = "testProp";
  private final List<String> items = Arrays.asList("test1", "test2");
  private final LinkedHashMap<String, String> itemMap = new LinkedHashMap<>();

  @InjectMocks
  private CategoryFilterController instance;
  @Mock
  private InvalidationListener queryListener;
  @Mock
  private I18n i18n;

  @BeforeEach
  public void setUp() throws Exception {
    itemMap.put("1", items.get(0));
    itemMap.put("2", items.get(1));

    loadFxml("theme/vault/search/categoryFilter.fxml", clazz -> instance);

    instance.setPropertyName(propertyName);
    instance.setItems(items);
  }

  @Test
  public void testSetItems() throws Exception {
    instance.setItems(items);

    assertArrayEquals(items.toArray(), instance.checkListView.getItems().toArray());
  }

  @Test
  public void testSetItemMap() throws Exception {
    instance.setItems(itemMap);

    assertArrayEquals(itemMap.keySet().toArray(), instance.checkListView.getItems().toArray());
  }

  @Test
  public void testAddListener() throws Exception {
    instance.addQueryListener(queryListener);
    instance.checkListView.getItemBooleanProperty(0).setValue(true);

    verify(queryListener).invalidated(any());
  }

  @Test 
  public void testListenerInvalidatesOnClear() throws Exception {
    instance.checkListView.getItemBooleanProperty(0).setValue(true);
    instance.addQueryListener(queryListener);
    instance.clear();

    verify(queryListener).invalidated(any());
  }

  @Test
  public void testClear() throws Exception {
    instance.checkListView.getItems().forEach(item ->
        instance.checkListView.getItemBooleanProperty(item).setValue(true));
    instance.clear();

    instance.checkListView.getItems().forEach(item ->
        assertFalse(instance.checkListView.getItemBooleanProperty(item).getValue()));
    assertFalse(instance.checkListView.getItems().isEmpty());
  }

  @Test
  public void testSetTitle() throws Exception {
    when(i18n.get(anyString(), any(), any())).thenReturn("Test");
    instance.setTitle("Test");

    assertTrue(instance.menu.textProperty().isBound());
    assertEquals("Test", instance.menu.getText());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertTrue(instance.getRoot() instanceof MenuButton);
  }

  @Test
  public void testGetConditionNonChecked() throws Exception {
    assertTrue(instance.getCondition().isEmpty());
    assertFalse(instance.menu.getStyleClass().contains("query-filter-selected"));
  }

  @Test
  public void testGetConditionCheckedNoMap() throws Exception {
    StringProperty property = new QBuilder<>().string(propertyName);
    instance.checkListView.getItems().forEach(item ->
        instance.checkListView.getItemBooleanProperty(item).setValue(true));

    Optional<List<Condition>> result = instance.getCondition();

    assertTrue(result.isPresent());
    assertEquals(result.get().getFirst().query(new RSQLVisitor()),
                 property.in(instance.checkListView.getCheckModel().getCheckedItems().toArray())
                         .query(new RSQLVisitor()));
    assertTrue(instance.menu.getStyleClass().contains("query-filter-selected"));
  }

  @Test
  public void testGetConditionCheckedWithMap() throws Exception {
    instance.setItems(itemMap);
    StringProperty property = new QBuilder<>().string(propertyName);
    instance.checkListView.getItems().forEach(item ->
        instance.checkListView.getItemBooleanProperty(item).setValue(true));

    Optional<List<Condition>> result = instance.getCondition();

    assertTrue(result.isPresent());
    assertEquals(result.get().getFirst().query(new RSQLVisitor()),
        property.in(instance.checkListView.getCheckModel().getCheckedItems().stream().map(itemMap::get).toArray()).query(new RSQLVisitor()));
    assertTrue(instance.menu.getStyleClass().contains("query-filter-selected"));
  }

  @Test
  public void testPersistentPropertiesSetCheckedItems() {
    ObservableList<String> checkedItems = FXCollections.observableArrayList();
    checkedItems.add("2");
    ObjectProperty<ObservableList<String>> property = new SimpleObjectProperty<ObservableList<String>>(checkedItems);
    instance.setItems(itemMap);
    property.get().stream().forEach((item) -> instance.checkItem(item));
    property.bind(Bindings.createObjectBinding(() -> instance.getCheckedItems()));

    assertFalse(instance.checkListView.getCheckModel().isChecked("1"));
    assertTrue(instance.checkListView.getCheckModel().isChecked("2"));
  }

  @Test
  public void testPersistentPropertyBindsToCheckedItems() {
    ObjectProperty<ObservableList<String>> property = new SimpleObjectProperty<ObservableList<String>>(FXCollections.emptyObservableList());
    instance.setItems(itemMap);
    property.get().stream().forEach((item) -> instance.checkItem(item));
    property.bind(Bindings.createObjectBinding(() -> instance.getCheckedItems()));

    instance.checkListView.getCheckModel().check("1");

    ObservableList<String> expected = FXCollections.observableArrayList();
    expected.add("1");
    assertEquals(property.get(), expected);
  }
}
