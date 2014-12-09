package is.hello.sense.bluetooth.devices;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import is.hello.sense.bluetooth.devices.transmission.SensePacketHandler;
import is.hello.sense.bluetooth.devices.transmission.protobuf.MorpheusBle;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.TestBluetoothStackBehavior;
import is.hello.sense.bluetooth.stacks.TestPeripheral;
import is.hello.sense.bluetooth.stacks.TestPeripheralBehavior;
import is.hello.sense.bluetooth.stacks.util.AdvertisingData;
import is.hello.sense.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.sense.functional.Either;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.SyncObserver;
import rx.Observable;

import static is.hello.sense.bluetooth.devices.transmission.protobuf.MorpheusBle.MorpheusCommand;

public class SensePeripheralTests extends InjectionTestCase {
    private static final String TEST_DEVICE_ID = "ca154ffa";

    @Inject TestBluetoothStackBehavior stackBehavior;
    @Inject BluetoothStack stack;

    private final SensePacketHandler packetHandler = new SensePacketHandler();
    private final TestPeripheralBehavior peripheralBehavior = new TestPeripheralBehavior("Sense-Test", "ca:15:4f:fa:b7:0b", -50);
    private SensePeripheral peripheral;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        stackBehavior.reset();

        if (peripheral == null) {
            this.peripheral = new SensePeripheral(new TestPeripheral(stack, peripheralBehavior));
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testDiscovery() throws Exception {
        Set<AdvertisingData.Payload> scanResponse = new HashSet<>();
        scanResponse.add(new AdvertisingData.Payload(AdvertisingData.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS, SenseIdentifiers.ADVERTISEMENT_SERVICE_128_BIT));

        TestPeripheralBehavior device1 = new TestPeripheralBehavior("Sense-Test", "ca:15:4f:fa:b7:0b", -50);
        device1.setScanResponse(scanResponse);
        stackBehavior.addPeripheralInRange(device1);

        TestPeripheralBehavior device2 = new TestPeripheralBehavior("Sense-Test2", "c2:18:4e:fb:b3:0a", -90);
        device1.setScanResponse(scanResponse);
        stackBehavior.addPeripheralInRange(device2);

        PeripheralCriteria peripheralCriteria = new PeripheralCriteria();
        SyncObserver<List<SensePeripheral>> peripherals = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, SensePeripheral.discover(stack, peripheralCriteria));
        peripherals.await();

        assertNull(peripherals.getError());
        assertEquals(2, peripherals.getSingle().size());
    }

    public void testRediscovery() throws Exception {
        Set<AdvertisingData.Payload> scanResponse = new HashSet<>();
        scanResponse.add(new AdvertisingData.Payload(AdvertisingData.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS, SenseIdentifiers.ADVERTISEMENT_SERVICE_128_BIT));
        scanResponse.add(new AdvertisingData.Payload(AdvertisingData.TYPE_SERVICE_DATA, SenseIdentifiers.ADVERTISEMENT_SERVICE_16_BIT + TEST_DEVICE_ID));

        TestPeripheralBehavior device = new TestPeripheralBehavior("Sense-Test", "ca:15:4f:fa:b7:0b", -50);
        device.setScanResponse(scanResponse);
        stackBehavior.addPeripheralInRange(device);

        SyncObserver<SensePeripheral> peripherals = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, SensePeripheral.rediscover(stack, TEST_DEVICE_ID));
        peripherals.await();

        assertNull(peripherals.getError());
        assertNotNull(peripherals.getSingle());
        assertEquals("Sense-Test", peripherals.getSingle().getName());
    }


    public void testWriteLargeCommand() throws Exception {
        //noinspection ConstantConditions
        peripheralBehavior.setWriteCommandResponse(Either.left(null)); // Void could use a singleton instance...

        MorpheusCommand command = MorpheusCommand.newBuilder()
                .setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_SET_WIFI_ENDPOINT)
                .setVersion(0)
                .setWifiName("Mostly Radiation")
                .setWifiSSID("00:00:00:00:00:00")
                .setSecurityType(MorpheusBle.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN)
                .build();
        Observable<Void> write = peripheral.writeLargeCommand(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND, command.toByteArray());
        SyncObserver<Void> writeObserver = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, write);
        writeObserver.await();

        assertNull(writeObserver.getError());
        assertTrue(peripheralBehavior.wasMethodCalled(TestPeripheralBehavior.Method.WRITE_COMMAND));
        assertEquals(3, peripheralBehavior.getCalledMethods().size());
    }
}
