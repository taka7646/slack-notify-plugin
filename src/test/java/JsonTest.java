import static org.junit.Assert.*;

import org.junit.Test;

import net.sf.json.JSONObject;

public class JsonTest{
	@Test
	public void testJsonParse(){
		JSONObject o = JSONObject.fromObject("{\"id\":\"1243206985728205_1280125435369693\"}");
		assertEquals("1243206985728205_1280125435369693", o.getString("id"));
		System.out.println(o);
	}
}