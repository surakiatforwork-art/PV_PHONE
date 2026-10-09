package mirror.android.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefMethod;

public class InstrumentationActivity {
    public static Class<?> TYPE = RefClass.load(InstrumentationActivity.class, "android.app.Instrumentation");

    @MethodParams({Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class})
    public static RefMethod<android.app.Instrumentation.ActivityResult> execStartActivity;
}
