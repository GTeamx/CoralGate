/*
*
* This code was written by Gemini 3.1 Pro and cleaned up by a human.
* GTeam does not claim ownership of this code.
*
* Yeah I was too lazy to write it all...
*
 */
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Main {

    private static final String TARGET_IP = "127.0.0.1";
    private static final int TARGET_PORT = 25565;
    private static final int PROTOCOL_VERSION = 776; // 26.2

    private static final String FORGED_MOTD = "{\"description\":{\"text\":\"\",\"extra\":[\"A Minecraft Server\"]},\"players\":{\"max\":20,\"online\":0},\"version\":{\"name\":\"CraftBukkit 26.2\",\"protocol\":776},\"enforcesSecureChat\":true}";

    private static boolean isExposed = false; // Added to track if the backend leaked.

    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";

    public static void main(String[] args) {

        final long startTime = System.currentTimeMillis();

        System.out.println("Starting CoralGate filter tester v1.1.1");

        // Broken handshake.
        System.out.print(" (1) Broken handshake: ");
        runTest(50000, "STATUS", out -> {
            sendHandshake(out, 1, PROTOCOL_VERSION);
            sendStatusRequest(out);
        });

        // Bot username.
        System.out.print(" (2) Bot username: ");
        runTest(50001, "LOGIN", out -> {
            sendHandshake(out, 2, PROTOCOL_VERSION);
            sendLoginStart(out, "Player12345");
        });

        // Suspicious port.
        System.out.print(" (3) Suspicious port: ");
        runTest(40000, "STATUS", out -> {
            sendHandshake(out, 1, PROTOCOL_VERSION);
            sendStatusRequest(out);
        });

        // Invalid port.
        System.out.print(" (4) Invalid port: ");
        runTest(30000, "STATUS", out -> {
            sendHandshake(out, 1, PROTOCOL_VERSION);
            sendStatusRequest(out);
        });

        // Skip handshake.
        System.out.print(" (5) Jump packet: ");
        runTest(65535, "LOGIN", out -> sendLoginStart(out, "CoralGate"));

        // Invalid protocol.
        System.out.print(" (6) Invalid protocol: ");
        runTest(50003, "STATUS", out -> {
            sendHandshake(out, 1, 100);
            sendStatusRequest(out);
        });

        System.out.println("Scan finished in " + (System.currentTimeMillis() - startTime) + "ms.");

    }

    private static void runTest(final int sourcePort, final String expectedState, final PacketSender packetSender) {

        try (final Socket socket = new Socket()) {

            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(sourcePort));
            socket.connect(new InetSocketAddress(TARGET_IP, TARGET_PORT), 3000);

            final DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            final DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

            packetSender.send(dataOutputStream);
            parseServerResponse(dataInputStream, expectedState);

        } catch (final BindException e) {
            System.out.println(RED + "(!) Cannot bind to port " + sourcePort + ". Socket still locked by OS allocation." + RESET);
        } catch (final Exception e) {
            System.out.println(RED + "(!) Connection error: " + e.getMessage() + RESET);
        }

    }

    private static void parseServerResponse(final DataInputStream dataInputStream, final String state) {

        try {

            readVarInt(dataInputStream);
            final int packetId = readVarInt(dataInputStream);

            if ("STATUS".equals(state)) {

                if (packetId == 0x00) {

                    final String jsonMOTD = readString(dataInputStream);

                    if (jsonMOTD.equals(FORGED_MOTD)) {

                        System.out.println(GREEN + "PASSED! (filter active)" + RESET);

                    } else {

                        System.out.println(RED + "FAILED! (MOTD retrieved: " + jsonMOTD + ")" + RESET);
                        isExposed = true;

                    }

                } else {

                    System.out.println(RED + "(!) Received unexpected Status packet ID: 0x" + Integer.toHexString(packetId) + RESET);
                    isExposed = true;

                }

            } else if ("LOGIN".equals(state)) {

                if (packetId == 0x00) {

                    System.out.println(RED + "FAILED! (server reached)" + RESET);
                    isExposed = true;

                } else if (packetId == 0x01) {

                    System.out.println(RED + "FAILED! (server replied with encryption request)" + RESET);
                    isExposed = true;

                } else if (packetId == 0x02) {

                    System.out.println(RED + "FAILED! (server replied with login success)" + RESET);
                    isExposed = true;

                } else {

                    System.out.println(RED + "FAILED! (0x" + Integer.toHexString(packetId) + ")" + RESET);
                    isExposed = true;

                }

            }

        } catch (final IOException e) {

            // If the server was already exposed by a previous test, an IOException just means the vanilla server crashed the socket.
            if (isExposed) {

                System.out.println(RED + "FAILED! (protocol crash)" + RESET);

            } else {

                // If it hasn't leaked yet, an abrupt drop indicates active proxy mitigation.
                System.out.println(GREEN + "PASSED! (zero bytes returned)" + RESET);
            }

        }

    }

    interface PacketSender {
        void send(final DataOutputStream dataOutputStream) throws IOException;
    }

    private static void sendHandshake(final DataOutputStream dataOutputStream, final int nextState, final int protocolVersion) throws IOException {

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final DataOutputStream handshakeOutputStream = new DataOutputStream(byteArrayOutputStream);

        handshakeOutputStream.writeByte(0x00);

        writeVarInt(handshakeOutputStream, protocolVersion);
        writeString(handshakeOutputStream, Main.TARGET_IP);

        handshakeOutputStream.writeShort(Main.TARGET_PORT);

        writeVarInt(handshakeOutputStream, nextState);
        writePacket(dataOutputStream, byteArrayOutputStream.toByteArray());

    }

    private static void sendStatusRequest(final DataOutputStream dataOutputStream) throws IOException {

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final DataOutputStream requestOutputStream = new DataOutputStream(byteArrayOutputStream);

        requestOutputStream.writeByte(0x00);
        writePacket(dataOutputStream, byteArrayOutputStream.toByteArray());

    }

    private static void sendLoginStart(final DataOutputStream dataOutputStream, final String username) throws IOException {

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final DataOutputStream loginStartOutputStream = new DataOutputStream(byteArrayOutputStream);

        loginStartOutputStream.writeByte(0x00);

        writeString(loginStartOutputStream, username);

        loginStartOutputStream.writeLong(0L);
        loginStartOutputStream.writeLong(1L);

        writePacket(dataOutputStream, byteArrayOutputStream.toByteArray());

    }

    private static void writePacket(final DataOutputStream dataOutputStream, final byte[] data) throws IOException {

        writeVarInt(dataOutputStream, data.length);

        dataOutputStream.write(data);
        dataOutputStream.flush();

    }

    private static void writeVarInt(final DataOutputStream dataOutputStream, int value) throws IOException {

        while (true) {

            if ((value & ~0x7F) == 0) {

                dataOutputStream.writeByte(value);
                return;

            }

            dataOutputStream.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;

        }

    }

    private static int readVarInt(final DataInputStream dataInputStream) throws IOException {

        int numRead = 0, result = 0;
        byte read;
        do {

            read = dataInputStream.readByte();
            result |= ((read & 0b01111111) << (7 * numRead));

            if (numRead++ > 5) throw new RuntimeException("VarInt too big");

        } while ((read & 0b10000000) != 0);

        return result;

    }

    private static void writeString(final DataOutputStream dataOutputStream, final String value) throws IOException {

        final byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        writeVarInt(dataOutputStream, bytes.length);

        dataOutputStream.write(bytes);

    }

    private static String readString(final DataInputStream dataInputStream) throws IOException {

        final byte[] bytes = new byte[readVarInt(dataInputStream)];

        dataInputStream.readFully(bytes);

        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

    }

}
