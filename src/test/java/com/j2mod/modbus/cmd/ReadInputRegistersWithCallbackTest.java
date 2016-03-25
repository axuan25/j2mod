/*
 * Copyright 2002-2016 jamod & j2mod development teams
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.j2mod.modbus.cmd;

import com.fazecast.jSerialComm.SerialPort;
import com.j2mod.modbus.ModbusException;
import com.j2mod.modbus.io.*;
import com.j2mod.modbus.msg.*;
import com.j2mod.modbus.net.ModbusMasterFactory;
import com.j2mod.modbus.util.Logger;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.wiringpi.Gpio;

import java.io.IOException;

/**
 * Class that implements a simple command line tool for writing to an analog
 * output over a Modbus/TCP connection.
 *
 * <p>
 * Note that if you write to a remote I/O with a Modbus protocol stack, it will
 * most likely expect that the communication is <i>kept alive</i> after the
 * first write message.
 *
 * <p>
 * This can be achieved either by sending any kind of message, or by repeating
 * the write message within a given period of time.
 *
 * <p>
 * If the time period is exceeded, then the device might react by turning off
 * all signals of the I/O modules. After this timeout, the device might require
 * a reset message.
 *
 * @author Dieter Wimberger
 * @author Julie Haugh
 * @version 1.03 (1/18/2014)
 *
 * @author Steve O'Hara (4energy)
 * @version 2.0 (March 2016)
 *
 */
public class ReadInputRegistersWithCallbackTest {

    private static final Logger logger = Logger.getLogger(ReadInputRegistersWithCallbackTest.class);
    public static final int RTS_PIN = 0;

    private static void printUsage() {
        logger.system("\nUsage:\n    java com.j2mod.modbus.cmd.ReadInputRegistersWithCallbackTest <address{:port{:unit}} [String]> <base [int]> <count [int]> {<repeat [int]>}");
    }

    public static void main(String[] args) {
        ModbusTransport transport = null;
        ModbusRequest req;
        ModbusTransaction trans;
        int ref = 0;
        int count = 0;
        int repeat = 1;
        int unit = 0;

        // 1. Setup parameters
        if (args.length < 3) {
            printUsage();
            System.exit(1);
        }

        GpioFactory.getInstance();

        try {
            try {
                // 2. Open the connection.
                transport = ModbusMasterFactory.createModbusMaster(args[0]);

                if (transport == null) {
                    logger.system("Cannot open %s", args[0]);
                    System.exit(1);
                }

                if (transport instanceof ModbusSerialTransport) {
                    ((ModbusSerialTransport)transport).setReceiveTimeout(1000);
                    ((ModbusSerialTransport)transport).addListener(new EventListener());
                    if (System.getProperty("com.j2mod.modbus.baud") != null) {
                        ((ModbusSerialTransport)transport).setBaudRate(Integer.parseInt(System.getProperty("com.j2mod.modbus.baud")));
                    }
                    else {
                        ((ModbusSerialTransport)transport).setBaudRate(9600);
                    }
                }

				/*
                 * There are a number of devices which won't initialize immediately
				 * after being opened.  Take a moment to let them come up.
				 */
                Thread.sleep(500);

                ref = Integer.parseInt(args[1]);
                count = Integer.parseInt(args[2]);

                if (args.length == 4) {
                    repeat = Integer.parseInt(args[3]);
                }

                if (transport instanceof ModbusTCPTransport) {
                    String parts[] = args[0].split(" *: *");
                    if (parts.length >= 4) {
                        unit = Integer.parseInt(parts[3]);
                    }
                }
                else if (transport instanceof ModbusRTUTransport) {
                    String parts[] = args[0].split(" *: *");
                    if (parts.length >= 3) {
                        unit = Integer.parseInt(parts[2]);
                    }

                    String baud = System.getProperty("com.j2mod.modbus.baud");
                    if (baud != null) {
                        ((ModbusRTUTransport)transport).setBaudRate(Integer.parseInt(baud));
                    }
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
                printUsage();
                System.exit(1);
            }

            // 3. Create the command.
            req = new ReadInputRegistersRequest(ref, count);
            req.setUnitID(unit);

            // 4. Prepare the transaction
            trans = transport.createTransaction();
            trans.setRequest(req);
            req.setHeadless(trans instanceof ModbusSerialTransaction);

            logger.system("Request: %s", req.getHexMessage());

            // 5. Execute the transaction repeat times

            for (int i = 0; i < repeat; i++) {
                try {
                    trans.execute();
                }
                catch (ModbusException x) {
                    logger.system(x.getMessage());
                    continue;
                }
                ModbusResponse res = trans.getResponse();
                if (res != null) {
                    logger.system("Response: %s", res.getHexMessage());
                }
                else {
                    logger.system("No response to READ HOLDING request");
                }
                if (res instanceof ExceptionResponse) {
                    ExceptionResponse exception = (ExceptionResponse)res;
                    logger.system(exception.toString());
                    continue;
                }

                if (!(res instanceof ReadInputRegistersResponse)) {
                    continue;
                }

                ReadInputRegistersResponse data = (ReadInputRegistersResponse)res;
                logger.system("Data: %3.1f", data.getRegister(0).toShort() / 10.0);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            // 6. Close the connection
            if (transport != null) {
                transport.close();
            }
        }
        catch (IOException e) {
            // Do nothing.
        }
        System.exit(0);
    }

    private static class EventListener implements ModbusSerialTransportListener {
        @Override
        public void event(EventType eventType, SerialPort port) {
            Gpio.pinMode(RTS_PIN, Gpio.OUTPUT);
            switch (eventType) {

                // Switch the UART into read mode
                case BEFORE_READ_REQUEST:
                case BEFORE_READ_RESPONSE:
                    Gpio.delayMicroseconds(1000);
                    Gpio.digitalWrite(RTS_PIN, false);
                    break;

                // Switch the UART into write mode
                case BEFORE_WRITE_MESSAGE:
                    Gpio.digitalWrite(RTS_PIN, true);
            }
        }
    }
}