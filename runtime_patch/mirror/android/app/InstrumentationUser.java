package mirror.android.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefMethod;

public class InstrumentationUser {
    public static Class<?> TYPE = RefClass.load(InstrumentationUser.class, "android.app.Instrumentation");

    @MethodParams({Context.class, IBinder.class, IBinder.class, String.class, Intent.class, int.class, Bundle.class, UserHandle.class})
    public static RefMethod<android.app.Instrumentation.ActivityResult> execStartActivity;
}
