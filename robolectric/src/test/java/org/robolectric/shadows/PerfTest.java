package org.robolectric.shadows;

import android.content.res.ApkAssets;
import android.content.res.AssetManager;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ForType;

@RunWith(RobolectricTestRunner.class)
public class PerfTest {
  @ForType(AssetManager.class)
  interface _AssetManager_ {
    ApkAssets[] getApkAssets();
  }

  @Test
  public void perf() throws Exception {
    AssetManager system = AssetManager.getSystem();

    System.out.println("reflection = " + Arrays.asList(byReflection(system)));
    time("reflection", 10_000_000, () -> byReflection(system));

    System.out.println("proxy = " + Arrays.asList(byProxy(system)));
    time("proxy", 10_000_000, () -> byProxy(system));

    time("reflection", 10_000_000, () -> byReflection(system));
    time("proxy", 10_000_000, () -> byProxy(system));
  }

  private void time(String name, int times, Runnable runnable) {
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < times; i++) {
      runnable.run();
    }
    long elasedMs = System.currentTimeMillis() - startTime;
    System.out.println(name + " took " + elasedMs);
  }

  private ApkAssets[] byReflection(AssetManager system) {
    return ReflectionHelpers.callInstanceMethod(system, "getApkAssets");
  }

  private ApkAssets[] byProxy(AssetManager system) {
    _AssetManager_ assetManager_ = ReflectionHelpers.proxyFor(_AssetManager_.class, system);
    return assetManager_.getApkAssets();
  }
}
