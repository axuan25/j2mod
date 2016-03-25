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
package com.j2mod.modbus;

import com.j2mod.modbus.procimg.SimpleInputRegister;
import com.j2mod.modbus.utils.AbstractTestModbusTCPMaster;
import org.junit.Assert;
import org.junit.Test;

/**
 * This class tests the TCP master write features of the library
 */
@SuppressWarnings("ConstantConditions")
public class TestModbusTCPMasterWrite extends AbstractTestModbusTCPMaster {

    @Test
    public void testWriteCoils() {
        try {
            boolean before = master.readCoils(UNIT_ID, 1, 1).getBit(0);
            master.writeCoil(UNIT_ID, 1, !before);
            Assert.assertEquals("Incorrect status for coil 1", true, !before);
            master.writeCoil(UNIT_ID, 1, before);
        }
        catch (Exception e) {
            Assert.fail(String.format("Cannot write to coil 1 - %s", e.getMessage()));
        }
    }

    @Test
    public void testWriteHoldingRegisters() {
        try {
            int before = master.readInputRegisters(UNIT_ID, 1, 1)[0].getValue();
            master.writeSingleRegister(UNIT_ID, 1, new SimpleInputRegister(9999));
            Assert.assertEquals("Incorrect status for register 1", 9999, master.readInputRegisters(UNIT_ID, 1, 1)[0].getValue());
            master.writeSingleRegister(UNIT_ID, 1, new SimpleInputRegister(before));
        }
        catch (Exception e) {
            Assert.fail(String.format("Cannot write to register 1 - %s", e.getMessage()));
        }
    }

}