package com.fr4gus.litoct.nsd;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

/**
 * Created by Franklin Garcia on 10/21/17.
 */

public class NsdHelper {
    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.RegistrationListener mRegistrationListener;
    private NsdManager.ResolveListener mResolveListener;

    private boolean mServiceRegistered = false;
    private boolean mIsDiscovering;
    private boolean mIsMaster;

    private static final String SERVICE_TYPE = "_http._tcp.";

    private static final String TAG = NsdHelper.class.getSimpleName();
    private String mServiceName = "ServiceOnNsd";

    private ClientListener mClientListener;
    private ServerListener mServerListener;

    public NsdHelper(Context context, String serviceName, boolean isMaster) {
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        mServiceName = serviceName;
        mIsMaster = isMaster;
        if (isMaster) {
            mServiceName = mServiceName + "-Master";
        }
    }

    public void init() {
        if (mIsMaster) {
            initializeRegistrationListener();
        } else {
            initializeDiscoveryListener();
        }
    }


    private void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
                mIsDiscovering = true;
                mClientListener.onDiscoveryStarted();

            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "Service discovery success " + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().contains(mServiceName)) {
                    mNsdManager.resolveService(service, new LocalResolveListener());
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost" + service);
                mClientListener.onServiceLost(service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
                mIsDiscovering = false;
                mClientListener.onDiscoveryStopped();
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Start Discovery failed: Error code:" + errorCode);
                mClientListener.onDiscoveryStartFail();

            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Stop Discovery failed: Error code:" + errorCode);
                mClientListener.onDiscoveryStopFail();
            }
        };
    }

    /**
     * Used for "Master" service, to register itself and be available
     */
    private void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                mServiceRegistered = true;
                mServiceName = NsdServiceInfo.getServiceName();

                Log.d(TAG, "Service registered " + mServiceName);
                mServerListener.onRegistrationSuccess();
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
                Log.d(TAG, "Registration failed");
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                Log.d(TAG, "Service unregistered");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d(TAG, "Unregistration failed");
            }
        };
    }

    /**
     * Registers a new service into the to be discovered through NSD
     *
     * @param port port where the server is listening connections
     */
    public void registerService(int port) {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);
        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
        Log.d(TAG, "Registering server at port " + port);

    }

    public void stopDiscovery() {
        if (mIsDiscovering) {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }
    }

    public void tearDown() {
        if (mServiceRegistered) {
            mNsdManager.unregisterService(mRegistrationListener);
            mServiceRegistered = false;
        }
    }

    public class LocalResolveListener implements NsdManager.ResolveListener {

        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "[" + errorCode + "] Resolve failed " + serviceInfo);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.i(TAG, "Resolve Succeeded. " + serviceInfo);

            mClientListener.onServiceFound(serviceInfo);
        }
    }

    public interface ClientListener {
        void onDiscoveryStarted();

        void onDiscoveryStartFail();

        void onDiscoveryStopFail();

        void onDiscoveryStopped();

        void onServiceFound(NsdServiceInfo info);

        void onServiceLost(NsdServiceInfo info)
    }

    public interface ServerListener {
        void onRegistrationSuccess();

        void onRegistratoinFail();
    }
}
