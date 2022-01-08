package org.oppia.android.util.math

import kotlin.math.absoluteValue
import org.oppia.android.app.model.Fraction
import org.oppia.android.app.model.Real
import org.oppia.android.app.model.Real.RealTypeCase.INTEGER
import org.oppia.android.app.model.Real.RealTypeCase.IRRATIONAL
import org.oppia.android.app.model.Real.RealTypeCase.RATIONAL
import org.oppia.android.app.model.Real.RealTypeCase.REALTYPE_NOT_SET
import kotlin.math.pow

val ZERO: Real by lazy {
  Real.newBuilder().apply { integer = 0 }.build()
}

val ONE: Real by lazy {
  Real.newBuilder().apply { integer = 1 }.build()
}

val ONE_HALF: Real by lazy {
  Real.newBuilder().apply {
    rational = Fraction.newBuilder().apply {
      numerator = 1
      denominator = 2
    }.build()
  }.build()
}

val REAL_COMPARATOR: Comparator<Real> by lazy { Comparator.comparing(Real::toDouble) }

fun Real.isRational(): Boolean = realTypeCase == RATIONAL

fun Real.isInteger(): Boolean = realTypeCase == INTEGER

fun Real.isWholeNumber(): Boolean {
  return when (realTypeCase) {
    RATIONAL -> rational.isOnlyWholeNumber()
    INTEGER -> true
    IRRATIONAL, REALTYPE_NOT_SET, null -> false
  }
}

fun Real.isNegative(): Boolean = when (realTypeCase) {
  RATIONAL -> rational.isNegative
  IRRATIONAL -> irrational < 0
  INTEGER -> integer < 0
  REALTYPE_NOT_SET, null -> throw Exception("Invalid real: $this.")
}

fun Real.isApproximatelyEqualTo(value: Double): Boolean {
  return toDouble().approximatelyEquals(value)
}

fun Real.isApproximatelyZero(): Boolean = isApproximatelyEqualTo(0.0)

fun Real.toDouble(): Double {
  return when (realTypeCase) {
    RATIONAL -> rational.toDouble()
    INTEGER -> integer.toDouble()
    IRRATIONAL -> irrational
    REALTYPE_NOT_SET, null -> throw Exception("Invalid real: $this.")
  }
}

fun Real.asWholeNumber(): Int? {
  return when (realTypeCase) {
    RATIONAL -> if (rational.isOnlyWholeNumber()) rational.toWholeNumber() else null
    INTEGER -> integer
    IRRATIONAL -> null
    REALTYPE_NOT_SET, null -> throw Exception("Invalid real: $this.")
  }
}

fun Real.toPlainText(): String = when (realTypeCase) {
  // Note that the rational part is first converted to an improper fraction since mixed fractions
  // can't be expressed as a single coefficient in typical polynomial syntax).
  RATIONAL -> rational.toImproperForm().toAnswerString()
  IRRATIONAL -> irrational.toPlainString()
  INTEGER -> integer.toString()
  REALTYPE_NOT_SET, null -> ""
}

operator fun Real.unaryMinus(): Real {
  return when (realTypeCase) {
    RATIONAL -> recompute { it.setRational(-rational) }
    IRRATIONAL -> recompute { it.setIrrational(-irrational) }
    INTEGER -> recompute { it.setInteger(-integer) }
    REALTYPE_NOT_SET, null -> throw Exception("Invalid real: $this.")
  }
}

operator fun Real.plus(rhs: Real): Real {
  return combine(
    this, rhs, Fraction::plus, Fraction::plus, Fraction::plus, Double::plus, Double::plus,
    Double::plus, Int::plus, Int::plus, Int::add
  )
}

operator fun Real.minus(rhs: Real): Real {
  return combine(
    this, rhs, Fraction::minus, Fraction::minus, Fraction::minus, Double::minus, Double::minus,
    Double::minus, Int::minus, Int::minus, Int::subtract
  )
}

