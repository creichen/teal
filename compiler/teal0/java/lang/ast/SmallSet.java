/* Copyright (c) 2021, Idriss Riouak <idriss.riouak@cs.lth.se>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package lang.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Functional set datatype
 *
 * This set implementation (mostly) hides side effects, to prevent accidental
 * mistakes.
 *
 * The implementation uses various strategies to avoid set allocation, and any
 * method (static or not) that returns a <tt>SmallSet<tt> may or may not reuse the
 * same <tt>SmallSet<tt> for different purposes.
 */
public class SmallSet<T> implements Iterable<T> {
	protected LinkedHashSet<T> set = new LinkedHashSet<T>();

	/**
	 * Protected constructor.
	 *
	 * To construct a set from elements, use one of the following factory methods:
	 * <ul>
	 *   <li>Empty set: <tt>empty()</tt><li>
	 *   <li>Singleton: <tt>singleton(T)</tt>
	 *   <li>More than one element: <tt>from(T...)</tt>
	 *   <li>From array: <tt>from(T...)</tt>
	 *   <li>From iterable: <tt>from(Iterable<T>)</tt>
	 * </ul>
	 */
	protected SmallSet() {}

	/**
	 * Constructs a set with precisely one element.
	 */
	public static<T> SmallSet<T> singleton(T elt) {
		SmallSet<T> res = new SmallSet<T>();
		res.set.add(elt);
		return res;
	}

	/**
	 * Obtains the empty SmallSet.
	 */
	@SuppressWarnings("unchecked")
	public static <T> SmallSet<T> empty() {
		return (SmallSet) SmallSet.EMPTY;
	}

	/**
	 * Constructs a SmallSet from some fixed number of elements, or from an array of elements
	 *
	 * @param elts The elements that the set will contain (may be empty or contain duplicates).
	 * @return A set that contains precisely all the elements in <tt>elts</tt>.
	 */
	@SuppressWarnings({"all", "varargs"})
	public static<T> SmallSet<T> from(T ... elts) {
		if (elts.length == 0) {
			return SmallSet.<T>empty();
		}
		SmallSet<T> res = new SmallSet<T>();
		for (T elt : elts) {
			res.set.add(elt);
		}
		return res;
	}

	/**
	 * Constructs a SmallSet from an iterable object.
	 *
	 * @param elts The elements that the set will contain (may be empty or contain duplicates).
	 * @return A set that contains precisely all the elements in <tt>elts</tt>.
	 */
	public static<T> SmallSet<T> from(Iterable<T> iterable) {
		Iterator<T> it = iterable.iterator();
		if (!it.hasNext()) {
			return SmallSet.<T>empty();
		}
		SmallSet<T> res = new SmallSet<T>();
		do {
			res.set.add(it.next());
		} while (it.hasNext());
		return res;
	}

	/**
	 * Obtains the SmallSet that contains all elements.
	 *
	 * Since the actual elements of the set are unknown, the returned set
	 * does not support set differencing.
	 */
	@SuppressWarnings("unchecked")
	public static<T> SmallSet<T> full() {
		return FULL_SET;
	}

	/**
	 * Obtain a set that is the union of this set with another.
	 *
	 * @param set The set to combine with <tt>this</tt>
	 * @return The union of both sets
	 */
	public SmallSet<T> union(SmallSet<T> set) {
		if (set.isEmpty() || this.equals(set)) {
			return this;
		}
		SmallSet<T> newSet = new SmallSet<T>();
		newSet.set.addAll(this.set);
		newSet.set.addAll(set.set);
		return newSet;
	}

	/**
	 * Extend a set by an element (if the element is not already present)
	 *
	 * @param element The element to add (if not present already)
	 * @return A set that contains <tt>element</tt> as well as all elements contained in <tt>this</tt>.
	 */
	public SmallSet<T> with(T element) {
		if (contains(element)) {
			return this;
		}
		SmallSet<T> newSet = new SmallSet<T>();
		newSet.set.addAll(this.set);
		newSet.set.add(element);
		return newSet;
	}

	/**
	 * Compute the set difference.
	 *
	 * @param set The set to subtract from <tt>this</tt>.
	 * @return A set that contains precisely all elements in <tt>this</tt> that are not
	 * also contained in <tt>set</tt>.
	 */
	public SmallSet<T> minus(SmallSet<T> set) {
		if (set.isEmpty()) {
			return this;
		}
		SmallSet<T> newSet = new SmallSet<T>();
		newSet.set.addAll(this.set);
		newSet.set.removeAll(set.set);
		return newSet;
	}

	/**
	 * Set difference with a singleton.
	 *
	 * @param element The element to exclude from the returned set.
	 * @return A set that contains precisely all elements in <tt>this</tt> that are
	 *   not equal to <tt>element</tt>.
	 */
	public SmallSet<T> without(Object element) {
		if (!set.contains(element)) {
			return this;
		}
		SmallSet<T> newSet = new SmallSet<T>();
		newSet.set.addAll(this.set);
		newSet.set.remove(element);
		return newSet;
	}

	/**
	 * Set intersection.
	 *
	 * @param set The set to intersect with.
	 * @return A set that contains precisely all elements that are contained in both
	 *   <tt>this</tt> and <tt>set</tt>.
	 */
	public SmallSet<T> intersect(SmallSet<T> set) {
		if (this.equals(set) || set == FULL_SET) {
			return this;
		}
		SmallSet<T> newSet = new SmallSet<T>();
		newSet.set.addAll(this.set);
		newSet.set.retainAll(set.set);
		return newSet;
	}

