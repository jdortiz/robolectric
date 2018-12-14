package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.accounts.Account;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
public class ShadowParcelTest {

  private Parcel parcel;

  @Before
  public void setup() {
    parcel = Parcel.obtain();
  }

  @After
  public void tearDown() {
    parcel.recycle();
  }

  @Test
  public void testObtain() {
    assertThat(parcel).isNotNull();
  }

  @Test
  public void testReadIntWhenEmpty() {
    assertThat(parcel.readInt()).isEqualTo(0);
    assertThat(parcel.dataPosition()).isEqualTo(0);
    assertInvariants();
  }

  @Test
  public void testReadIntWhenUninitialized() {
    parcel.setDataSize(100);
    assertThat(parcel.readInt()).isEqualTo(0);
    assertThat(parcel.dataPosition()).isEqualTo(4);
    assertInvariants();
  }

  @Test
  public void testReadLongWhenEmpty() {
    assertThat(parcel.readLong()).isEqualTo(0);
    assertThat(parcel.dataPosition()).isEqualTo(0);
    assertInvariants();
  }

  @Test
  public void testReadLongWhenUninitialized() {
    parcel.setDataSize(100);
    assertThat(parcel.readLong()).isEqualTo(0);
    assertThat(parcel.dataPosition()).isEqualTo(8);
    assertInvariants();
  }

  @Test
  public void testReadLongWhenNotBigEnoughForALong() {
    parcel.setDataSize(4);
    assertThat(parcel.readLong()).isEqualTo(0);
    assertWithMessage("reading a long should increment 4 bytes if the buffer is only 4 bytes long")
        .that(parcel.dataPosition())
        .isEqualTo(4);
    assertInvariants();
  }

  @Test
  public void testReadStringWhenEmpty() {
    assertThat(parcel.readString()).isNull();
    assertInvariants();
  }

  @Test
  public void testReadStrongBinderWhenEmpty() {
    parcel.setDataSize(100);
    assertThat(parcel.readStrongBinder()).isNull();
    assertThat(parcel.dataPosition()).isEqualTo(4);
  }

  @Test
  public void testReadWriteSingleString() {
    String val = "test";
    parcel.writeString(val);
    assertInvariants();
    parcel.setDataPosition(0);
    assertThat(parcel.readString()).isEqualTo(val);
  }

  @Test
  public void testWriteNullString() {
    parcel.writeString(null);
    assertInvariants();
    parcel.setDataPosition(0);
    assertThat(parcel.readString()).isNull();
    assertThat(parcel.dataPosition()).isEqualTo(4);
  }

  @Test
  public void testWriteEmptyString() {
    parcel.writeString("");
    assertInvariants();
    parcel.setDataPosition(0);
    assertThat(parcel.readString()).isEmpty();
  }

  @Test
  public void testReadWriteMultipleStrings() {
    for (int i = 0; i < 10; ++i) {
      parcel.writeString(Integer.toString(i));
      assertInvariants();
    }
    parcel.setDataPosition(0);
    for (int i = 0; i < 10; ++i) {
      assertThat(parcel.readString()).isEqualTo(Integer.toString(i));
    }
    // now try to read past the number of items written and see what happens
    assertThat(parcel.readString()).isNull();
  }

  @Test
  @Config(minSdk = LOLLIPOP)
  public void testReadWriteSingleStrongBinder() {
    IBinder binder = new Binder();
    parcel.writeStrongBinder(binder);
    parcel.setDataPosition(0);
    assertThat(parcel.readStrongBinder()).isEqualTo(binder);
  }

  @Test
  @Config(minSdk = LOLLIPOP)
  public void testWriteNullStrongBinder() {
    parcel.writeStrongBinder(null);
    parcel.setDataPosition(0);
    assertThat(parcel.readStrongBinder()).isNull();
  }

