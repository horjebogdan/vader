package com.savatech.vader;

import org.junit.Assert;
import org.junit.Test;

public class TimestampTest {

	@Test
	public void testIn() throws Exception {
		final String s = "ffeooo [@00:00:01.200] dsadsd";
		Timestamp ts = Timestamp.in(s, s.length() - 1);
		Assert.assertEquals(new Timestamp(1200), ts);
	}
	
	@Test
	public void testInvalidIn() throws Exception {
		final String s = "ffeooo [@a00:00:01.200] dsadsd";
		Timestamp ts = Timestamp.in(s, s.length() - 1);
		Assert.assertNull(ts);
	}

	@Test
	public void testSkipInvalidIn() throws Exception {
		final String s = " mo hh dhsd s [@00:00:02.200] ffeooo [@a00:00:01.200] dsadsd";
		Timestamp ts = Timestamp.in(s, s.length() - 1);
		Assert.assertEquals(new Timestamp(2200), ts);
	}

}
