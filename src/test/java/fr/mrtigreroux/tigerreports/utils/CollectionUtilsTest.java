package fr.mrtigreroux.tigerreports.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.mrtigreroux.tigerreports.utils.CollectionUtils.LimitedOrderedList;

/**
 * @author MrTigreroux
 */
class CollectionUtilsTest {
    
    @Test
    void testToString() {
        List<Integer> list1 = new ArrayList<>();
        int size1 = 5;
        for (int i = 0; i < size1; i++) {
            list1.add(i);
        }
        String list1Str = "[0, 1, 2, 3, 4](5)";
        assertEquals(list1Str, CollectionUtils.toString(list1));
        
        List<Integer> list2 = new ArrayList<>();
        int size2 = 8;
        for (int i = 0; i < size2; i++) {
            list2.add(i);
        }
        String list2Str = "[0, 1, 2, 3, 4, 5, 6, 7](8)";
        assertEquals(list2Str, CollectionUtils.toString(list2));
        
        List<Integer> list3 = new ArrayList<>();
        for (int i = 0; i < size2 - 1; i++) {
            list3.add(i);
        }
        list3.add(null);
        String list3Str = "[0, 1, 2, 3, 4, 5, 6, null](8)";
        assertEquals(list3Str, CollectionUtils.toString(list3));
        
        List<List<Integer>> listOfLists = new ArrayList<>();
        listOfLists.add(list1);
        listOfLists.add(list2);
        listOfLists.add(list3);
        listOfLists.add(list1);
        listOfLists.add(list2);
        String listOfListsStr = "[" + list1Str + ", " + list2Str + ", " + list3Str + ", " + list1Str
                + ", " + list2Str + "](5)";
        assertEquals(listOfListsStr, CollectionUtils.toString(listOfLists));
        
        Map<Integer, Integer> map1 = new HashMap<>();
        for (int i = 0; i < size1; i++) {
            map1.put(i, i);
        }
        String map1Str = "[0: 0, 1: 1, 2: 2, 3: 3, 4: 4](5)";
        assertEquals(map1Str, CollectionUtils.toString(map1));
        
        Map<Integer, List<Integer>> map2 = new HashMap<>();
        map2.put(0, list1);
        map2.put(1, list2);
        map2.put(2, list3);
        map2.put(3, list2);
        String map2Str = "[0: " + list1Str + ", 1: " + list2Str + ", 2: " + list3Str + ", 3: "
                + list2Str + "](4)";
        assertEquals(map2Str, CollectionUtils.toString(map2));
        
        Map<Integer, List<List<Integer>>> map3 = new HashMap<>();
        map3.put(0, listOfLists);
        map3.put(1, listOfLists);
        String map3Str = "[0: " + listOfListsStr + ", 1: " + listOfListsStr + "](2)";
        assertEquals(map3Str, CollectionUtils.toString(map3));
        
        Map<Integer, Map<Integer, Integer>> map4 = new HashMap<>();
        map4.put(0, map1);
        map4.put(1, map1);
        String map4Str = "[0: " + map1Str + ", 1: " + map1Str + "](2)";
        assertEquals(map4Str, CollectionUtils.toString(map4));
        
        Map<Integer, Map<Integer, List<Integer>>> map5 = new HashMap<>();
        map5.put(0, map2);
        map5.put(1, map2);
        String map5Str = "[0: " + map2Str + ", 1: " + map2Str + "](2)";
        assertEquals(map5Str, CollectionUtils.toString(map5));
        
        Map<Integer, Map<Integer, List<List<Integer>>>> map6 = new HashMap<>();
        map6.put(0, map3);
        map6.put(1, map3);
        String map6Str = "[0: " + map3Str + ", 1: " + map3Str + "](2)";
        assertEquals(map6Str, CollectionUtils.toString(map6));
        
        Map<Integer, Map<Integer, Map<Integer, Integer>>> map7 = new HashMap<>();
        map7.put(0, map4);
        map7.put(1, map4);
        String map7Str = "[0: " + map4Str + ", 1: " + map4Str + "](2)";
        assertEquals(map7Str, CollectionUtils.toString(map7));
        
    }
    
    /**
     * Test method for
     * {@link fr.mrtigreroux.tigerreports.utils.CollectionUtils#reversedList(java.util.List)}.
     */
    @Test
    void testReversedList() {
        List<Integer> list = new ArrayList<>();
        int size = 5;
        for (int i = 0; i < size; i++) {
            list.add(i);
        }
        
        int expected = size - 1;
        for (Integer integer : CollectionUtils.reversedList(list)) {
            assertEquals(expected, integer);
            expected--;
        }
    }
    
    @Test
    void testLimitedList() {
        int maxSize = 5;
        LimitedOrderedList<Integer> limitedList = new LimitedOrderedList<>(maxSize);
        assertEquals(0, limitedList.size());
        for (int i = 0; i < maxSize; i++) {
            limitedList.add(i);
            assertEquals(i + 1, limitedList.size());
            assertTrue(limitedList.contains(i));
            assertEquals(i, limitedList.indexOf(i));
        }
        assertEquals(maxSize, limitedList.size());
        for (int i = 0; i < maxSize; i++) {
            assertEquals(i, limitedList.get(i));
            assertTrue(limitedList.contains(i));
            assertEquals(i, limitedList.indexOf(i));
        }
        
        limitedList.add(5);
        assertEquals(maxSize, limitedList.size());
        for (int i = 0; i < maxSize; i++) {
            assertEquals(i + 1, limitedList.get(i));
            assertTrue(limitedList.contains(i + 1));
            assertEquals(i, limitedList.indexOf(i + 1));
        }
        
        assertThrows(UnsupportedOperationException.class, () -> limitedList.add(2, 1));
        Collection<Integer> simpleList = new ArrayList<>();
        assertThrows(UnsupportedOperationException.class, () -> limitedList.addAll(simpleList));
        assertThrows(UnsupportedOperationException.class, () -> limitedList.addAll(2, simpleList));
        assertThrows(UnsupportedOperationException.class, () -> limitedList.addFirst(2));
        assertThrows(UnsupportedOperationException.class, () -> limitedList.offerFirst(2));
        assertThrows(UnsupportedOperationException.class, () -> limitedList.push(2));
        assertThrows(UnsupportedOperationException.class, () -> limitedList.set(2, 2));
        assertThrows(UnsupportedOperationException.class, () -> limitedList.sort(null));
    }
    
}
