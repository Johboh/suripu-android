package is.hello.sense.bluetooth.devices;

import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.TestBluetoothStackBehavior;
import is.hello.sense.bluetooth.stacks.TestPeripheral;
import is.hello.sense.bluetooth.stacks.TestPeripheralBehavior;
import is.hello.sense.bluetooth.stacks.util.AdvertisingData;
import is.hello.sense.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.sense.functional.Either;
import is.hello.sense.graph.InjectionTests;
import is.hello.sense.util.AdvertisingDataBuilder;
import is.hello.sense.util.Sync;

import static is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos.MorpheusCommand;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SensePeripheralTests extends InjectionTests {
    private static final String TEST_DEVICE_ID = "CA154FFA";

    @Inject TestBluetoothStackBehavior stackBehavior;
    @Inject BluetoothStack stack;

    private final TestPeripheralBehavior peripheralBehavior = new TestPeripheralBehavior("Sense-Test", "ca:15:4f:fa:b7:0b", -50);
    private SensePeripheral peripheral;

    @Before
    public void initialize() throws Exception {
        stackBehavior.reset();

        if (peripheral == null) {
            this.peripheral = new SensePeripheral(new TestPeripheral(stack, peripheralBehavior));
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void discovery() throws Exception {
        AdvertisingDataBuilder builder = new AdvertisingDataBuilder();
        builder.add(AdvertisingData.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS, SenseIdentifiers.ADVERTISEMENT_SERVICE_128_BIT);
        AdvertisingData advertisingData = builder.build();

        TestPeripheralBehavior device1 = new TestPeripheralBehavior("Sense-Test", "ca:15:4f:fa:b7:0b", -50);
        device1.setAdvertisingData(advertisingData);
        stackBehavior.addPeripheralInRange(device1);

        TestPeripheralBehavior device2 = new TestPeripheralBehavior("Sense-Test2", "c2:18:4e:fb:b3:0a", -90);
        device2.setAdvertisingData(advertisingData);
        stackBehavior.addPeripheralInRange(device2);

        PeripheralCriteria peripheralCriteria = new PeripheralCriteria();
        Sync.wrap(SensePeripheral.discover(stack, peripheralCriteria))
            .assertTrue(peripherals -> peripherals.size() == 2);
    }

    @Test
    public void rediscovery() throws Exception {
        AdvertisingDataBuilder builder = new AdvertisingDataBuilder();
        builder.add(AdvertisingData.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS, SenseIdentifiers.ADVERTISEMENT_SERVICE_128_BIT);
        builder.add(AdvertisingData.TYPE_SERVICE_DATA, SenseIdentifiers.ADVERTISEMENT_SERVICE_16_BIT + TEST_DEVICE_ID);
        AdvertisingData advertisingData = builder.build();

        TestPeripheralBehavior device = new TestPeripheralBehavior("Sense-Test", "ca:15:4f:fa:b7:0b", -50);
        device.setAdvertisingData(advertisingData);
        stackBehavior.addPeripheralInRange(device);

        Sync.wrap(SensePeripheral.rediscover(stack, TEST_DEVICE_ID, false))
            .assertTrue(p -> "Sense-Test".equals(p.getName()));
    }


    @Test
    public void getDeviceId() throws Exception {
        AdvertisingDataBuilder builder = new AdvertisingDataBuilder();
        builder.add(AdvertisingData.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS, SenseIdentifiers.ADVERTISEMENT_SERVICE_128_BIT);
        builder.add(AdvertisingData.TYPE_SERVICE_DATA, SenseIdentifiers.ADVERTISEMENT_SERVICE_16_BIT + TEST_DEVICE_ID);
        AdvertisingData advertisingData = builder.build();

        peripheralBehavior.setAdvertisingData(advertisingData);

        assertEquals(TEST_DEVICE_ID, peripheral.getDeviceId());
    }

    @Test
    public void writeLargeCommand() throws Exception {
        //noinspection ConstantConditions
        peripheralBehavior.setWriteCommandResponse(Either.left(null)); // Void could use a singleton instance...

        MorpheusCommand command = MorpheusCommand.newBuilder()
                .setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_SET_WIFI_ENDPOINT)
                .setVersion(0)
                .setWifiName("Mostly Radiation")
                .setWifiSSID("00:00:00:00:00:00")
                .setSecurityType(SenseCommandProtos.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN)
                .build();
        Sync.last(peripheral.writeLargeCommand(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND, command.toByteArray()));
        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.WRITE_COMMAND));
        assertEquals(3, peripheralBehavior.getCalledMethods().size());
    }
}