  @Test
  @Config(minSdk = LOLLIPOP)
  public void testReadWriteMultipleStrongBinders() {
    List<IBinder> binders = new ArrayList<>();
    for (int i = 0; i < 10; ++i) {
      IBinder binder = new Binder();
      binders.add(binder);
      parcel.writeStrongBinder(binder);
    }
    parcel.setDataPosition(0);
    for (int i = 0; i < 10; ++i) {
      assertThat(parcel.readStrongBinder()).isEqualTo(binders.get(i));
    }
    // now try to read past the number of items written and see what happens
    assertThat(parcel.readStrongBinder()).isNull();
  }

  @Test
  public void testReadWriteSingleInt() {
    int val = 5;
    parcel.writeInt(val);
    parcel.setDataPosition(0);
    assertThat(parcel.readInt()).isEqualTo(val);
  }

  @Test
  public void testOverwriteSameSize() {
    parcel.writeInt(1);
    parcel.writeInt(2);
    parcel.writeInt(3);
    parcel.writeInt(4);
    assertInvariants();

    parcel.setDataPosition(4);
    parcel.writeByte((byte) 55); // Byte and int have the parceled size.
    parcel.writeString(null); // And so does a null string.
    assertInvariants();

    parcel.setDataPosition(0);
    assertThat(parcel.readInt()).named("readInt @ 0").isEqualTo(1);
    assertThat(parcel.dataPosition()).named("position after 0->readInt").isEqualTo(4);
    assertThat(parcel.readByte()).named("readByte").isEqualTo(55);
    assertThat(parcel.dataPosition()).named("position after 4->readByte").isEqualTo(8);
    assertThat(parcel.readString()).named("readString").isNull();
    assertThat(parcel.dataPosition()).named("position after 8->readString").isEqualTo(12);
    assertThat(parcel.readInt()).isEqualTo(4);
  }

  @Test
  public void testOverwriteInMiddleOfObject() {
    // NOTE: This is not conformant Parcel behavior, but strict behavior intended to catch bugs in
    // tests.
    parcel.writeLong(111L);
    parcel.writeLong(222L);

    parcel.setDataPosition(4);
    try {
      parcel.readInt();
      fail("should have thrown");
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "Cannot partially overwrite objects: "
                  + "4 in middle of \"111\" (java.lang.Long) spanning [0,8)");
    }