operator fun Real.times(rhs: Real): Real {
  return combine(
    this, rhs, Fraction::times, Fraction::times, Fraction::times, Double::times, Double::times,
    Double::times, Int::times, Int::times, Int::multiply
  )
}

operator fun Real.div(rhs: Real): Real {
  return combine(
    this, rhs, Fraction::div, Fraction::div, Fraction::div, Double::div, Double::div, Double::div,
    Int::div, Int::div, Int::divide
  )
}

fun Real.pow(rhs: Real): Real {
  // Powers can really only be effectively done via floats or whole-number only fractions.
  return when (realTypeCase) {
    RATIONAL -> {
      // Left-hand side is Fraction.
      when (rhs.realTypeCase) {
        // Anything raised by a fraction is pow'd by the numerator and rooted by the denominator.
        RATIONAL -> rhs.rational.toImproperForm().let { power ->
          rational.pow(power.numerator).root(power.denominator, power.isNegative)
        }
        IRRATIONAL -> recompute { it.setIrrational(rational.pow(rhs.irrational)) }
        INTEGER -> recompute { it.setRational(rational.pow(rhs.integer)) }
        REALTYPE_NOT_SET, null -> throw Exception("Invalid real: $rhs.")
      }
    }
    IRRATIONAL -> {
      // Left-hand side is a double.
      when (rhs.realTypeCase) {
        RATIONAL -> recompute { it.setIrrational(irrational.pow(rhs.rational)) }
        IRRATIONAL -> recompute { it.setIrrational(irrational.pow(rhs.irrational)) }
        INTEGER -> recompute { it.setIrrational(irrational.pow(rhs.integer)) }
        REALTYPE_NOT_SET, null -> throw Exception("Invalid real: $rhs.")
      }
    }
    INTEGER -> {
      // Left-hand side is an integer.
      when (rhs.realTypeCase) {
        // An integer raised to a fraction can use the same approach as above (fraction raised to
        // fraction) by treating the integer as a whole number fraction.
        RATIONAL -> rhs.rational.toImproperForm().let { power ->
          integer.toWholeNumberFraction()
            .pow(power.numerator)
            .root(power.denominator, power.isNegative)
        }
        IRRATIONAL -> recompute { it.setIrrational(integer.toDouble().pow(rhs.irrational)) }
        INTEGER -> integer.pow(rhs.integer)
        REALTYPE_NOT_SET, null -> throw Exception("Invalid real: $rhs.")
      }
    }
    REALTYPE_NOT_SET, null -> throw Exception("Invalid real: $this.")
  }
}

fun sqrt(real: Real): Real {
  return when (real.realTypeCase) {
    RATIONAL -> sqrt(real.rational)
    IRRATIONAL -> real.recompute { it.setIrrational(kotlin.math.sqrt(real.irrational)) }
    INTEGER -> sqrt(real.integer)
    REALTYPE_NOT_SET, null -> throw Exception("Invalid real: $real.")
  }
}

fun abs(real: Real): Real = if (real.isNegative()) -real else real

private operator fun Double.plus(rhs: Fraction): Double = this + rhs.toDouble()
private operator fun Fraction.plus(rhs: Double): Double = toDouble() + rhs
private operator fun Fraction.plus(rhs: Int): Fraction = this + rhs.toWholeNumberFraction()
private operator fun Int.plus(rhs: Fraction): Fraction = toWholeNumberFraction() + rhs
private operator fun Double.minus(rhs: Fraction): Double = this - rhs.toDouble()
private operator fun Fraction.minus(rhs: Double): Double = toDouble() - rhs
private operator fun Fraction.minus(rhs: Int): Fraction = this - rhs.toWholeNumberFraction()
private operator fun Int.minus(rhs: Fraction): Fraction = toWholeNumberFraction() - rhs
private operator fun Double.times(rhs: Fraction): Double = this * rhs.toDouble()
private operator fun Fraction.times(rhs: Double): Double = toDouble() * rhs
private operator fun Fraction.times(rhs: Int): Fraction = this * rhs.toWholeNumberFraction()
private operator fun Int.times(rhs: Fraction): Fraction = toWholeNumberFraction() * rhs
private operator fun Double.div(rhs: Fraction): Double = this / rhs.toDouble()
private operator fun Fraction.div(rhs: Double): Double = toDouble() / rhs
private operator fun Fraction.div(rhs: Int): Fraction = this / rhs.toWholeNumberFraction()
private operator fun Int.div(rhs: Fraction): Fraction = toWholeNumberFraction() / rhs

