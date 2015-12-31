package com.deathrayresearch.outlier;

import com.deathrayresearch.outlier.io.TypeUtils;
import com.google.common.base.Strings;
import net.mintern.primitive.Primitive;
import org.roaringbitmap.RoaringBitmap;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;

/**
 * A column in a base table that contains float values
 */
public class LocalTimeColumn extends AbstractColumn {

  private static int DEFAULT_ARRAY_SIZE = 128;

  // For internal iteration. What element are we looking at right now
  private int pointer = 0;

  // The number of elements, which may be less than the size of the array
  private int N = 0;

  private int[] data;

  public static LocalTimeColumn create(String name) {
    return new LocalTimeColumn(name);
  }

  private LocalTimeColumn(String name) {
    super(name);
    data = new int[DEFAULT_ARRAY_SIZE];
  }

  public LocalTimeColumn(String name, int initialSize) {
    super(name);
    data = new int[initialSize];
  }

  public int size() {
    return N;
  }

  @Override
  public ColumnType type() {
    return ColumnType.LOCAL_TIME;
  }

  @Override
  public boolean hasNext() {
    return pointer < N;
  }

  public float next() {
    return data[pointer++];
  }

  public void add(int f) {
    if (N >= data.length) {
      resize();
    }
    data[N++] = f;
  }

  // TODO(lwhite): Redo to reduce the increase for large columns
  private void resize() {
    int[] temp = new int[Math.round(data.length * 2)];
    System.arraycopy(data, 0, temp, 0, N);
    data = temp;
  }

  /**
   * Removes (most) extra space (empty elements) from the data array
   */
  public void compact() {
    int[] temp = new int[N + 100];
    System.arraycopy(data, 0, temp, 0, N);
    data = temp;
  }

  @Override
  public String getString(int row) {
    return PackedLocalTime.toShortTimeString(data[row]);
  }

  @Override
  public LocalTimeColumn emptyCopy() {
    return new LocalTimeColumn(name());
  }

  @Override
  public void clear() {
    data = new int[DEFAULT_ARRAY_SIZE];
  }

  public void reset() {
    pointer = 0;
  }

  private LocalTimeColumn copy() {
    LocalTimeColumn copy = emptyCopy();
    copy.data = this.data;
    copy.N = this.N;
    return copy;
  }

  @Override
  public Column sortAscending() {
    LocalTimeColumn copy = this.copy();
    Arrays.sort(copy.data);
    return copy;
  }

  @Override
  public Column sortDescending() {
    LocalTimeColumn copy = this.copy();
    Primitive.sort(copy.data, (d1, d2) -> Integer.compare(d2, d1), false);
    return copy;
  }

  // TODO(lwhite): Implement column summary()
  @Override
  public Table summary() {
    return null;
  }

  // TODO(lwhite): Implement countUnique()
  @Override
  public int countUnique() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    return N == 0;
  }

  public int convert(String value) {
    if (Strings.isNullOrEmpty(value)
        || TypeUtils.MISSING_INDICATORS.contains(value)
        || value.equals("-1")) {
      return (int) ColumnType.LOCAL_TIME.getMissingValue();
    }
    value = Strings.padStart(value, 4, '0');
    return PackedLocalTime.pack(LocalTime.parse(value, TypeUtils.timeFormatter));
  }

  @Override
  public void addCell(String object) {
    try {
      add(convert(object));
    } catch (NullPointerException e) {
      throw new RuntimeException(name() + ": "
          + String.valueOf(object) + ": "
          + e.getMessage());
    }
  }

  public int get(int index) {
    return data[index];
  }

  @Override
  public Comparator<Integer> rowComparator() {
    return comparator;
  }

  Comparator<Integer> comparator = new Comparator<Integer>() {

    @Override
    public int compare(Integer r1, Integer r2) {
      int f1 = data[r1];
      int f2 = data[r2];
      return Integer.compare(f1, f2);
    }
  };

  public RoaringBitmap isEqualTo(LocalTime value) {
    RoaringBitmap results = new RoaringBitmap();
    int packedLocalTime = PackedLocalTime.pack(value);
    int i = 0;
    while (hasNext()) {
      if (packedLocalTime == next()) {
        results.add(i);
      }
      i++;
    }
    reset();
    return results;
  }
}