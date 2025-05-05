package net.minecraft.scalar.rosettaenchanttable;

import java.util.Random;

/** 実装の異なる Java 実行環境間で同一の乱数値を出すための合同線形方程式による乱数 */
public class PseudoRandom extends Random {
	private static final long serialVersionUID = 1211923446219709298L;
	private static final long multiplier = 0x5DEECE66DL;
	private static final long addend = 0xBL;
	private static final long mask = (1L << 48) - 1;

	private long _seed = 0;

    @Override
	public synchronized void setSeed(long seed) {
		this._seed = seed;
		haveNextNextGaussian = false;
	}

	@Override
	protected int next(int bits) {
		this._seed = (this._seed * multiplier + addend) & mask;
		return (int) (this._seed >>> (48 - bits));
	}

	@Override
	public void nextBytes(byte[] bytes) {
		for (int i = 0, len = bytes.length; i < len;) {
			for (int rnd = nextInt(), n = Math.min(len - i, Integer.SIZE
					/ Byte.SIZE); n-- > 0; rnd >>= Byte.SIZE) {
				bytes[i++] = (byte) rnd;
			}
		}
	}

	@Override
	public int nextInt() {
		return next(32);
	}

	@Override
	public int nextInt(int n) {
		if (n <= 0) {
			throw new IllegalArgumentException("n must be positive");
		}

		if ((n & -n) == n) { // i.e., n is a power of 2
			return (int) ((n * (long) next(31)) >> 31);
		}

		int bits, val;
		do {
			bits = next(31);
			val = bits % n;
		} while (bits - val + (n - 1) < 0);
		return val;
	}

	@Override
	public long nextLong() {
		return ((long) (next(32)) << 32) + next(32);
	}

	@Override
	public boolean nextBoolean() {
		return next(1) != 0;
	}

	@Override
	public float nextFloat() {
		return next(24) / ((float) (1 << 24));
	}

	@Override
	public double nextDouble() {
		return (((long) (next(26)) << 27) + next(27)) / (double) (1L << 53);
	}

	private double nextNextGaussian;
	private boolean haveNextNextGaussian = false;
	@Override
	public synchronized double nextGaussian() {
		if (haveNextNextGaussian) {
			haveNextNextGaussian = false;
			return nextNextGaussian;
		}
		double v1, v2, s;
		do {
			v1 = 2 * nextDouble() - 1; // between -1 and 1
			v2 = 2 * nextDouble() - 1; // between -1 and 1
			s = v1 * v1 + v2 * v2;
		} while (s >= 1 || s == 0);
		double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
		nextNextGaussian = v2 * multiplier;
		haveNextNextGaussian = true;
		return v1 * multiplier;
	}

	public static void main(String[] args) {
		Random r = new PseudoRandom();
		for (int i = 0; i < 100; ++i) {
			System.out.println(r.nextInt(16));
		}
	}
}
