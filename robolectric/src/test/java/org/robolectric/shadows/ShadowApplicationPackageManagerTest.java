package org.robolectric.shadows;

import static com.google.common.truth.Truth.assertThat;

import android.app.ApplicationPackageManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadow.api.Shadow;

@RunWith(AndroidJUnit4.class)
public class ShadowApplicationPackageManagerTest {

  private static final String TEST_PACKAGE_NAME = "com.some.other.package";
  private static final String TEST_PACKAGE_LABEL = "My Little App";
  private static final int USER_ID = 0;

  ApplicationPackageManager applicationPackageManager;
  ShadowApplicationPackageManager shadowApplicationPackageManager;

  @Before
  public void setUp() {
    applicationPackageManager =
        (ApplicationPackageManager) ApplicationProvider.getApplicationContext().getPackageManager();
    shadowApplicationPackageManager = Shadow.extract(applicationPackageManager);
  }

  @Test
  public void resolveActivityForUser_Match() throws Exception {
    Intent i = new Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER);
    ResolveInfo info = new ResolveInfo();
    info.nonLocalizedLabel = TEST_PACKAGE_LABEL;
    info.activityInfo = new ActivityInfo();
    info.activityInfo.name = "name";
    info.activityInfo.packageName = TEST_PACKAGE_NAME;
    shadowApplicationPackageManager.addResolveInfoForIntent(i, info);

    assertThat(applicationPackageManager.resolveActivityAsUser(i, 0, USER_ID)).isNotNull();
    assertThat(applicationPackageManager.resolveActivityAsUser(i, 0, USER_ID).activityInfo.name).isEqualTo("name");
    assertThat(applicationPackageManager.resolveActivityAsUser(i, 0, USER_ID).activityInfo.packageName)
        .isEqualTo(TEST_PACKAGE_NAME);
  }

  @Test
  public void resolveActivityForUser_NoMatch() throws Exception {
    Intent i = new Intent();
    i.setComponent(new ComponentName("foo.bar", "No Activity"));
    assertThat(applicationPackageManager.resolveActivityAsUser(i, 0, USER_ID)).isNull();
  }

}
