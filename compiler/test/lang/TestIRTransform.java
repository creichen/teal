package lang;

import org.junit.Test;
import static org.junit.Assert.*;

import lang.ir.*;

public class TestIRTransform {
	@Test
	public void testInsertInsn() {
		IRCodeBB bb = new IRCodeBB();
		// insert newarray at position 0
		bb.insertInsn(new IRNewArrayInsn(), 0);
		// insert another instruction at position 0, that pushes
		// the newarray at position 1
		bb.insertInsn(new IRCopyInsn(), 0);

		assertEquals(2, bb.getNumIRInsn());
		assertTrue(bb.getIRInsn(0) instanceof IRCopyInsn);
		assertTrue(bb.getIRInsn(1) instanceof IRNewArrayInsn);
	}

	@Test
	public void testInsertBefore() {
		IRCodeBB bb = new IRCodeBB();

		IRInsn i0 = new IRNewArrayInsn();
		bb.addIRInsn(i0);

		i0.addInsnBefore(new IRCopyInsn()).addInsnBefore(new IRNewInsn());


		assertEquals(3, bb.getNumIRInsn());
		assertTrue(bb.getIRInsn(0) instanceof IRCopyInsn);
		assertTrue(bb.getIRInsn(1) instanceof IRNewInsn);
		assertTrue(bb.getIRInsn(2) instanceof IRNewArrayInsn);
	}

	@Test
	public void testInsertAfter() {
		IRCodeBB bb = new IRCodeBB();

		IRInsn i0 = new IRNewArrayInsn();
		bb.addIRInsn(i0);

		i0.addInsnAfter(new IRCopyInsn()).addInsnAfter(new IRNewInsn());

		assertEquals(3, bb.getNumIRInsn());
		assertTrue(bb.getIRInsn(0) instanceof IRNewArrayInsn);
		assertTrue(bb.getIRInsn(1) instanceof IRCopyInsn);
		assertTrue(bb.getIRInsn(2) instanceof IRNewInsn);
	}
}
