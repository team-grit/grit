package fraction;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestIFraction {

	/** EPSILON for rounding errors. */
	private final static double EPSILON = 0.0001;

	@Test
	public final void testConstructors() {
		IFraction f = new Fraction();
		assertTrue("Denominator must be greater 0", f.getDenominator() > 0);
		assertEquals("Value must be 0", 0, f.getNumerator(), EPSILON);
		f = new Fraction(5);
		assertTrue("Denominator must be greater 0", f.getDenominator() > 0);
		assertEquals("gcd must be 1", 5, f.getNumerator(), EPSILON);
		assertEquals("gcd must be 1", 1, f.getDenominator(), EPSILON);
		f = new Fraction(2, 3);
		assertTrue("Denominator must be greater 0", f.getDenominator() > 0);
		assertEquals("gcd must be 1", 2, f.getNumerator(), EPSILON);
		assertEquals("gcd must be 1", 3, f.getDenominator(), EPSILON);
	}

	@Test
	public final void testGetNumerator() {
		IFraction frac = new Fraction(5);
		assertEquals(5, frac.getNumerator());
	}

	@Test
	public final void testGetDenominator() {
		IFraction frac = new Fraction(1, 5);
		assertEquals(5, frac.getDenominator());
	}

	@Test
	public final void testAdd() {
		IFraction f1 = new Fraction(1, 2);
		IFraction f2 = new Fraction(1, 3);
		IFraction sum = f1.add(f2);
		assertEquals("Numerator not correct", 5, sum.getNumerator(), EPSILON);
		assertEquals("Denominator not correct", 6, sum.getDenominator(),
				EPSILON);
		assertEquals("Fraction should not change", 1, f1.getNumerator(),
				EPSILON);
		assertEquals("Fraction should not change", 2, f1.getDenominator(),
				EPSILON);
		assertEquals("Fraction should not change", 1, f2.getNumerator(),
				EPSILON);
		assertEquals("Fraction should not change", 3, f2.getDenominator(),
				EPSILON);
	}

	@Test
	public final void testSub() {
		IFraction f1 = new Fraction(1, 2);
		IFraction f2 = new Fraction(1, 3);
		IFraction diff = f1.sub(f2);
		assertEquals("Numerator not correct", 1, diff.getNumerator(), EPSILON);
		assertEquals("Denominator not correct", 6, diff.getDenominator(),
				EPSILON);
		assertEquals("Fraction should not change", 1, f1.getNumerator(),
				EPSILON);
		assertEquals("Fraction should not change", 2, f1.getDenominator(),
				EPSILON);
		assertEquals("Fraction should not change", 1, f2.getNumerator(),
				EPSILON);
		assertEquals("Fraction should not change", 3, f2.getDenominator(),
				EPSILON);
	}

	@Test
	public final void testMult() {
		IFraction f1 = new Fraction(2, 3);
		IFraction f2 = new Fraction(1, 5);
		IFraction prod = f1.mult(f2);
		assertEquals("Numerator not correct", 2, prod.getNumerator(), EPSILON);
		assertEquals("Denominator not correct", 15, prod.getDenominator(),
				EPSILON);
		assertEquals("Fraction should not change", 2, f1.getNumerator(),
				EPSILON);
		assertEquals("Fraction should not change", 3, f1.getDenominator(),
				EPSILON);
		assertEquals("Fraction should not change", 1, f2.getNumerator(),
				EPSILON);
		assertEquals("Fraction should not change", 5, f2.getDenominator(),
				EPSILON);
	}

	@Test
	public final void testDiv() {
		IFraction f1 = new Fraction(2, 3);
		IFraction f2 = new Fraction(1, 5);
		IFraction div = f1.div(f2);
		assertEquals("Numerator not correct", 10, div.getNumerator(), EPSILON);
		assertEquals("Denominator not correct", 3, div.getDenominator(),
				EPSILON);
		assertEquals("Fraction should not change", 2, f1.getNumerator(),
				EPSILON);
		assertEquals("Fraction should not change", 3, f1.getDenominator(),
				EPSILON);
		assertEquals("Fraction should not change", 1, f2.getNumerator(),
				EPSILON);
		assertEquals("Fraction should not change", 5, f2.getDenominator(),
				EPSILON);
	}

	@Test
	public final void testNegated() {
		IFraction f = new Fraction(1, 5);
		IFraction fNeg = f.negated();
		IFraction fDoubleNeg = fNeg.negated();


		assertEquals("f + (-f) should be zero", 0, f.add(fNeg).getNumerator(),
				EPSILON);
		assertEquals("Double negation failed", f.getNumerator(),
				fDoubleNeg.getNumerator(), EPSILON);
		assertEquals("Double negation failed", f.getDenominator(),
				fDoubleNeg.getDenominator(), EPSILON);
		assertEquals("Fraction should not change", 1, f.getNumerator(), EPSILON);
		assertEquals("Fraction should not change", 5, f.getDenominator(),
				EPSILON);
	}

	@Test
	public final void testAsDouble() {
		Fraction f = new Fraction(1, 2);
		assertEquals("Wrong value as double", 0.5, f.asDouble(), EPSILON);
		f = new Fraction(1, 3);
		assertEquals("Wrong value as double", 0.33333, f.asDouble(), EPSILON);
	}
}
