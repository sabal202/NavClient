/*
 * Copyright (C) 2009 The Android Open Source Project
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

package sabal.navclient.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import sabal.navclient.DeviceData;
import sabal.navclient.activity.DeviceControlActivity;


public class DeviceConnector {
    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    private static final String TAG = "DeviceConnector";
    private static final boolean D = false;
    private final BluetoothAdapter btAdapter;
    private final BluetoothDevice connectedDevice;
    private final Handler mHandler;
    private final String deviceName;
    private int mState;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    public DeviceConnector(DeviceData deviceData, Handler handler) {
        mHandler = handler;
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        connectedDevice = btAdapter.getRemoteDevice(deviceData.getAddress());
        deviceName = (deviceData.getName() == null) ? deviceData.getAddress() : deviceData.getName();
        mState = STATE_NONE;
    }

    public synchronized void connect() {
        if (D) Log.d(TAG, "connect to: " + connectedDevice);

        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                if (D) Log.d(TAG, "cancel mConnectThread");
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        if (mConnectedThread != null) {
            if (D) Log.d(TAG, "cancel mConnectedThread");
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectThread = new ConnectThread(connectedDevice);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");

        if (mConnectThread != null) {
            if (D) Log.d(TAG, "cancel mConnectThread");
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            if (D) Log.d(TAG, "cancel mConnectedThread");
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    public synchronized int getState() {
        return mState;
    }

    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        mHandler.obtainMessage(DeviceControlActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized void connected(BluetoothSocket socket) {
        if (D) Log.d(TAG, "connected");

        if (mConnectThread != null) {
            if (D) Log.d(TAG, "cancel mConnectThread");
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            if (D) Log.d(TAG, "cancel mConnectedThread");
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_CONNECTED);

        Message msg = mHandler.obtainMessage(DeviceControlActivity.MESSAGE_DEVICE_NAME, deviceName);
        mHandler.sendMessage(msg);

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
    }


    public void write(byte[] data) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }

        if (data.length == 1) r.write(data[0]);
        else r.writeData(data);
    }


    private void connectionFailed() {
        if (D) Log.d(TAG, "connectionFailed");

        Message msg = mHandler.obtainMessage(DeviceControlActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_NONE);
    }


    private void connectionLost() {
        Message msg = mHandler.obtainMessage(DeviceControlActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_NONE);
    }

    private class ConnectThread extends Thread {
        private static final String TAG = "ConnectThread";
        private static final boolean D = false;

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        ConnectThread(BluetoothDevice device) {
            if (D) Log.d(TAG, "create ConnectThread");
            mmDevice = device;
            mmSocket = BluetoothUtils.createRfcommSocket(mmDevice);
        }

        public void run() {
            if (D) Log.d(TAG, "ConnectThread run");
            btAdapter.cancelDiscovery();
            if (mmSocket == null) {
                if (D) Log.d(TAG, "unable to connect to device, socket isn't created");
                connectionFailed();
                return;
            }

            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    if (D) Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            synchronized (DeviceConnector.this) {
                mConnectThread = null;
            }

            connected(mmSocket);
        }


        public void cancel() {
            if (D) Log.d(TAG, "ConnectThread cancel");

            if (mmSocket == null) {
                if (D) Log.d(TAG, "unable to close null socket");
                return;
            }
            try {
                mmSocket.close();
            } catch (IOException e) {
                if (D) Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private static final String TAG = "ConnectedThread";
        private static final boolean D = false;

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            if (D) Log.d(TAG, "create ConnectedThread");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                if (D) Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            if (D) Log.i(TAG, "ConnectedThread run");
            byte[] buffer = new byte[512];
            int bytes;
            StringBuilder readMessage = new StringBuilder();
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readed = new String(buffer, 0, bytes);
                    readMessage.append(readed);

                    if (readed.contains("\n")) {
                        mHandler.obtainMessage(DeviceControlActivity.MESSAGE_READ, bytes, -1, readMessage.toString()).sendToTarget();
                        readMessage.setLength(0);
                    }

                } catch (IOException e) {
                    if (D) Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void writeData(byte[] chunk) {

            try {
                mmOutStream.write(chunk);
                mmOutStream.flush();
                mHandler.obtainMessage(DeviceControlActivity.MESSAGE_WRITE, -1, -1, chunk).sendToTarget();
            } catch (IOException e) {
                if (D) Log.e(TAG, "Exception during write", e);
            }
        }

        public void write(byte command) {
            byte[] buffer = new byte[1];
            buffer[0] = command;

            try {
                mmOutStream.write(buffer);

                mHandler.obtainMessage(DeviceControlActivity.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                if (D) Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                if (D) Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