private fun Int.add(rhs: Int): Real = Real.newBuilder().apply { integer = this@add + rhs }.build()
private fun Int.subtract(rhs: Int): Real = Real.newBuilder().apply {
  integer = this@subtract - rhs
}.build()
private fun Int.multiply(rhs: Int): Real = Real.newBuilder().apply {
  integer = this@multiply * rhs
}.build()
private fun Int.divide(rhs: Int): Real = Real.newBuilder().apply {
  // If rhs divides this integer, retain the integer.
  val lhs = this@divide
  if ((lhs % rhs) == 0) {
    integer = lhs / rhs
  } else {
    // Otherwise, keep precision by turning the division into a fraction.
    rational = Fraction.newBuilder().apply {
      isNegative = (lhs < 0) xor (rhs < 0)
      numerator = kotlin.math.abs(lhs)
      denominator = kotlin.math.abs(rhs)
    }.build()
  }
}.build()

private fun Double.pow(rhs: Fraction): Double = this.pow(rhs.toDouble())
private fun Fraction.pow(rhs: Double): Double = toDouble().pow(rhs)

private fun Int.pow(exp: Int): Real {
  return when {
    exp == 0 -> Real.newBuilder().apply { integer = 0 }.build()
    exp == 1 -> Real.newBuilder().apply { integer = this@pow }.build()
    exp < 0 -> Real.newBuilder().apply { rational = toWholeNumberFraction().pow(exp) }.build()
    else -> {
      // exp > 1
      var computed = this
      for (i in 0 until exp - 1) computed *= this
      Real.newBuilder().apply { integer = computed }.build()
    }
  }
}

private fun sqrt(fraction: Fraction): Real = fraction.root(base = 2, invert = false)

private fun Fraction.root(base: Int, invert: Boolean): Real {
  check(base > 1) { "Expected base of 2 or higher, not: $base" }

  val adjustedFraction = toImproperForm()
  val adjustedNum =
    if (adjustedFraction.isNegative) -adjustedFraction.numerator else adjustedFraction.numerator
  val adjustedDenom = adjustedFraction.denominator
  val rootedNumerator = if (invert) root(adjustedDenom, base) else root(adjustedNum, base)
  val rootedDenominator = if (invert) root(adjustedNum, base) else root(adjustedDenom, base)
  return if (rootedNumerator.isInteger() && rootedDenominator.isInteger()) {
    Real.newBuilder().apply {
      rational = Fraction.newBuilder().apply {
        isNegative = rootedNumerator.isNegative() || rootedDenominator.isNegative()
        numerator = rootedNumerator.integer.absoluteValue
        denominator = rootedDenominator.integer.absoluteValue
      }.build().toProperForm()
    }.build()
  } else {
    // One or both of the components of the fraction can't be rooted, so compute an irrational
    // version.
    Real.newBuilder().apply {
      irrational = rootedNumerator.toDouble() / rootedDenominator.toDouble()
    }.build()
  }
}

private fun sqrt(int: Int): Real = root(int, base = 2)

