/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.afwsamples.testdpc;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.afwsamples.testdpc.common.Util;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class DevicePolicyManagerGatewayImpl implements DevicePolicyManagerGateway {

    private static final String TAG = DevicePolicyManagerGatewayImpl.class.getSimpleName();

    private final DevicePolicyManager mDevicePolicyManager;
    private final UserManager mUserManager;
    private final ComponentName mAdminComponentName;
    private final LocationManager mLocationManager;

    public DevicePolicyManagerGatewayImpl(@NonNull Context context) {
        this(context.getSystemService(DevicePolicyManager.class),
                context.getSystemService(UserManager.class),
                context.getSystemService(LocationManager.class),
                DeviceAdminReceiver.getComponentName(context));
    }

    public DevicePolicyManagerGatewayImpl(@NonNull DevicePolicyManager dpm, @NonNull UserManager um,
                                          @NonNull LocationManager lm, @NonNull ComponentName admin) {
        mDevicePolicyManager = dpm;
        mUserManager = um;
        mLocationManager = lm;
        mAdminComponentName = admin;

        Log.d(TAG, "constructor: admin=" + mAdminComponentName + ", dpm=" + dpm);
    }

    public static DevicePolicyManagerGatewayImpl forParentProfile(@NonNull Context context) {
        ComponentName admin = DeviceAdminReceiver.getComponentName(context);
        DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class)
                .getParentProfileInstance(admin);
        UserManager um = context.getSystemService(UserManager.class);
        LocationManager lm = context.getSystemService(LocationManager.class);
        return new DevicePolicyManagerGatewayImpl(dpm, um, lm, admin);
    }

    private static void onSuccessLog(String template, Object... args) {
        Util.onSuccessLog(TAG, template, args);
    }

    private static void onErrorLog(Exception e, String template, Object... args) {
        Util.onErrorLog(TAG, e, template, args);
    }

    @Override
    public ComponentName getAdmin() {
        return mAdminComponentName;
    }

    @Override
    public DevicePolicyManager getDevicePolicyManager() {
        return mDevicePolicyManager;
    }

    @Override
    public boolean isProfileOwnerApp() {
        return mDevicePolicyManager.isProfileOwnerApp(mAdminComponentName.getPackageName());
    }

    @Override
    public boolean isDeviceOwnerApp() {
        return mDevicePolicyManager.isDeviceOwnerApp(mAdminComponentName.getPackageName());
    }

    @Override
    public boolean isOrganizationOwnedDeviceWithManagedProfile() {
        return mDevicePolicyManager.isOrganizationOwnedDeviceWithManagedProfile();
    }

    @Override
    public void createAndManageUser(String name, int flags, Consumer<UserHandle> onSuccess,
                                    Consumer<Exception> onError) {
        Log.d(TAG, "createAndManageUser(" + name + ", " + flags + ")");

        try {
            UserHandle userHandle = mDevicePolicyManager.createAndManageUser(mAdminComponentName,
                    name, mAdminComponentName, /* adminExtras= */ null, flags);
            if (userHandle != null) {
                onSuccess.accept(userHandle);
            } else {
                onError.accept(
                        new InvalidResultException("null",
                                "createAndManageUser(%s, %d)", name, flags));
            }
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public void setUserIcon(Bitmap icon, Consumer<Void> onSuccess, Consumer<Exception> onError) {
        Log.d(TAG, "setUserIcon(" + icon + ")");
        try {
            mDevicePolicyManager.setUserIcon(mAdminComponentName, icon);
            onSuccess.accept(null);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public UserHandle getUserHandle(long serialNumber) {
        return mUserManager.getUserForSerialNumber(serialNumber);
    }

    @Override
    public long getSerialNumber(UserHandle user) {
        return mUserManager.getSerialNumberForUser(user);
    }

    @Override
    public void removeUser(UserHandle userHandle, Consumer<Void> onSuccess,
                           Consumer<Exception> onError) {
        Log.d(TAG, "removeUser(" + userHandle + ")");

        try {
            boolean success = mDevicePolicyManager.removeUser(mAdminComponentName, userHandle);
            if (success) {
                onSuccess.accept(null);
            } else {
                onError.accept(new InvalidResultException("false", "removeUser(%s)", userHandle));
            }
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public void switchUser(UserHandle userHandle, Consumer<Void> onSuccess,
                           Consumer<Exception> onError) {
        Log.d(TAG, "switchUser(" + userHandle + ")");

        try {
            boolean success = mDevicePolicyManager.switchUser(mAdminComponentName, userHandle);
            if (success) {
                onSuccess.accept(null);
            } else {
                onError.accept(new FailedOperationException("switchUser(%s)", userHandle));
            }
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public void startUserInBackground(UserHandle userHandle, Consumer<Integer> onSuccess,
                                      Consumer<Exception> onError) {
        Log.d(TAG, "startUserInBackground(" + userHandle + ")");
        try {
            int status = mDevicePolicyManager.startUserInBackground(mAdminComponentName,
                    userHandle);
            if (status == UserManager.USER_OPERATION_SUCCESS) {
                onSuccess.accept(status);
            } else {
                onError.accept(new FailedUserOperationException(status, "startUserInBackground(%s)",
                        userHandle));
            }
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public void stopUser(UserHandle userHandle, Consumer<Integer> onSuccess,
                         Consumer<Exception> onError) {
        Log.d(TAG, "stopUser(" + userHandle + ")");
        try {
            int status = mDevicePolicyManager.stopUser(mAdminComponentName, userHandle);
            if (status == UserManager.USER_OPERATION_SUCCESS) {
                onSuccess.accept(status);
            } else {
                onError.accept(new FailedUserOperationException(status, "stopUser(%s)",
                        userHandle));
            }
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public boolean isAffiliatedUser() {
        boolean isIt = mDevicePolicyManager.isAffiliatedUser();
        Log.d(TAG, "isAffiliatedUser(): " + isIt);
        return isIt;
    }

    @Override
    public Set<String> getAffiliationIds() {
        Set<String> ids = mDevicePolicyManager.getAffiliationIds(mAdminComponentName);
        Log.d(TAG, "getAffiliationIds(): " + ids);
        return ids;
    }

    @Override
    public void setAffiliationIds(Set<String> ids) {
        Log.d(TAG, "setAffiliationIds(" + ids + ")");
        mDevicePolicyManager.setAffiliationIds(mAdminComponentName, ids);
    }

    @Override
    public Set<String> getUserRestrictions() {
        Log.d(TAG, "getUserRestrictions()");
        Bundle restrictions = mDevicePolicyManager.getUserRestrictions(mAdminComponentName);
        return restrictions.keySet().stream().filter(k -> restrictions.getBoolean(k))
                .collect(Collectors.toSet());
    }

    @Override
    public void setUserRestriction(String userRestriction, boolean enabled,
                                   Consumer<Void> onSuccess, Consumer<Exception> onError) {
        Log.d(TAG, "setUserRestriction(" + userRestriction + ", " + enabled + ")");

        try {
            if (enabled) {
                mDevicePolicyManager.addUserRestriction(mAdminComponentName, userRestriction);
            } else {
                mDevicePolicyManager.clearUserRestriction(mAdminComponentName, userRestriction);
            }
            onSuccess.accept(null);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public void setUserRestriction(String userRestriction, boolean enabled) {
        String message = String.format("setUserRestriction(%s, %b)", userRestriction, enabled);
        setUserRestriction(userRestriction, enabled,
                (v) -> onSuccessLog(message),
                (e) -> onErrorLog(e, message));
    }

    @Override
    public boolean hasUserRestriction(String userRestriction) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "hasUserRestriction(" + userRestriction + ")");
        }
        return mUserManager.hasUserRestriction(userRestriction);
    }

    @Override
    public void lockNow(Consumer<Void> onSuccess, Consumer<Exception> onError) {
        Log.d(TAG, "lockNow()");

        try {
            mDevicePolicyManager.lockNow();
            onSuccess.accept(null);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public void lockNow(int flags, Consumer<Void> onSuccess, Consumer<Exception> onError) {
        Log.d(TAG, "lockNow(" + flags + ")");

        try {
            mDevicePolicyManager.lockNow(flags);
            onSuccess.accept(null);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public void reboot(Consumer<Void> onSuccess, Consumer<Exception> onError) {
        Log.d(TAG, "reboot()");

        try {
            mDevicePolicyManager.reboot(mAdminComponentName);
            onSuccess.accept(null);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public void wipeData(int flags, Consumer<Void> onSuccess, Consumer<Exception> onError) {
        Log.d(TAG, "wipeData(" + flags + ")");

        try {
            mDevicePolicyManager.wipeData(flags);
            onSuccess.accept(null);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public void requestBugreport(Consumer<Void> onSuccess, Consumer<Exception> onError) {
        Log.d(TAG, "requestBugreport(");

        try {
            boolean success = mDevicePolicyManager.requestBugreport(mAdminComponentName);
            if (success) {
                onSuccess.accept(null);
            } else {
                onError.accept(new FailedOperationException("requestBugreport()"));
            }
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public void setNetworkLogging(boolean enabled, Consumer<Void> onSuccess,
                                  Consumer<Exception> onError) {
        Log.d(TAG, "setNetworkLogging(" + enabled + ")");

        try {
            mDevicePolicyManager.setNetworkLoggingEnabled(mAdminComponentName, enabled);
            onSuccess.accept(null);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public void setNetworkLogging(boolean enabled) {
        String message = String.format("setNetworkLogging(%b)", enabled);
        setNetworkLogging(enabled,
                (v) -> onSuccessLog(message),
                (e) -> onErrorLog(e, message));
    }

    @Override
    public void setOrganizationName(CharSequence title, Consumer<Void> onSuccess,
                                    Consumer<Exception> onError) {
        Log.d(TAG, "setOrganizationName(" + title + ")");

        try {
            mDevicePolicyManager.setOrganizationName(mAdminComponentName, title);
            onSuccess.accept(null);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public CharSequence getOrganizationName() {
        return mDevicePolicyManager.getOrganizationName(mAdminComponentName);
    }

    @Override
    public void setUserControlDisabledPackages(List<String> packages, Consumer<Void> onSuccess,
                                               Consumer<Exception> onError) {
        Log.d(TAG, "setUserControlDisabledPackages(" + packages + ")");

        try {
            mDevicePolicyManager.setUserControlDisabledPackages(mAdminComponentName, packages);
            onSuccess.accept(null);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public List<String> getUserControlDisabledPackages() {
        return mDevicePolicyManager.getUserControlDisabledPackages(mAdminComponentName);
    }

    @Override
    public void removeActiveAdmin(Consumer<Void> onSuccess, Consumer<Exception> onError) {
        Log.d(TAG, "removeActiveAdmin()");

        try {
            mDevicePolicyManager.removeActiveAdmin(mAdminComponentName);
            onSuccess.accept(null);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public void clearDeviceOwnerApp(Consumer<Void> onSuccess, Consumer<Exception> onError) {
        Log.d(TAG, "clearDeviceOwnerApp()");

        try {
            mDevicePolicyManager.clearDeviceOwnerApp(mAdminComponentName.getPackageName());
            onSuccess.accept(null);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public void clearProfileOwner(Consumer<Void> onSuccess, Consumer<Exception> onError) {
        Log.d(TAG, "clearProfileOwner()");

        try {
            mDevicePolicyManager.clearProfileOwner(mAdminComponentName);
            onSuccess.accept(null);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public void setPasswordQuality(int quality, Consumer<Void> onSuccess,
                                   Consumer<Exception> onError) {
        Log.d(TAG, "setPasswordQuality(" + quality + ")");

        try {
            mDevicePolicyManager.setPasswordQuality(mAdminComponentName, quality);
            onSuccess.accept(null);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public int getPasswordQuality() {
        int quality = mDevicePolicyManager.getPasswordQuality(mAdminComponentName);
        Log.d(TAG, "getPasswordQuality(): " + quality);
        return quality;
    }

    @Override
    public void transferOwnership(ComponentName target, PersistableBundle bundle,
                                  Consumer<Void> onSuccess, Consumer<Exception> onError) {
        Log.d(TAG, "transferOwnership(" + target + ", " + bundle + ")");

        try {
            mDevicePolicyManager.transferOwnership(mAdminComponentName, target, bundle);
            onSuccess.accept(null);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public void setPackagesSuspended(String[] packageNames, boolean suspended,
                                     Consumer<String[]> onSuccess, Consumer<Exception> onError) {
        Log.d(TAG, "setPackagesSuspended(" + Arrays.toString(packageNames) + ": " + suspended + ")");

        try {
            String[] result = mDevicePolicyManager.setPackagesSuspended(mAdminComponentName,
                    packageNames, suspended);
            onSuccess.accept(result);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public boolean isPackageSuspended(String packageName) throws NameNotFoundException {
        return mDevicePolicyManager.isPackageSuspended(mAdminComponentName, packageName);
    }

    @Override
    public void setPersonalAppsSuspended(boolean suspended, Consumer<Void> onSuccess,
                                         Consumer<Exception> onError) {
        Log.d(TAG, "setPersonalAppsSuspended(" + suspended + ")");

        try {
            mDevicePolicyManager.setPersonalAppsSuspended(mAdminComponentName, suspended);
            onSuccess.accept(null);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public int getPersonalAppsSuspendedReasons() {
        return mDevicePolicyManager.getPersonalAppsSuspendedReasons(mAdminComponentName);
    }

    @Override
    public void setApplicationHidden(String packageName, boolean hidden,
                                     Consumer<Void> onSuccess, Consumer<Exception> onError) {
        Log.d(TAG, "setApplicationHidden(" + packageName + ", " + hidden + ")");

        try {
            mDevicePolicyManager.setApplicationHidden(mAdminComponentName, packageName, hidden);
            onSuccess.accept(null);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public boolean isApplicationHidden(String packageName) throws NameNotFoundException {
        return mDevicePolicyManager.isApplicationHidden(mAdminComponentName, packageName);
    }

    @Override
    public void setLockTaskPackages(String[] packages, Consumer<Void> onSuccess,
                                    Consumer<Exception> onError) {
        Log.d(TAG, "setLockTaskPackages(" + Arrays.toString(packages) + ")");
        try {
            mDevicePolicyManager.setLockTaskPackages(mAdminComponentName, packages);
            onSuccess.accept(null);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public String[] getLockTaskPackages() {
        return mDevicePolicyManager.getLockTaskPackages(mAdminComponentName);
    }

    @Override
    public void setLockTaskFeatures(int flags, Consumer<Void> onSuccess,
                                    Consumer<Exception> onError) {
        String features = Util.lockTaskFeaturesToString(flags);
        Log.d(TAG, "setLockTaskFeatures(" + features + ")");
        try {
            mDevicePolicyManager.setLockTaskFeatures(mAdminComponentName, flags);
            onSuccess.accept(null);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public int getLockTaskFeatures() {
        int flags = mDevicePolicyManager.getLockTaskFeatures(mAdminComponentName);
        Log.d(TAG, "getLockTaskFeatures(): " + Util.lockTaskFeaturesToString(flags)
                + " (" + flags + ")");
        return flags;
    }

    @Override
    public boolean isLockTaskPermitted(String packageName) {
        return mDevicePolicyManager.isLockTaskPermitted(packageName);
    }

    @Override
    public Bundle getApplicationRestrictions(String packageName) {
        return mDevicePolicyManager.getApplicationRestrictions(mAdminComponentName, packageName);
    }

    @Override
    public Bundle getSelfRestrictions() {
        return mUserManager.getApplicationRestrictions(mAdminComponentName.getPackageName());
    }

    @Override
    public void setApplicationRestrictions(String packageName, Bundle settings,
                                           Consumer<Void> onSuccess, Consumer<Exception> onError) {
        Log.d(TAG, "setApplicationRestrictions(" + packageName + ")");
        try {
            mDevicePolicyManager.setApplicationRestrictions(mAdminComponentName, packageName,
                    settings);
            onSuccess.accept(null);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public void setPermissionGrantState(String packageName, String permission, int grantState,
                                        Consumer<Boolean> onSuccess, Consumer<Exception> onError) {
        Log.d(TAG, "setPermissionGrantState(" + packageName + ", " + permission + "): "
                + Util.grantStateToString(grantState));
        try {
            boolean result = mDevicePolicyManager.setPermissionGrantState(mAdminComponentName,
                    packageName, permission, grantState);
            onSuccess.accept(result);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public int getPermissionGrantState(String packageName, String permission) {
        int grantState = mDevicePolicyManager.getPermissionGrantState(mAdminComponentName,
                packageName, permission);
        Log.d(TAG, "getPermissionGrantState(" + packageName + ", " + permission + "): "
                + Util.grantStateToString(grantState));
        return grantState;
    }

    @Override
    public void setLocationEnabled(boolean enabled, Consumer<Void> onSuccess,
                                   Consumer<Exception> onError) {
        Log.d(TAG, "setLocationEnabled(" + enabled + ")");
        try {
            mDevicePolicyManager.setLocationEnabled(mAdminComponentName, enabled);
            onSuccess.accept(null);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public boolean isLocationEnabled() {
        return mLocationManager.isLocationEnabled();
    }

    @Override
    public void setDeviceOwnerLockScreenInfo(CharSequence info, Consumer<Void> onSuccess,
                                             Consumer<Exception> onError) {
        Log.d(TAG, "setDeviceOwnerLockScreenInfo(" + info + ")");
        try {
            mDevicePolicyManager.setDeviceOwnerLockScreenInfo(mAdminComponentName, info);
            onSuccess.accept(null);
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    @Override
    public CharSequence getDeviceOwnerLockScreenInfo() {
        return mDevicePolicyManager.getDeviceOwnerLockScreenInfo();
    }

    @Override
    public String toString() {
        return "DevicePolicyManagerGatewayImpl[" + mAdminComponentName + "]";
    }
}
