package com.example.arduinocommunication;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);

        System.out.println(Context.USB_SERVICE);
        HashMap<String, UsbDevice> devices = manager.getDeviceList();
        Log.d("hello", "Devices: " + devices);
        UsbDevice device = devices.values().iterator().next();
        manager.requestPermission(device, permissionIntent);
        manager.requestPermission(device, permissionIntent);

        System.out.println(manager.hasPermission(device));

        // find the endpoints and interfaces necessary to connect to the USB comm

        Log.d("hello", "Device Name: " + device.getDeviceName());
        Log.d("hello", "Manufacturer Name: " + device.getManufacturerName());
        Log.d("hello", "Product Name: " + device.getProductName());
        Log.d("hello", "ProductId: " + device.getProductId());
        Log.d("hello", "VendorId: " + device.getVendorId());
        Log.d("hello", "interface Count: " + device.getInterfaceCount());

        for (int j = 0; j < device.getInterfaceCount(); j++) {
            Log.d("hello", "interface #"+j+": " + device.getInterface(j));
            Log.d("hello", "interface class: " + device.getInterface(j).getInterfaceClass());
            Log.d("hello", "Endpoint Count: " + device.getInterface(j).getEndpointCount());
            if (device.getInterface(j).getInterfaceClass() == 10) {
                UsbInterface interfaceEndpoint = device.getInterface(j);
                for (int i = 0; i < interfaceEndpoint.getEndpointCount(); i++) {
                    Log.d("hello", "Endpoint " + i + ": " + interfaceEndpoint.getEndpoint(i));
                    Log.d("hello", "Endpoint " + i + ": " + interfaceEndpoint.getEndpoint(i).getType());
                    Log.d("hello", "Endpoint " + i + " address: " + interfaceEndpoint.getEndpoint(i).getAddress());
                    Log.d("hello", "Endpoint " + i + " Direction: " + interfaceEndpoint.getEndpoint(i).getDirection());
                }
                break;
            }
        }
        Log.d("hello","Constant IN:" + UsbConstants.USB_DIR_IN);
        Log.d("hello","Constant OUT:" + UsbConstants.USB_DIR_OUT);
        Log.d("hello","Constant Type:" + UsbConstants.USB_ENDPOINT_XFER_BULK);
        Log.d("hello","Constant CDC:" + UsbConstants.USB_CLASS_CDC_DATA);

        UsbInterface interfaceEndpoint = device.getInterface(1);
        UsbEndpoint endpointDev = interfaceEndpoint.getEndpoint(1);

        UsbDeviceConnection connection = manager.openDevice(device);


        /*// controlTransfer for CH34x
        /*int response = connection.controlTransfer(0x40, 0x9a, 0x1312, 0xb282, null, 0, 0); //baudrate 9600
        Log.d("hello", "SetBaudrate-response: " + response);
        response = connection.controlTransfer(0x40, 0x9a, 0x0f2c, 0x08, null, 0, 0);
        Log.d("hello", "SetParity-response: " + response);
        response = connection.controlTransfer(0x40, 0x9a, 0x2727, 0, null, 0, 0);
        Log.d("hello", "SetControlFlow-response: " + response);
        response = connection.controlTransfer(0x40, 0x9a, 0x2727, 0, null, 0, 0);
        Log.d("hello", "SetControlFlow-response: " + response);*/


        int baudRate = 9600;
        byte  parityBitesByte = 0;
        byte stopBitsByte = 0;

       /* CDC Configuration for USB Communication*/

        //int datalength = data.length;

        //int response = connection.controlTransfer(0x21, 34, 0, 0, null, 0, 0);
        //Log.d("hello", "Setting_CDC: " + response);
        //response = connection.controlTransfer(0x21, 0x20, 0, 0, new byte[] {(byte) 0x80, 0x25, 0x00, 0x00, 0x00, 0x00, 0x08}, 7, 0)
        //Log.d("hello", "Setting_CDC: " + response);
        int response = connection.controlTransfer(0x21, 0x22, 0x1, 0, null, 0, 0);
        Log.d("hello", "Setting_CDC: " + response);
        response = connection.controlTransfer(0x21, 0x20, 0, 0, new byte[] {(byte) 0x80, 0x25, 0x00, 0x00, 0x00, 0x00, 0x08}, 7, 0);
        Log.d("hello", "Setting_CDC: " + response);

        //response = connection.controlTransfer(0x21, 0x22, 0x2, 0, null, 0, 0);
       // Log.d("hello", "SetCDC 2: " + response);



        byte[] bytes = new byte[255];
        byte[] bytesN = new byte[255];
        int TIMEOUT = 1000;


        UsbRequest request = new UsbRequest();
        ByteBuffer buffer = ByteBuffer.allocate(255);
        connection.claimInterface(interfaceEndpoint,true);
        request.initialize(connection,interfaceEndpoint.getEndpoint(0));

        Log.d("hello", "Started the usb listener");



        Thread threadData = new Thread(new Runnable() {
            @Override
            public void run() {
                String dat = "";

                int packetState = 0;
                int headerCount = 0;
                boolean readThreadRunning = true;
        while (readThreadRunning) {
            String dataByte = "";

            Log.d("hello", "REQUEST: " + request);
            request.queue(buffer, buffer.capacity());

            if (connection.requestWait() == request) {

                for (int i = 0; i < buffer.capacity() && buffer.get(i) != 0; i++) {
                    // transform ascii (0-255) to its character equivalent and append
                    dataByte = Character.toString((char) buffer.get(i));
                    //Log.e("hello", "databyte: " + dataByte);

                    if(packetState == 0 && dataByte.equals("["))  {
                        headerCount += 1;
                        packetState = 1;
                    }
                    else if(packetState == 1 && !dataByte.equals("]")) {
                        // in-between
                        if (dataByte.equals("[")) {
                            headerCount += 1;
                        }
                        dat += dataByte;
                    }
                    else if(packetState == 1 && dataByte.equals("]")) {
                        // end
                        headerCount -= 1;
                        if (headerCount == 0) {
                            packetState = 2;
                            break;
                        }
                        dat += dataByte;
                    }
                }

                if(packetState == 2)
                {

                    Log.e("hello", "data received: " + dat);
                    //Log.e("hello", "databyte: " + dataByte);
                    // reset packet
                    packetState = 0;
                    dat  = "";
                }
            } else {
                Log.e("hello", "No more data");
                readThreadRunning = false;
            }
        }
            }
    });
        threadData.start();

        /* This piece will send data back to the Particle
        Using the USB Com*/
         */
        String data = "Worlc\n" +
                "";
        int sentBytes = 0;

        // send data to usb device
        byte[] tada = data.getBytes();
        sentBytes = connection.bulkTransfer(endpointDev, tada, tada.length, 1000);
        Log.e("hello", "databytes: "+ data.getBytes());


                    /*if (packetState == 0 && dataByte.equals("[")) {
                        // start
                        packetState = 1;
                        data += dataByte;
                        Log.d("hello", "start: " + data);
                    } else if (packetState == 1 && !dataByte.equals("]")) {
                        // in-between
                        data += dataByte;
                        Log.d("hello", "middle: " + data);
                    } else if (packetState == 1 && dataByte.equals("]")) {
                        // end
                        packetState = 2;
                        data += dataByte;
                        Log.d("hello", "end: " + data);
                        break;
                    }
                }
                if (packetState == 2) {
                    Log.d("hello", "this is the data: " + data);
                }
            } else {
                Log.e("hello", "Was not able to read from USB device, ending listening thread");
                readThreadRunning = false;
            }*/



       /* for (UsbDevice device : devices.values()) {
            Log.d("hello", "Device Name: " + device.getDeviceName());
            Log.d("hello", "Manufacturer Name: " + device.getManufacturerName());
            Log.d("hello", "Product Name: " + device.getProductName());
            Log.d("hello", "ProductId: " + device.getProductId());
            Log.d("hello", "VendorId: " + device.getVendorId());
            Log.d("hello", "interface Count: " + device.getInterfaceCount());
            Log.d("hello", "interface #1: " + device.getInterface(0));
            Log.d("hello", "interface class: " + device.getInterface(0).getInterfaceClass());
            Log.d("hello", "Endpoint Count: " + device.getInterface(0).getEndpointCount());

            //manager.requestPermission(device, permissionIntent);
            UsbInterface interfaceEndpoint = device.getInterface(0);

            for (int i = 0; i < interfaceEndpoint.getEndpointCount(); i++) {
                Log.d("hello", "Endpoint "+ i +": "  + interfaceEndpoint.getEndpoint(i).getType());
                Log.d("hello", "Endpoint "+ i +" Direction: "  + interfaceEndpoint.getEndpoint(i).getDirection());

            }
            System.out.println(UsbConstants.USB_ENDPOINT_XFER_BULK);
            System.out.println(UsbConstants.USB_CLASS_CDC_DATA);
        }

        int response = connection.controlTransfer(0x40,0x9A,0x2518,0xd982,null);
        Log.d("Serial", "SetBaudrate-response: " + response);*/
    }
}