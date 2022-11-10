package lang;

import lang.common.SourceLocation;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestSourceLocation {
	private static SourceLocation
	loc(int sl, int sc,
	    int el, int ec) {
		return new SourceLocation("foo", sl, sc, el, ec);
	}

	private static SourceLocation
	floc(String filename,
	     int sl, int sc,
	     int el, int ec) {
		return new SourceLocation(filename, sl, sc, el, ec);
	}

	@Test
	public void
	equalsTest() {
		assertEquals(loc(1, 2, 3, 4),
			     loc(1, 2, 3, 4));

		assertNotEquals(loc(1, 2, 3, 4),
				loc(1, 2, 3, 3));
		assertNotEquals(loc(1, 2, 3, 4),
				loc(1, 2, 3, 5));

		assertNotEquals(loc(1, 2, 3, 4),
				loc(1, 2, 2, 4));
		assertNotEquals(loc(1, 2, 3, 4),
				loc(1, 2, 4, 4));

		assertNotEquals(loc(1, 2, 3, 4),
				loc(1, 1, 3, 4));
		assertNotEquals(loc(1, 2, 3, 4),
				loc(1, 3, 3, 4));

		assertNotEquals(loc(2, 2, 3, 4),
				loc(3, 2, 3, 4));
		assertNotEquals(loc(2, 2, 3, 4),
				loc(1, 2, 3, 4));

		assertNotEquals(floc("foo", 1, 2, 3, 4),
				floc("fop", 1, 2, 3, 4));
		assertNotEquals(floc("foo", 1, 2, 3, 4),
				floc("fon", 1, 2, 3, 4));
	}

	@Test
	public void
	readSourceLocation() {
		for (SourceLocation loc : new SourceLocation[] {
				loc(1, 1, 1, 1),
				loc(11, 22, 33, 44),
			}) {
			SourceLocation read = SourceLocation.fromString(loc.toString());
			assertEquals(loc, read);
		}
	}

	@Test
	public void
	within() {
		assertTrue(loc(1, 2, 3, 4).within(loc(1, 2, 3, 4)));
		// different file
		assertFalse(floc("foo", 1, 2, 3, 4)
			    .within(floc("bar", 1, 2, 3, 4)));

		// -- multiline
		// left boundary
		assertTrue(        loc(10, 10,
				       10, 10)
			   .within(loc(10, 10,
				       20, 20)));

		assertFalse(       loc(10,  9,
				       10, 10)
			   .within(loc(10, 10,
				       20, 20)));

		assertFalse(       loc( 9, 10,
				       10, 10)
			   .within(loc(10, 10,
				       20, 20)));

		assertTrue(        loc(10, 11,
				       10, 11)
			   .within(loc(10, 10,
				       20, 20)));

		assertTrue(        loc(11, 10,
				       11, 10)
			   .within(loc(10, 10,
				       20, 20)));

		assertTrue(        loc(11,  2,
				       11, 40)
			   .within(loc(10, 10,
				       20, 20)));


		// right boundary
		assertTrue(        loc(20, 20,
				       20, 20)
			   .within(loc(10, 10,
				       20, 20)));

		assertFalse(       loc(20, 20,
				       20, 21)
			   .within(loc(10, 10,
				       20, 20)));

		assertFalse(       loc(20, 10,
				       21, 10)
			   .within(loc(10, 10,
				       20, 20)));

		assertTrue(        loc(20, 19,
				       20, 19)
			   .within(loc(10, 10,
				       20, 20)));

		assertTrue(        loc(19, 20,
				       19, 20)
			   .within(loc(10, 10,
				       20, 20)));

		assertTrue(        loc(19, 2,
				       19, 80)
			   .within(loc(10, 10,
				       20, 20)));


		// -- single line
		assertTrue(        loc(1, 10,
				       1, 10)
			   .within(loc(1, 10,
				       1, 10)));

		assertTrue(        loc(1, 10,
				       1, 20)
			   .within(loc(1, 10,
				       1, 20)));

		assertTrue(        loc(1, 11,
				       1, 20)
			   .within(loc(1, 10,
				       1, 20)));

		assertTrue(        loc(1, 10,
				       1, 19)
			   .within(loc(1, 10,
				       1, 20)));

		assertTrue(        loc(1, 11,
				       1, 19)
			   .within(loc(1, 10,
				       1, 20)));

		assertFalse(       loc(1,  9,
				       1, 19)
			   .within(loc(1, 10,
				       1, 20)));

		assertFalse(       loc(1,  9,
				       1, 21)
			   .within(loc(1, 10,
				       1, 20)));

		assertFalse(       loc(1, 11,
				       1, 21)
			   .within(loc(1, 10,
				       1, 20)));

		// left boundary
		assertTrue(        loc(1, 10,
				       1, 10)
			   .within(loc(1, 10,
				       1, 20)));
		assertTrue(        loc(1, 10,
				       1, 11)
			   .within(loc(1, 10,
				       1, 20)));
		assertFalse(       loc(1,  9,
				       1, 10)
			   .within(loc(1, 10,
				       1, 20)));

		// right boundary
		assertTrue(        loc(1, 20,
				       1, 20)
			   .within(loc(1, 10,
				       1, 20)));
		assertTrue(        loc(1, 19,
				       1, 20)
			   .within(loc(1, 10,
				       1, 20)));
		assertFalse(       loc(1, 19,
				       1, 21)
			   .within(loc(1, 10,
				       1, 20)));

    }
}