	/**
	 * Checks whether this set contains a specific element.
	 *
	 * @param o The object to check for
	 * @return <tt>true</tt> iff <tt>this</tt> contains the element <tt>o</tt>.
	 */
	public boolean contains(Object o) {
		return set.contains(o);
	}

	/**
	 * Test for set inclusion against another set
	 *
	 * @param that The set to compare against
	 * @return <tt>true</tt> iff <tt>that</tt> contains at least all elements in <tt>this</tt>
	 */
	public boolean isSubsetOf(SmallSet<? extends T> that) {
		for (T x : set) {
			if (!that.contains(x)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Determine the cardinality / size of the set
	 *
	 * @return The number of elements in the set
	 */
	public int size() {
		return set.size();
	}

	/**
	 * Test if this set is the empty set
	 *
	 * Note that there may be multiple <tt>SmallSet</tt> objects that are empty,
	 * at least if you construct sets via <tt>mutable()</tt>.
	 *
	 * @return <tt>true</tt> iff this set is empty.
	 */
	public boolean isEmpty() {
		return set.isEmpty();
	}

	/**
	 * Test if this set contains precisely one element.
	 *
	 * @return <tt>true</tt> iff the cardinality of this set is 1.
	 */
	public boolean isSingleton() {
		return set.size() == 1;
	}

	@SuppressWarnings("unchecked")
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (this == o) {
			return true;
		}
		if (o instanceof SmallSet) {
			SmallSet<T> set = (SmallSet)o;
			return this.set.equals(set.set);
		}
		return super.equals(o);
	}

	public Iterator<T> iterator() {
		return set.iterator();
	}

	public Stream<T> stream() {
		return this.set.stream();
	}

	private static final SmallSet<Object> EMPTY = new SmallSet<Object>(){
		@Override
		public boolean equals(Object o) {
			return o instanceof SmallSet && ((SmallSet)o).isEmpty();
		}

		@Override
		public SmallSet<Object> union(SmallSet<Object> set) {
			return set;
		}

		@Override
		public SmallSet<Object> with(Object element) {
			return SmallSet.<Object>singleton(element);
		}

		@Override
		public SmallSet<Object> minus(SmallSet<Object> set) {
			return this;
		}

		@Override
		public SmallSet<Object> without(Object element) {
			return this;
		}

		@Override
		public SmallSet<Object> intersect(SmallSet<Object> set) {
			return this;
		}

		@Override
		public boolean isEmpty() {
			return true;
		}

		@Override
		public boolean isSingleton() {
			return false;
		}

		@Override
		public int size() {
			return 0;
		}

		@Override
		public boolean isSubsetOf(SmallSet<?> that) {
			return true;
		}
	};

	@SuppressWarnings("unchecked")
	private static SmallSet FULL_SET = new SmallSet() {
		@Override
		public String toString(){
			return "full set";
		}

		@Override
		public SmallSet union(SmallSet set) {
			return this;
		}

		@Override
		public SmallSet with(Object element) {
			return this;
		}

		@Override
		public SmallSet minus(SmallSet set) {
			throw new Error("compl not supported for the full set");
		}

		@Override
		public SmallSet without(Object element) {
			throw new Error("compl not supported for the full set");
		}

		@Override
		public SmallSet intersect(SmallSet set) {
			return set;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public boolean isSingleton() {
			return false;
		}

		@Override
		public int size() {
			throw new UnsupportedOperationException("fullSet.size()");
		}

		@Override
		public boolean isSubsetOf(SmallSet that) {
			throw new UnsupportedOperationException("fullSet.isSubsetOf");
		}
	};


	/**
	 * Constructs a mutable set.
	 *
	 * Avoid using this unless you know what you're doing.
	 */
	public static<T> SmallSet.Mutable<T> mutable() {
	 	return new SmallSet.Mutable<T>();
	}

	public SmallSet<T> union(SmallSet<T> ... sets) {
		if (sets.length == 0) {
			return this;
		}
		SmallSet.Mutable<T> result = null;
		for (SmallSet<T> set : sets) {
			if (!set.isEmpty() && !set.equals(this)) {
				if (result == null) {
					result = new SmallSet.Mutable<>();
					result.addAll(this);
				}
				result.addAll(set);
			}
		}

		if (result == null) {
			return this;
		}
		return result;
	}

	@Override
	public String toString() {
		return set.toString();
	}

	/**
	 * A mutable SmallSet.
	 *
	 * Only use this locally.
	 *
	 * <b>Only</b> call <tt>add()</tt> or <tt>addAll()</tt> <b>before</b> you use
	 * the functional API with a <tt>SmallSet.Mutable</tt>.
	 *
	 * Rationale: Passing a <tt>SmallSet.Mutable</tt> into the functional API
	 * or invoking functional API methods on a <tt>SmallSet.Mutable</tt> may return
	 * the <tt>SmallSet.Mutable</tt> object.
	 */
	public static class Mutable<T> extends SmallSet<T> {
		/**
		 * Update the mutable <tt>SmallSet</tt> by adding all elements in the
		 * specified set.
		 *
		 * <b>Be careful if you choose to use this</b> (see above.)
		 */
		public void addAll(SmallSet<T> set) {
			for (T v : set) {
				this.set.add(v);
			}
		}

		/**
		 * Update the mutable <tt>SmallSet</tt> by adding an element to the
		 * specified set.
		 *
		 * <b>Be careful if you choose to use this</b> (see above.)
		 */
		public void add(T o) {
			this.set.add(o);
		}
	}
}
