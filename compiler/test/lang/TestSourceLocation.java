package lang;

import lang.common.SourceLocation;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestSourceLocation {
    @Test
    public void
    includesTest() {
	assertEquals(new SourceLocation("foo", 1, 2, 3, 4),
		     new SourceLocation("foo", 1, 2, 3, 4));
    }
}