    parcel.setDataPosition(13);
    try {
      parcel.writeInt(5);
      fail("should have thrown");
    } catch (IllegalArgumentException e) {
      assertThat(e)
          .hasMessage(
              "Cannot partially overwrite objects: "
                  + "13 in middle of \"222\" (java.lang.Long) spanning [8,16)");
    }
  }

  @Test
  public void testReadWriteIntArray() throws Exception {
    final int[] ints = { 1, 2 };
    parcel.writeIntArray(ints);
    parcel.setDataPosition(0);
    final int[] ints2 = new int[ints.length];
    parcel.readIntArray(ints2);
    assertTrue(Arrays.equals(ints, ints2));
  }

  @Test
  public void testWriteAndCreateNullIntArray() throws Exception {
    parcel.writeIntArray(null);
    parcel.setDataPosition(0);
    assertThat(parcel.createIntArray()).isNull();
  }

  @Test
  public void testReadWriteLongArray() throws Exception {
    final long[] longs = { 1, 2 };
    parcel.writeLongArray(longs);
    parcel.setDataPosition(0);
    final long[] longs2 = new long[longs.length];
    parcel.readLongArray(longs2);
    assertTrue(Arrays.equals(longs, longs2));
  }

  @Test
  public void testWriteAndCreateNullLongArray() throws Exception {
    parcel.writeLongArray(null);
    parcel.setDataPosition(0);
    assertThat(parcel.createLongArray()).isNull();
  }

  @Test
  public void testReadWriteSingleFloat() {
    float val = 5.2f;
    parcel.writeFloat(val);
    parcel.setDataPosition(0);
    assertThat(parcel.readFloat()).isEqualTo(val);
  }

  @Test
  public void testReadWriteFloatArray() throws Exception {
    final float[] floats = { 1.1f, 2.0f };
    parcel.writeFloatArray(floats);
    parcel.setDataPosition(0);
    final float[] floats2 = new float[floats.length];
    parcel.readFloatArray(floats2);
    assertTrue(Arrays.equals(floats, floats2));
  }

  @Test
  public void testWriteAndCreateNullFloatArray() throws Exception {
    parcel.writeFloatArray(null);
    parcel.setDataPosition(0);
    assertThat(parcel.createFloatArray()).isNull();
  }

  @Test
  public void testReadWriteDoubleArray() throws Exception {
    final double[] doubles = { 1.1f, 2.0f };
    parcel.writeDoubleArray(doubles);
    parcel.setDataPosition(0);
    final double[] doubles2 = new double[doubles.length];
    parcel.readDoubleArray(doubles2);
    assertTrue(Arrays.equals(doubles, doubles2));
  }

  @Test
  public void testWriteAndCreateNullDoubleArray() throws Exception {
    parcel.writeDoubleArray(null);
    parcel.setDataPosition(0);
    assertThat(parcel.createDoubleArray()).isNull();
  }

  @Test
  public void testReadWriteStringArray() throws Exception {
    final String[] strings = { "foo", "bar" };
    parcel.writeStringArray(strings);
    parcel.setDataPosition(0);
    final String[] strings2 = new String[strings.length];
    parcel.readStringArray(strings2);
    assertTrue(Arrays.equals(strings, strings2));
  }

  @Test
  public void testWriteAndCreateNullStringArray() throws Exception {
    parcel.writeStringArray(null);
    parcel.setDataPosition(0);
    assertThat(parcel.createStringArray()).isNull();
  }

  @Test
  public void testWriteAndCreateByteArray() {
    byte[] bytes = new byte[] { -1, 2, 3, 127 };
    parcel.writeByteArray(bytes);
    parcel.setDataPosition(0);
    byte[] actualBytes = parcel.createByteArray();
    assertTrue(Arrays.equals(bytes, actualBytes));
  }

  @Test
  public void testWriteAndCreateNullByteArray() throws Exception {
    parcel.writeByteArray(null);
    parcel.setDataPosition(0);
    assertThat(parcel.createByteArray()).isNull();
  }

  @Test
  public void testWriteAndCreateByteArray_lengthZero() {
    byte[] bytes = new byte[] {};
    parcel.writeByteArray(bytes);
    parcel.setDataPosition(0);
    byte[] actualBytes = parcel.createByteArray();
    assertTrue(Arrays.equals(bytes, actualBytes));
  }

  @Test
  public void testWriteAndReadByteArray() {
    byte[] bytes = new byte[] { -1, 2, 3, 127 };
    parcel.writeByteArray(bytes);
    parcel.setDataPosition(0);
    byte[] actualBytes = new byte[bytes.length];
    parcel.readByteArray(actualBytes);
    assertTrue(Arrays.equals(bytes, actualBytes));
  }

  @Test(expected = RuntimeException.class)
  public void testWriteAndReadByteArray_badLength() {
    byte[] bytes = new byte[] { -1, 2, 3, 127 };
    parcel.writeByteArray(bytes);
    parcel.setDataPosition(0);
    byte[] actualBytes = new byte[0];
    parcel.readByteArray(actualBytes);
  }

  @Test
  public void testReadWriteMultipleInts() {
    for (int i = 0; i < 10; ++i) {
      parcel.writeInt(i);
    }
    parcel.setDataPosition(0);
    for (int i = 0; i < 10; ++i) {
      assertThat(parcel.readInt()).isEqualTo(i);
    }
    // now try to read past the number of items written and see what happens
    assertThat(parcel.readInt()).isEqualTo(0);
  }

  @Test
  public void testReadWriteSingleByte() {
    byte val = 1;
    parcel.writeByte(val);
    parcel.setDataPosition(0);
    assertThat(parcel.readByte()).isEqualTo(val);
  }

  @Test
  public void testReadWriteMultipleBytes() {
    for (byte i = Byte.MIN_VALUE; i < Byte.MAX_VALUE; ++i) {
      parcel.writeByte(i);
    }
    parcel.setDataPosition(0);
    for (byte i = Byte.MIN_VALUE; i < Byte.MAX_VALUE; ++i) {
      assertThat(parcel.readByte()).isEqualTo(i);
    }
    // now try to read past the number of items written and see what happens
    assertThat(parcel.readByte()).isEqualTo((byte) 0);
  }

  @Test
  public void testReadWriteStringInt() {
    for (int i = 0; i < 10; ++i) {
      parcel.writeString(Integer.toString(i));
      parcel.writeInt(i);
    }
    parcel.setDataPosition(0);
    for (int i = 0; i < 10; ++i) {
      assertThat(parcel.readString()).isEqualTo(Integer.toString(i));
      assertThat(parcel.readInt()).isEqualTo(i);
    }
    // now try to read past the number of items written and see what happens
    assertThat(parcel.readString()).isNull();
    assertThat(parcel.readInt()).isEqualTo(0);
  }

  @Test(expected = ClassCastException.class)
  public void testWriteStringReadInt() {
    String val = "test";
    parcel.writeString(val);
    parcel.setDataPosition(0);
    parcel.readInt();
  }

  @Test(expected = ClassCastException.class)
  public void testWriteIntReadString() {
    int val = 9;
    parcel.writeInt(val);
    parcel.setDataPosition(0);
    parcel.readString();
  }

  @Test
  public void testReadWriteSingleLong() {
    long val = 5;
    parcel.writeLong(val);
    parcel.setDataPosition(0);
    assertThat(parcel.readLong()).isEqualTo(val);
  }

  @Test
  public void testReadWriteMultipleLongs() {
    for (long i = 0; i < 10; ++i) {
      parcel.writeLong(i);
    }
    parcel.setDataPosition(0);
    for (long i = 0; i < 10; ++i) {
      assertThat(parcel.readLong()).isEqualTo(i);
    }
    // now try to read past the number of items written and see what happens
    assertThat(parcel.readLong()).isEqualTo(0L);
  }

  @Test
  public void testReadWriteStringLong() {
    for (long i = 0; i < 10; ++i) {
      parcel.writeString(Long.toString(i));
      parcel.writeLong(i);
    }
    parcel.setDataPosition(0);
    for (long i = 0; i < 10; ++i) {
      assertThat(parcel.readString()).isEqualTo(Long.toString(i));
      assertThat(parcel.readLong()).isEqualTo(i);
    }
    // now try to read past the number of items written and see what happens
    assertThat(parcel.readString()).isNull();
    assertThat(parcel.readLong()).isEqualTo(0L);
  }

  @Test(expected = ClassCastException.class)
  public void testWriteStringReadLong() {
    String val = "test";
    parcel.writeString(val);
    parcel.setDataPosition(0);
    parcel.readLong();
  }

  @Test(expected = ClassCastException.class)
  public void testWriteLongReadString() {
    long val = 9;
    parcel.writeLong(val);
    parcel.setDataPosition(0);
    parcel.readString();
  }

  @Test
  public void testReadWriteParcelable() {
    Account a1 = new Account("name", "type");
    parcel.writeParcelable(a1, 0);
    parcel.setDataPosition(0);

    Account a2 = parcel.readParcelable(Account.class.getClassLoader());
    assertEquals(a1, a2);
  }

  @Test
  public void testReadWriteBundle() {
    Bundle b1 = new Bundle();
    b1.putString("hello", "world");
    parcel.writeBundle(b1);
    parcel.setDataPosition(0);
    Bundle b2 = parcel.readBundle();

    assertEquals("world", b2.getString("hello"));

    parcel.setDataPosition(0);
    parcel.writeBundle(b1);
    parcel.setDataPosition(0);
    b2 = parcel.readBundle(null /* ClassLoader */);
    assertEquals("world", b2.getString("hello"));
  }

  @Test
  public void testCreateStringArrayList() throws Exception {
    parcel.writeStringList(Arrays.asList("str1", "str2"));
    parcel.setDataPosition(0);

    List<String> actual = parcel.createStringArrayList();
    assertEquals(2, actual.size());
    assertEquals("str1", actual.get(0));
    assertEquals("str2", actual.get(1));
  }

  @Test
  public void testWriteTypedListAndCreateTypedArrayList() throws Exception {
    TestParcelable normal = new TestParcelable(23);
    ArrayList<TestParcelable> normals = new ArrayList<>();
    normals.add(normal);

    parcel.writeTypedList(normals);
    parcel.setDataPosition(0);
    List<org.robolectric.shadows.TestParcelable> rehydrated = parcel
        .createTypedArrayList(TestParcelable.CREATOR);

    assertEquals(1, rehydrated.size());
    assertEquals(23, rehydrated.get(0).contents);
  }

  @Test
  public void testParcelableWithPackageProtected() throws Exception {
    TestParcelablePackage normal = new TestParcelablePackage(23);

    parcel.writeParcelable(normal, 0);
    parcel.setDataPosition(0);

    TestParcelablePackage rehydrated = parcel.readParcelable(TestParcelablePackage.class.getClassLoader());

    assertEquals(normal.contents, rehydrated.contents);
  }

  @Test
  public void testParcelableWithBase() throws Exception {
    TestParcelableImpl normal = new TestParcelableImpl(23);

    parcel.writeParcelable(normal, 0);
    parcel.setDataPosition(0);

    TestParcelableImpl rehydrated = parcel.readParcelable(TestParcelableImpl.class.getClassLoader());

    assertEquals(normal.contents, rehydrated.contents);
  }

  @Test
  public void testParcelableWithPublicClass() throws Exception {
    TestParcelable normal = new TestParcelable(23);

    parcel.writeParcelable(normal, 0);
    parcel.setDataPosition(0);

    TestParcelable rehydrated = parcel.readParcelable(TestParcelable.class.getClassLoader());

    assertEquals(normal.contents, rehydrated.contents);
  }

  @Test
  public void testReadAndWriteStringList() throws Exception {
    ArrayList<String> original = new ArrayList<>();
    List<String> rehydrated = new ArrayList<>();
    original.add("str1");
    original.add("str2");
    parcel.writeStringList(original);
    parcel.setDataPosition(0);
    parcel.readStringList(rehydrated);
    assertEquals(2, rehydrated.size());
    assertEquals("str1", rehydrated.get(0));
    assertEquals("str2", rehydrated.get(1));
  }

  @Test
  public void testReadWriteMap() throws Exception {
    HashMap<String, String> original = new HashMap<>();
    original.put("key", "value");
    parcel.writeMap(original);
    parcel.setDataPosition(0);
    HashMap<String, String> rehydrated = parcel.readHashMap(null);

    assertEquals("value", rehydrated.get("key"));
  }

  @Test
  public void testCreateStringArray() {
    String[] strs = { "a1", "b2" };
    parcel.writeStringArray(strs);
    parcel.setDataPosition(0);
    String[] newStrs = parcel.createStringArray();
    assertTrue(Arrays.equals(strs, newStrs));
  }

  @Test
  public void testDataPositionAfterSomeWrites() {
    parcel.writeInt(1);
    assertThat(parcel.dataPosition()).isEqualTo(4);

    parcel.writeFloat(5);
    assertThat(parcel.dataPosition()).isEqualTo(8);

    parcel.writeDouble(37);
    assertThat(parcel.dataPosition()).isEqualTo(16);

    parcel.writeStrongBinder(new Binder()); // 20 bytes
    assertThat(parcel.dataPosition()).isEqualTo(36);
  }

  @Test
  public void testDataPositionAfterSomeReads() {
    parcel.writeInt(1);
    parcel.writeFloat(5);
    parcel.writeDouble(37);
    parcel.setDataPosition(0);

    parcel.readInt();
    assertThat(parcel.dataPosition()).isEqualTo(4);

    parcel.readFloat();
    assertThat(parcel.dataPosition()).isEqualTo(8);

    parcel.readDouble();
    assertThat(parcel.dataPosition()).isEqualTo(16);
  }

  @Test
  public void testDataSizeAfterSomeWrites() {
    parcel.writeInt(1);
    assertThat(parcel.dataSize()).isEqualTo(4);

    parcel.writeFloat(5);
    assertThat(parcel.dataSize()).isEqualTo(8);

    parcel.writeDouble(37);
    assertThat(parcel.dataSize()).isEqualTo(16);
  }

  @Test
  public void testDataAvail() {
    parcel.writeInt(1);
    parcel.writeFloat(5);
    parcel.writeDouble(6);
    parcel.setDataPosition(4);

    assertThat(parcel.dataAvail()).isEqualTo(12);
  }

  @Test
  public void testSetDataPositionIntoMiddleOfParcel() {
    parcel.writeInt(1);
    parcel.writeFloat(5);
    parcel.writeDouble(6);
    parcel.setDataPosition(4);

    assertThat(parcel.readFloat()).isEqualTo(5.0f);
  }

  @Test
  public void testSetDataPositionToEmptyString() {
    parcel.writeString("");
    parcel.setDataPosition(parcel.dataPosition());
    parcel.writeString("something else");

    parcel.setDataPosition(0);
    assertThat(parcel.readString()).isEmpty();
  }

  @Test
  public void testAppendFrom() {
    parcel.writeInt(1);
    parcel.writeInt(2);
    parcel.writeInt(3);
    parcel.writeInt(4);

    Parcel parcel2 = Parcel.obtain();
    parcel2.appendFrom(parcel, 4, 8);
    parcel2.setDataPosition(0);

    assertThat(parcel2.readInt()).isEqualTo(2);
    assertThat(parcel2.readInt()).isEqualTo(3);
    assertThat(parcel2.dataSize()).isEqualTo(8);
  }

  @Test
  public void testMarshallAndUnmarshall() {
    parcel.writeInt(1);
    parcel.writeString("hello");
    parcel.writeDouble(25);
    parcel.writeFloat(1.25f);
    parcel.writeByte((byte) 0xAF);

    byte[] rawBytes = parcel.marshall();
    Parcel parcel2 = Parcel.obtain();
    parcel2.unmarshall(rawBytes, 0, rawBytes.length);

    assertThat(parcel2.readInt()).isEqualTo(1);
    assertThat(parcel2.readString()).isEqualTo("hello");
    assertThat(parcel2.readDouble()).isEqualTo(25.0);
    assertThat(parcel2.readFloat()).isEqualTo(1.25f);
    assertThat(parcel2.readByte()).isEqualTo((byte) 0xAF);
  }

  @Test
  public void testSetDataSize() {
    parcel.writeInt(1);
    parcel.writeInt(2);
    parcel.writeInt(3);
    parcel.writeInt(4);
    parcel.writeInt(5);
    assertThat(parcel.dataSize()).isEqualTo(20);
    assertInvariants();
    int oldCapacity = parcel.dataCapacity();

    parcel.setDataSize(12);
    assertWithMessage("should equal requested size").that(parcel.dataSize()).isEqualTo(12);
    assertWithMessage("position gets truncated").that(parcel.dataPosition()).isEqualTo(12);
    assertWithMessage("capacity doesn't shrink").that(parcel.dataCapacity()).isEqualTo(oldCapacity);

    parcel.setDataSize(100);
    assertWithMessage("should equal requested size").that(parcel.dataSize()).isEqualTo(100);
    assertWithMessage("position untouched").that(parcel.dataPosition()).isEqualTo(12);
    assertInvariants();
  }

  @Test
  public void testDataCapacityGrowing() {
    parcel.writeInt(-1);
    assertWithMessage("size is 1 int").that(parcel.dataSize()).isEqualTo(4);
    assertInvariants();
    parcel.readInt();
    assertWithMessage("reading within capacity but over size does not increase size")
        .that(parcel.dataSize())
        .isEqualTo(4);

    parcel.setDataCapacity(100);
    assertInvariants();
    assertWithMessage("capacity equals requested").that(parcel.dataCapacity()).isEqualTo(100);
    assertWithMessage("size does not increase with capacity").that(parcel.dataSize()).isEqualTo(4);

    for (int i = 0; i < 100; i++) {
      parcel.writeInt(i);
    }
    assertInvariants();
    assertWithMessage("101 ints in size").that(parcel.dataSize()).isEqualTo(404);
    assertWithMessage("advanced 101 ints").that(parcel.dataPosition()).isEqualTo(404);
  }

  @Test
  public void testDataCapacityShrinking() {
    parcel.setDataCapacity(400);
    assertWithMessage("still empty").that(parcel.dataSize()).isEqualTo(0);
    assertWithMessage("did not advance").that(parcel.dataPosition()).isEqualTo(0);
    for (int i = 0; i < 100; i++) {
      parcel.writeInt(1000 + i);
    }
    assertInvariants();
    assertWithMessage("now has 100 ints").that(parcel.dataSize()).isEqualTo(400);
    assertWithMessage("advanced 100 ints").that(parcel.dataPosition()).isEqualTo(400);
    assertWithMessage("capacity equals requested size").that(parcel.dataCapacity()).isEqualTo(400);

    parcel.setDataPosition(88);
    assertInvariants();
    parcel.setDataSize(100);
    assertInvariants();
    parcel.setDataCapacity(120);
    assertInvariants();
    assertWithMessage("requested size honored").that(parcel.dataSize()).isEqualTo(100);
    assertWithMessage("requested position honored").that(parcel.dataPosition()).isEqualTo(88);
    assertWithMessage("requested capacity honored").that(parcel.dataCapacity()).isEqualTo(120);
    assertWithMessage("data preserved (index 22, byte 88)").that(parcel.readInt()).isEqualTo(1022);

    parcel.setDataCapacity(8);
    assertInvariants();
    assertWithMessage("requested capacity honored").that(parcel.dataCapacity()).isEqualTo(8);
    assertWithMessage("truncated size").that(parcel.dataSize()).isEqualTo(8);
    assertWithMessage("truncated position").that(parcel.dataPosition()).isEqualTo(8);

    parcel.setDataCapacity(400);
    assertInvariants();
    parcel.setDataSize(400);
    assertInvariants();
    parcel.setDataPosition(88);
    assertInvariants();
    assertWithMessage("data should not re-appear").that(parcel.readInt()).isEqualTo(0);
    parcel.setDataPosition(4);
    assertWithMessage("early data should be preserved").that(parcel.readInt()).isEqualTo(1001);
  }
  
  @Test
  public void testWriteAndEnforceCompatibleInterface() {
    parcel.writeInterfaceToken("com.example.IMyInterface");
    parcel.setDataPosition(0);
    parcel.enforceInterface("com.example.IMyInterface");
    // Nothing explodes
  }
  
  @Test
  public void testWriteAndEnforceIncompatibleInterface() {
    parcel.writeInterfaceToken("com.example.Derp");
    parcel.setDataPosition(0);
    try {
      parcel.enforceInterface("com.example.IMyInterface");
      fail("Expected SecurityException");
    } catch (SecurityException e) {
      // Expected
    }
  }

  private void assertInvariants() {
    assertWithMessage("capacity >= size").that(parcel.dataCapacity()).isAtLeast(parcel.dataSize());
    assertWithMessage("position <= size").that(parcel.dataPosition()).isAtMost(parcel.dataSize());
    assertWithMessage("size % 4 == 0").that(parcel.dataSize() % 4).isEqualTo(0);
    assertWithMessage("capacity % 4 == 0").that(parcel.dataSize() % 4).isEqualTo(0);
  }
}
