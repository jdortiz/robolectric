package org.robolectric.shadows.maps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.location.Address;
import android.location.Geocoder;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.util.TestRunnerWithManifest;

@RunWith(TestRunnerWithManifest.class)
public class ShadowGeocoderTest {
  private Geocoder geocoder;

  @Before
  public void setUp() throws Exception {
    geocoder = new Geocoder(RuntimeEnvironment.application);
  }

  @Test
  public void shouldRecordLastLocationName() throws Exception {
    geocoder.getFromLocationName("731 Market St, San Francisco, CA 94103", 1);
    String lastLocationName = shadowOf(geocoder).getLastLocationName();

    assertEquals("731 Market St, San Francisco, CA 94103", lastLocationName);
  }

  @Test
  public void setsUpHasLocationInAddressFromLocationName() throws Exception {
    shadowOf(geocoder).setSimulatedHasLatLong(true, true);
    Address address = geocoder.getFromLocationName("731 Market St, San Francisco, CA 94103", 1).get(0);
    assertTrue(address.hasLatitude());
    assertTrue(address.hasLongitude());
    shadowOf(geocoder).setSimulatedHasLatLong(false, false);
    address = geocoder.getFromLocationName("731 Market St, San Francisco, CA 94103", 1).get(0);
    assertFalse(address.hasLatitude());
    assertFalse(address.hasLongitude());
  }

  @Test
  public void canReturnNoAddressesOnRequest() throws Exception {
    shadowOf(geocoder).setReturnNoResults(true);
    List<Address> result = geocoder.getFromLocationName("731 Market St, San Francisco, CA 94103", 1);
    assertEquals(0, result.size());
  }

  @Test
  public void answersWhetherResolutionHappened() throws Exception {
    assertFalse(shadowOf(geocoder).didResolution());
    shadowOf(geocoder).setReturnNoResults(true);
    geocoder.getFromLocationName("731 Market St, San Francisco, CA 94103", 1);
    assertTrue(shadowOf(geocoder).didResolution());
  }

  private ShadowGeocoder shadowOf(Geocoder geocoder) {
    return (ShadowGeocoder) ShadowExtractor.extract(geocoder);
  }
}