private fun root(int: Int, base: Int): Real {
  // First, check if the integer is a root. Base reference for possible methods:
  // https://www.researchgate.net/post/How-to-decide-if-a-given-number-will-have-integer-square-root-or-not.
  check(base > 1) { "Expected base of 2 or higher, not: $base" }
  check((int < 0 && base.isOdd()) || int >= 0) { "Radicand results in imaginary number: $int" }

  if (int == 1) {
    // 1^x is always 1.
    return Real.newBuilder().apply {
      integer = 1
    }.build()
  }

  val radicand = int.absoluteValue
  var potentialRoot = base
  while (potentialRoot.pow(base).integer < radicand) {
    potentialRoot++
  }
  if (potentialRoot.pow(base).integer == radicand) {
    // There's an exact integer representation of the root.
    if (int < 0 && base.isOdd()) {
      // Odd roots of negative numbers retain the negative.
      potentialRoot = -potentialRoot
    }
    return Real.newBuilder().apply {
      integer = potentialRoot
    }.build()
  }

  // Otherwise, compute the irrational square root.
  return Real.newBuilder().apply {
    irrational = if (base == 2) {
      kotlin.math.sqrt(int.toDouble())
    } else int.toDouble().pow(1.0 / base.toDouble())
  }.build()
}

private fun Int.isOdd() = this % 2 == 1

private fun Real.recompute(transform: (Real.Builder) -> Real.Builder): Real {
  return transform(newBuilderForType()).build()
}

// TODO: consider replacing this with inline alternatives since they'll probably be simpler.
private fun combine(
  lhs: Real,
  rhs: Real,
  leftRationalRightRationalOp: (Fraction, Fraction) -> Fraction,
  leftRationalRightIrrationalOp: (Fraction, Double) -> Double,
  leftRationalRightIntegerOp: (Fraction, Int) -> Fraction,
  leftIrrationalRightRationalOp: (Double, Fraction) -> Double,
  leftIrrationalRightIrrationalOp: (Double, Double) -> Double,
  leftIrrationalRightIntegerOp: (Double, Int) -> Double,
  leftIntegerRightRationalOp: (Int, Fraction) -> Fraction,
  leftIntegerRightIrrationalOp: (Int, Double) -> Double,
  leftIntegerRightIntegerOp: (Int, Int) -> Real,
): Real {
  return when (lhs.realTypeCase) {
    RATIONAL -> {
      // Left-hand side is Fraction.
      when (rhs.realTypeCase) {
        RATIONAL ->
          lhs.recompute { it.setRational(leftRationalRightRationalOp(lhs.rational, rhs.rational)) }
        IRRATIONAL ->
          lhs.recompute {
            it.setIrrational(leftRationalRightIrrationalOp(lhs.rational, rhs.irrational))
          }
        INTEGER ->
          lhs.recompute { it.setRational(leftRationalRightIntegerOp(lhs.rational, rhs.integer)) }
        REALTYPE_NOT_SET, null -> throw Exception("Invalid real: $rhs.")
      }
    }
    IRRATIONAL -> {
      // Left-hand side is a double.
      when (rhs.realTypeCase) {
        RATIONAL ->
          lhs.recompute {
            it.setIrrational(leftIrrationalRightRationalOp(lhs.irrational, rhs.rational))
          }
        IRRATIONAL ->
          lhs.recompute {
            it.setIrrational(leftIrrationalRightIrrationalOp(lhs.irrational, rhs.irrational))
          }
        INTEGER ->
          lhs.recompute {
            it.setIrrational(leftIrrationalRightIntegerOp(lhs.irrational, rhs.integer))
          }
        REALTYPE_NOT_SET, null -> throw Exception("Invalid real: $rhs.")
      }
    }
    INTEGER -> {
      // Left-hand side is an integer.
      when (rhs.realTypeCase) {
        RATIONAL ->
          lhs.recompute { it.setRational(leftIntegerRightRationalOp(lhs.integer, rhs.rational)) }
        IRRATIONAL ->
          lhs.recompute {
            it.setIrrational(leftIntegerRightIrrationalOp(lhs.integer, rhs.irrational))
          }
        INTEGER -> leftIntegerRightIntegerOp(lhs.integer, rhs.integer)
        REALTYPE_NOT_SET, null -> throw Exception("Invalid real: $rhs.")
      }
    }
    REALTYPE_NOT_SET, null -> throw Exception("Invalid real: $lhs.")
  }
}
