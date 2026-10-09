package com.lody.virtual.client.hook.proxies.role;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ResultStaticMethodProxy;

/**
 * Virtualizes read-only Android RoleManager checks for guest apps.
 *
 * System roles (browser, dialer, SMS, etc.) belong to real host packages/UIDs.
 * A virtual guest must not claim those host roles. Returning false for
 * isRoleHeldAsUser() also avoids sending a guest package name to the real
 * RoleManager service, which rejects it because the guest package does not
 * belong to the host runtime UID.
 */
public class RoleManagerStub extends BinderInvocationProxy {

    private static final String SERVICE_NAME = "role";

    public RoleManagerStub() {
        super(getStubClass(), SERVICE_NAME);
    }

    private static Class<?> getStubClass() {
        try {
            return Class.forName("android.app.role.IRoleManager$Stub");
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new ResultStaticMethodProxy("isRoleHeldAsUser", false));
    }
}
