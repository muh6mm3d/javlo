package org.javlo.helper;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

public class StringHelperTest extends TestCase {
	
  public void testIsImage() throws Exception {
	  assertTrue(StringHelper.isImage("test.jpg"));
	  assertTrue(StringHelper.isImage("test.png"));
	  assertTrue(StringHelper.isImage("test.gif"));
	  assertTrue(!StringHelper.isImage("test.xml"));
	  assertTrue(!StringHelper.isImage("/test.xml?param=value"));
	  assertTrue(!StringHelper.isImage("/test"));
	  assertTrue(!StringHelper.isImage(""));
	  assertTrue(!StringHelper.isImage("/"));
	  assertTrue(!StringHelper.isImage("http://host.com/rss/rss.html;jsessionid=97F113FC3A1036324B2889FD5E795F8E.node1"));	  
	  assertTrue(!StringHelper.isImage(null));
  }
  
  public void testIsVideo() throws Exception {
	  assertTrue(StringHelper.isVideo("test.mp4"));
	  assertTrue(!StringHelper.isVideo("test.xml"));
	  assertTrue(!StringHelper.isVideo("/test.xml?param=value"));
	  assertTrue(!StringHelper.isVideo("/test"));
	  assertTrue(!StringHelper.isVideo(""));
	  assertTrue(!StringHelper.isVideo("/"));
	  assertTrue(!StringHelper.isVideo(null));
  }
  
  public void testGetFileExtension() throws Exception {
	  assertTrue(StringHelper.getFileExtension("test.jpg").equals("jpg"));
	  assertTrue(StringHelper.getFileExtension("test.xml").equals("xml"));
	  assertTrue(StringHelper.getFileExtension("test.xml?coucou=test").equals("xml"));
	  assertTrue(StringHelper.getFileExtension("test.xml;jsessionid=6D7A0CD25887B1A3C4CAECFBA17ACB0D").equals("xml"));
	  assertTrue(StringHelper.getFileExtension("test.xml;jsessionid=6D7A0CD25887B1A3C4CAECFBA17ACB0D?coucou=test").equals("xml"));
	  assertTrue(StringHelper.getFileExtension("test.xml?coucou=test;jsessionid=6D7A0CD25887B1A3C4CAECFBA17ACB0D").equals("xml"));
	  assertTrue(StringHelper.getFileExtension(null).equals(""));
	  assertTrue(StringHelper.getFileExtension("test").equals(""));
  }
  
  public void testGetLanguageFromFileName() throws Exception {
	  assertTrue(StringHelper.getLanguageFromFileName("test_en.jpg").equals("en"));
	  assertTrue(StringHelper.getLanguageFromFileName("test_nolang.jpg") == null);
	  assertTrue(StringHelper.getLanguageFromFileName("test.jpg") == null);	  
	  assertTrue(StringHelper.getLanguageFromFileName(null) == null);
  }
  
  public void testStringToCollection() throws Exception {	  
		List<String> testList = Arrays.asList(new String[] { "item1", "item\\, 2", "item3\\" });
		String rawTest = StringHelper.collectionToString(testList, ", ");
		assertEquals(rawTest, "item1, item\\\\\\, 2, item3\\\\");
		List<String> list = StringHelper.stringToCollection(rawTest, ", ");
		int i = 0;
		for (String item : list) {
			assertEquals(item, testList.get(i));
			i++;
		}
		
		testList = Arrays.asList(new String[] { "item1", "item2", "item3", "item4" });
		rawTest = StringHelper.collectionToString(testList, ",");
		assertEquals(rawTest, "item1,item2,item3,item4");
		list = StringHelper.stringToCollection(rawTest, ",");
		i = 0;
		for (String item : list) {
			assertEquals(item, testList.get(i));
			i++;
		}
		
		testList = Arrays.asList(new String[] { });
		rawTest = StringHelper.collectionToString(testList, ",");
		assertEquals(rawTest, "");		
		
		list = StringHelper.stringToCollection("item1,,,item4", ",");
		assertEquals(list.get(0), "item1");
		assertEquals(list.get(1), "");
		assertEquals(list.get(2), "");
		assertEquals(list.get(3), "item4");		
  }
  
  public void testGetFileNameFromPath() throws Exception {
	  assertEquals(StringHelper.getFileNameFromPath("/folder/file.jpg"), "file.jpg");
	  assertEquals(StringHelper.getFileNameFromPath("/folder/file.jpg?param=value"), "file.jpg");
	  assertEquals(StringHelper.getFileNameFromPath("http://host.com/folder/file.jpg?param=value"), "file.jpg");
	  assertEquals(StringHelper.getFileNameFromPath("http://host.com/folder/1234"), "1234");
  }
  
  public void testEncodeAsStructuredCommunicationMod97() throws Exception {
	  assertEquals(StringHelper.encodeAsStructuredCommunicationMod97("3542232806"), "354/2232/80695");
	  assertEquals(StringHelper.encodeAsStructuredCommunicationMod97("0000000007"), "000/0000/00707");
  }
  
 
}
