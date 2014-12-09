package is.hello.sense.bluetooth.devices.transmission;

import junit.framework.TestCase;

import java.util.List;
import java.util.UUID;

import is.hello.sense.bluetooth.devices.SenseIdentifiers;
import is.hello.sense.bluetooth.stacks.transmission.SequencedPacket;
import is.hello.sense.functional.Lists;
import is.hello.sense.util.LambdaVar;

import static is.hello.sense.bluetooth.devices.transmission.protobuf.MorpheusBle.MorpheusCommand;
import static is.hello.sense.bluetooth.devices.transmission.protobuf.MorpheusBle.wifi_endpoint;

public class SensePacketDataHandlerTests extends TestCase {
    private final SensePacketDataHandler packetDataHandler = new SensePacketDataHandler();
    private final SensePacketHandler packetHandler = new SensePacketHandler();
    
    public void testShouldProcessCharacteristic() throws Exception {
        assertTrue(packetDataHandler.shouldProcessCharacteristic(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE));
        assertFalse(packetDataHandler.shouldProcessCharacteristic(UUID.fromString("D1700CFA-A6F8-47FC-92F5-9905D15F261C")));
    }

    public void testProcessPacketOutOfOrder() throws Exception {
        SequencedPacket testPacket = new SequencedPacket(99, new byte[] {});

        LambdaVar<MorpheusCommand> response = LambdaVar.empty();
        LambdaVar<Throwable> error = LambdaVar.empty();
        packetDataHandler.onResponse = response::set;
        packetDataHandler.onError = error::set;

        packetDataHandler.processPacket(testPacket);

        assertNull(response.get());
        assertNotNull(error.get());
    }

    public void testProcessPacketInOrder() throws Exception {
        MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                .setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_GET_WIFI_ENDPOINT)
                .setWifiSSID("Mostly Radiation")
                .setSecurityType(wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN)
                .setVersion(0)
                .build();

        List<byte[]> rawPackets = packetHandler.createPackets(morpheusCommand.toByteArray());

        List<SequencedPacket> packets = Lists.mapList(rawPackets, payload ->
                packetHandler.createSequencedPacket(SenseIdentifiers.CHARACTERISTIC_PROTOBUF_COMMAND_RESPONSE, payload));


        LambdaVar<MorpheusCommand> response = LambdaVar.empty();
        packetDataHandler.onResponse = response::set;

        LambdaVar<Throwable> error = LambdaVar.empty();
        packetDataHandler.onError = error::set;

        for (SequencedPacket packet : packets) {
            packetDataHandler.processPacket(packet);
        }

        assertNull(error.get());
        assertNotNull(response.get());
        assertEquals("Mostly Radiation", response.get().getWifiSSID());
        assertEquals(wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN, response.get().getSecurityType());
    }
}
