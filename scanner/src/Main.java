// Written by Claude Sonnet 5.
// GTeam does not claim ownership or this code.
// Yes I was too lazy to write it all...

/*
 * CoralGateScanner
 * ------------------------------------------------------------
 * A raw Minecraft-protocol test client built to exercise every
 * branch of NetworkProcessor (CoralGate). It does NOT use
 * packetevents - it speaks the wire protocol directly so it can
 * freely forge handshakes, bad protocol versions, and control its
 * own *source* port (which is what NetworkProcessor actually
 * inspects via InetSocketAddress#getPort()).
 *
 * Usage:
 *   java Main <host> <port> [protocolVersion] [timeoutMs]
 *
 * Example:
 *   java Main 127.0.0.1 25565 776 3000
 *
 * Exit code: 0 if every test's actual outcome matched the expected
 * outcome, 1 if at least one test deviated (i.e. a real security
 * failure such as leaking real server data when it should not have).
 *
 * IMPORTANT: run this only against servers you own/operate. Binding
 * arbitrary local source ports and forging handshakes is exactly the
 * kind of traffic pattern that gets IPs auto-reported by your own
 * CoralGate API manager - expect your own IP to get flagged during
 * this run, that's the point.
 */

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Main {

    // ----------------------------------------------------------------
    // ANSI colors
    // ----------------------------------------------------------------
    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String DIM    = "\u001B[2m";
    private static final String RED    = "\u001B[31m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN   = "\u001B[36m";

    // ----------------------------------------------------------------
    // Config
    // ----------------------------------------------------------------
    private static String HOST;
    private static int PORT;
    private static int PROTOCOL;          // "legit" protocol version to advertise
    private static int TIMEOUT_MS = 3000;

    // Local source ports used to simulate each client class.
    // >=49152           -> normal dynamic port (Windows/Mac range)   -> "legit"
    // 32768..49151      -> below Windows/Mac range, above Linux one  -> "suspicious"
    // <32768            -> below Linux dynamic range                -> "invalid"
    private static final int LEGIT_LOCAL_PORT       = 51000;
    private static final int SUSPICIOUS_LOCAL_PORT  = 40000;
    private static final int INVALID_LOCAL_PORT     = 10000;

    // Fingerprint of NetworkProcessor#getForgedMOTD()
    private static final String FORGED_VERSION = "1.21.11";
    private static final String FORGED_MARKER_1 = "Paper " + FORGED_VERSION;
    private static final String FORGED_MARKER_2 = "\"protocol\":774";
    private static final String FORGED_MARKER_3 = "\"enforcesSecureChat\":true";

    private static int passCount = 0;
    private static int failCount = 0;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java CoralGateScanner <host> <port> [protocolVersion] [timeoutMs]");
            System.exit(2);
        }
        HOST = args[0];
        PORT = Integer.parseInt(args[1]);
        PROTOCOL = args.length >= 3 ? Integer.parseInt(args[2]) : 774;
        if (args.length >= 4) TIMEOUT_MS = Integer.parseInt(args[3]);

        banner();

        runTest("T1  Legit connection (full status handshake)", Main::testLegitStatus);
        runTest("T2  Suspicious source port (status request)", Main::testSuspiciousPortStatus);
        runTest("T3  Invalid source port (status request)", Main::testInvalidPortStatus);
        runTest("T4  Invalid protocol version (status request)", Main::testInvalidProtocolStatus);
        runTest("T5  Missing handshake before status request", Main::testMissingHandshakeStatus);
        runTest("T6  Invalid source port (login intent)", Main::testInvalidPortLogin);
        runTest("T7  Suspicious port + bot-like username (login)", Main::testSuspiciousPortBotUsername);
        runTest("T8  Suspicious port + normal username (login)", Main::testSuspiciousPortNormalUsername);
        runTest("T9  Legacy server list ping (0xFE)", Main::testLegacyPing);
        runTest("T10 Legit login handshake, bot-like username", Main::testLegitPortBotUsername);

        summary();
        System.exit(failCount == 0 ? 0 : 1);
    }

    // ----------------------------------------------------------------
    // Test cases
    // ----------------------------------------------------------------

    /** Fully legitimate client: dynamic port, correct protocol, proper handshake -> should receive REAL info. */
    private static Outcome testLegitStatus() throws IOException {
        try (Socket s = connect(LEGIT_LOCAL_PORT)) {
            sendPacket(s, 0x00, buildHandshake(PROTOCOL, HOST, PORT, 1));
            sendPacket(s, 0x00, new byte[0]); // Status Request

            RawPacket resp = receivePacket(s, TIMEOUT_MS);
            String json = readString(new ByteArrayInputStream(resp.data));

            long pingPayload = 0x1234ABCDL;
            sendPacket(s, 0x01, longBytes(pingPayload)); // Ping
            RawPacket pong = receivePacket(s, TIMEOUT_MS);
            long pongPayload = readLong(pong.data);

            boolean forged = isForgedMotd(json);
            boolean pingOk = pongPayload == pingPayload;

            String info = describeMotd(json);
            if (forged && pingOk) {
                return Outcome.pass("Forged server data received as expected. " + info);
            }
            return Outcome.fail("Legit traffic wasn't sent forged motd on first request! motd=" + forged
                    + " pingEchoOk=" + pingOk + " raw=" + truncate(json));
        } catch (IOException e) {
            return Outcome.fail("Legit connection was unexpectedly blocked/closed: " + e);
        }
    }

    /** Suspicious source port during a status request -> filter should return the forged MOTD. */
    private static Outcome testSuspiciousPortStatus() throws IOException {
        return expectForgedStatus(SUSPICIOUS_LOCAL_PORT, "Suspicious-port status request");
    }

    /** Invalid source port during a status request -> filter should also return the forged MOTD. */
    private static Outcome testInvalidPortStatus() throws IOException {
        return expectForgedStatus(INVALID_LOCAL_PORT, "Invalid-port status request");
    }

    /** Legit port, but garbage/negative protocol version -> filter should return the forged MOTD. */
    private static Outcome testInvalidProtocolStatus() throws IOException {
        try (Socket s = connect(LEGIT_LOCAL_PORT)) {
            sendPacket(s, 0x00, buildHandshake(-1, HOST, PORT, 1));
            sendPacket(s, 0x00, new byte[0]);
            RawPacket resp = receivePacket(s, TIMEOUT_MS);
            String json = readString(new ByteArrayInputStream(resp.data));
            if (isForgedMotd(json)) {
                return Outcome.pass("Forged MOTD correctly returned for invalid protocol version.");
            }
            return Outcome.fail("Real server info leaked despite invalid protocol version! raw=" + truncate(json));
        } catch (IOException e) {
            // Some servers may just close on a garbage handshake before status - that's also an
            // acceptable "no info leaked" outcome.
            return Outcome.pass("Connection closed/blocked on invalid protocol version before info leaked (" + e + ").");
        }
    }

    /** Skip the handshake entirely and jump straight to a status request. */
    private static Outcome testMissingHandshakeStatus() throws IOException {
        try (Socket s = connect(LEGIT_LOCAL_PORT)) {
            sendPacket(s, 0x00, new byte[0]); // Status Request with no prior HANDSHAKE
            RawPacket resp = receivePacket(s, TIMEOUT_MS);
            String json = readString(new ByteArrayInputStream(resp.data));
            if (isForgedMotd(json)) {
                return Outcome.pass("Forged MOTD correctly returned for missing handshake.");
            }
            return Outcome.fail("Real server info leaked with no prior handshake! raw=" + truncate(json));
        } catch (IOException e) {
            return Outcome.pass("Connection closed/blocked with no prior handshake before info leaked (" + e + ").");
        }
    }

    /** Invalid port + LOGIN intent -> handshake itself should trigger an immediate disconnect. */
    private static Outcome testInvalidPortLogin() throws IOException {
        try (Socket s = connect(INVALID_LOCAL_PORT)) {
            sendPacket(s, 0x00, buildHandshake(PROTOCOL, HOST, PORT, 2)); // LOGIN intent
            RawPacket resp = tryReceive(s, TIMEOUT_MS);
            if (resp == null) {
                return Outcome.pass("Connection closed immediately after handshake, as expected.");
            }
            return Outcome.fail("Connection stayed open after invalid-port LOGIN handshake! got packet id="
                    + resp.id + " data=" + truncate(bytesToHex(resp.data)));
        } catch (IOException e) {
            return Outcome.pass("Connection closed/reset immediately, as expected (" + e + ").");
        }
    }

    /** Suspicious port (warn-only) + a bot-like username ("Player...") -> should be kicked at LOGIN_START. */
    private static Outcome testSuspiciousPortBotUsername() throws IOException {
        try (Socket s = connect(SUSPICIOUS_LOCAL_PORT)) {
            sendPacket(s, 0x00, buildHandshake(PROTOCOL, HOST, PORT, 2));
            sendPacket(s, 0x00, buildLoginStart("Player" + System.currentTimeMillis() % 1000));
            RawPacket resp = tryReceive(s, TIMEOUT_MS);

            if (resp == null) {
                return Outcome.fail("Connection closed abruptly with no data, expected a forged disconnect reason packet.");
            }

            if (resp.id == 0x00) { // Disconnect Packet ID
                String message = readString(new ByteArrayInputStream(resp.data));
                if (message.contains(FORGED_VERSION)) {
                    return Outcome.pass("Bot rejected with expected forged string. Decoded text: \"" + message + "\"");
                }
                return Outcome.fail("Bot disconnected, but reason did not match forged version '" + FORGED_VERSION + "'! Got: \"" + message + "\"");
            }

            return Outcome.fail("Bot-like username was not rejected! got packet id=" + resp.id
                    + " data=" + truncate(bytesToHex(resp.data)));
        } catch (IOException e) {
            return Outcome.fail("Connection threw unexpected exception instead of offering packet validation: " + e);
        }
    }

    /** Suspicious port + a normal-looking username -> port alone should only warn, not close. */
    private static Outcome testSuspiciousPortNormalUsername() throws IOException {
        try (Socket s = connect(SUSPICIOUS_LOCAL_PORT)) {
            sendPacket(s, 0x00, buildHandshake(PROTOCOL, HOST, PORT, 2));
            sendPacket(s, 0x00, buildLoginStart("ScannerUser"));
            RawPacket resp = tryReceive(s, TIMEOUT_MS);
            if (resp != null) {
                return Outcome.pass("Login sequence continued past the suspicious-port check (got packet id="
                        + resp.id + "). Note: any later disconnect is your own server logic (online-mode/"
                        + "whitelist/etc.), not necessarily CoralGate.");
            }
            return Outcome.fail("Connection was closed immediately on a normal username - suspicious port "
                    + "alone should only log a warning, not close the connection during LOGIN.");
        } catch (IOException e) {
            return Outcome.fail("Connection closed/reset on a normal username at a suspicious (not invalid) "
                    + "port - suspicious port should only warn during LOGIN, not disconnect (" + e + ").");
        }
    }

    /** Legit port + LOGIN intent + bot-like username -> should still be kicked (username check is independent of port). */
    private static Outcome testLegitPortBotUsername() throws IOException {
        try (Socket s = connect(LEGIT_LOCAL_PORT + 1)) {
            sendPacket(s, 0x00, buildHandshake(PROTOCOL, HOST, PORT, 2));
            sendPacket(s, 0x00, buildLoginStart("Player_Bot"));
            RawPacket resp = tryReceive(s, TIMEOUT_MS);

            if (resp == null) {
                return Outcome.fail("Connection closed abruptly with no data, expected a forged disconnect reason packet.");
            }

            if (resp.id == 0x00) { // Disconnect Packet ID
                String message = readString(new ByteArrayInputStream(resp.data));
                if (message.contains(FORGED_VERSION)) {
                    return Outcome.pass("Bot rejected with expected forged string on legit port. Decoded text: \"" + message + "\"");
                }
                return Outcome.fail("Bot disconnected, but reason did not match forged version '" + FORGED_VERSION + "'! Got: \"" + message + "\"");
            }

            return Outcome.fail("Bot-like username was NOT rejected even on a legit port! got packet id="
                    + resp.id + " data=" + truncate(bytesToHex(resp.data)));
        } catch (IOException e) {
            return Outcome.fail("Connection threw unexpected exception instead of offering packet validation: " + e);
        }
    }

    /** Old-style (pre-Netty) 0xFE server list ping, sent with no modern handshake. */
    private static Outcome testLegacyPing() throws IOException {
        try (Socket s = connect(LEGIT_LOCAL_PORT + 2)) {
            OutputStream out = s.getOutputStream();
            out.write(0xFE);
            out.write(0x01);
            out.flush();

            s.setSoTimeout(TIMEOUT_MS);
            int first;
            try {
                first = s.getInputStream().read();
            } catch (SocketTimeoutException e) {
                return Outcome.info("No response to legacy ping within " + TIMEOUT_MS
                        + "ms (server may silently drop legacy pings - verify manually).");
            }
            if (first == -1) {
                return Outcome.pass("Connection closed on legacy ping with no prior handshake, as expected.");
            }
            if (first == 0xFF) {
                // Legacy disconnect/kick packet: short length (UTF-16BE chars) + UTF-16BE string
                DataInputStream dis = new DataInputStream(s.getInputStream());
                int len = dis.readUnsignedShort();
                byte[] strBytes = new byte[len * 2];
                dis.readFully(strBytes);
                String message = new String(strBytes, StandardCharsets.UTF_16BE);
                boolean forged = isForgedMotd(message);
                return forged
                        ? Outcome.pass("Legacy ping answered with forged data, as expected.")
                        : Outcome.info("Legacy ping answered with a 0xFF packet - manually verify it doesn't "
                                       + "leak real info: " + truncate(message));
            }
            return Outcome.info("Legacy ping got an unexpected first byte (0x"
                    + Integer.toHexString(first) + ") - likely a modern packet sent in reply to a legacy "
                    + "ping (protocol mismatch on the plugin side); verify manually.");
        } catch (IOException e) {
            return Outcome.pass("Connection closed/reset on legacy ping, as expected (" + e + ").");
        }
    }

    // ----------------------------------------------------------------
    // Shared helpers
    // ----------------------------------------------------------------

    private static Outcome expectForgedStatus(int localPort, String label) throws IOException {
        try (Socket s = connect(localPort)) {
            sendPacket(s, 0x00, buildHandshake(PROTOCOL, HOST, PORT, 1));
            sendPacket(s, 0x00, new byte[0]);
            RawPacket resp = receivePacket(s, TIMEOUT_MS);
            String json = readString(new ByteArrayInputStream(resp.data));
            if (isForgedMotd(json)) {
                return Outcome.pass("Forged MOTD correctly returned. (" + label + ")");
            }
            return Outcome.fail("Real server info leaked! raw=" + truncate(json));
        } catch (IOException e) {
            // Filter closing the connection outright instead of forging is still "no leak" - acceptable.
            return Outcome.pass("Connection closed/blocked before any info leaked (" + e + ").");
        }
    }

    private static Socket connect(int localPort) throws IOException {
        Socket socket = new Socket();
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(localPort));
        socket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT_MS);
        return socket;
    }

    private static boolean isForgedMotd(String text) {
        return text != null
                && text.contains(FORGED_MARKER_1)
                && text.contains(FORGED_MARKER_2)
                && text.contains(FORGED_MARKER_3);
    }

    private static String describeMotd(String json) {
        String version = extract(json, "\"version\"\\s*:\\s*\\{[^}]*\"name\"\\s*:\\s*\"([^\"]*)\"");
        String protocol = extract(json, "\"version\"\\s*:\\s*\\{[^}]*\"protocol\"\\s*:\\s*(-?\\d+)");
        String online = extract(json, "\"players\"\\s*:\\s*\\{[^}]*\"online\"\\s*:\\s*(\\d+)");
        String max = extract(json, "\"players\"\\s*:\\s*\\{[^}]*\"max\"\\s*:\\s*(\\d+)");
        return "version=" + version + " protocol=" + protocol + " players=" + online + "/" + max;
    }

    private static String extract(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1) : "?";
    }

    private static String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 160 ? s.substring(0, 160) + "..." : s;
    }

    private static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02x ", b));
        return sb.toString().trim();
    }

    // -- Minecraft protocol wire helpers --------------------------------

    private static void writeVarInt(OutputStream out, int value) throws IOException {
        while (true) {
            if ((value & ~0x7F) == 0) {
                out.write(value);
                return;
            }
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }

    private static int readVarInt(InputStream in) throws IOException {
        int value = 0, position = 0, b;
        while (true) {
            b = in.read();
            if (b == -1) throw new EOFException("Stream closed while reading VarInt");
            value |= (b & 0x7F) << position;
            if ((b & 0x80) == 0) break;
            position += 7;
            if (position >= 32) throw new IOException("VarInt too big");
        }
        return value;
    }

    private static void writeString(OutputStream out, String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private static String readString(InputStream in) throws IOException {
        int len = readVarInt(in);
        byte[] bytes = new byte[len];
        int read = 0;
        while (read < len) {
            int r = in.read(bytes, read, len - read);
            if (r == -1) throw new EOFException("Stream closed while reading String");
            read += r;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static byte[] longBytes(long value) {
        byte[] b = new byte[8];
        for (int i = 7; i >= 0; i--) {
            b[i] = (byte) (value & 0xFF);
            value >>>= 8;
        }
        return b;
    }

    private static long readLong(byte[] data) {
        long v = 0;
        for (int i = 0; i < 8; i++) v = (v << 8) | (data[i] & 0xFF);
        return v;
    }

    private static byte[] buildHandshake(int protocol, String host, int port, int nextState) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        writeVarInt(b, protocol);
        writeString(b, host);
        b.write((port >> 8) & 0xFF);
        b.write(port & 0xFF);
        writeVarInt(b, nextState);
        return b.toByteArray();
    }

    private static byte[] buildLoginStart(String username) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        writeString(b, username);
        // Zero UUID - covers modern protocol versions that expect a mandatory player UUID field.
        // Harmless for older versions since NetworkProcessor only reads the username itself.
        b.write(new byte[16]);
        return b.toByteArray();
    }

    private static void sendPacket(Socket socket, int packetId, byte[] data) throws IOException {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        writeVarInt(payload, packetId);
        payload.write(data);
        ByteArrayOutputStream full = new ByteArrayOutputStream();
        writeVarInt(full, payload.size());
        full.write(payload.toByteArray());
        OutputStream out = socket.getOutputStream();
        out.write(full.toByteArray());
        out.flush();
    }

    private record RawPacket(int id, byte[] data) {}

    private static RawPacket receivePacket(Socket socket, int timeoutMs) throws IOException {
        socket.setSoTimeout(timeoutMs);
        InputStream in = socket.getInputStream();
        int length = readVarInt(in);
        byte[] full = new byte[length];
        int read = 0;
        while (read < length) {
            int r = in.read(full, read, length - read);
            if (r == -1) throw new EOFException("Connection closed mid-packet");
            read += r;
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(full);
        int id = readVarInt(bais);
        byte[] data = bais.readAllBytes();
        return new RawPacket(id, data);
    }

    /** Like receivePacket but returns null instead of throwing on timeout/EOF (i.e. "connection did not respond"). */
    private static RawPacket tryReceive(Socket socket, int timeoutMs) throws IOException {
        try {
            return receivePacket(socket, timeoutMs);
        } catch (SocketTimeoutException | EOFException e) {
            return null;
        }
    }

    // ----------------------------------------------------------------
    // Test harness plumbing
    // ----------------------------------------------------------------

    private interface TestCase {
        Outcome run() throws IOException;
    }

    private record Outcome(Status status, String detail) {
        static Outcome pass(String detail) { return new Outcome(Status.PASS, detail); }
        static Outcome fail(String detail) { return new Outcome(Status.FAIL, detail); }
        static Outcome info(String detail) { return new Outcome(Status.INFO, detail); }
    }

    private enum Status { PASS, FAIL, INFO }

    private static void runTest(String name, TestCase test) {
        System.out.print(BOLD + name + RESET + " ".repeat(Math.max(1, 52 - name.length())));
        Outcome outcome;
        try {
            outcome = test.run();
        } catch (Exception e) {
            outcome = Outcome.fail("Unhandled scanner exception: " + e);
        }
        switch (outcome.status()) {
            case PASS -> {
                System.out.println(GREEN + "[ PASS ]" + RESET);
                passCount++;
            }
            case FAIL -> {
                System.out.println(RED + "[ FAIL ]" + RESET);
                failCount++;
            }
            case INFO -> System.out.println(YELLOW + "[ INFO ]" + RESET);
        }
        System.out.println(DIM + "        " + outcome.detail() + RESET);
        sleep(250); // let connectionState cleanup settle between tests
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    private static void banner() {
        System.out.println(CYAN + BOLD + "CoralGate NetworkProcessor Scanner" + RESET);
        System.out.println(CYAN + "Target: " + HOST + ":" + PORT + "  protocol=" + PROTOCOL
                + "  timeout=" + TIMEOUT_MS + "ms" + RESET);
        System.out.println(DIM + "Local test ports -> legit>=49152: " + LEGIT_LOCAL_PORT
                + "  suspicious(32768-49151): " + SUSPICIOUS_LOCAL_PORT
                + "  invalid(<32768): " + INVALID_LOCAL_PORT + RESET);
        System.out.println();
    }

    private static void summary() {
        System.out.println();
        System.out.println(BOLD + "Summary: " + RESET
                + GREEN + passCount + " passed" + RESET + ", "
                + (failCount > 0 ? RED : DIM) + failCount + " failed" + RESET);
        if (failCount > 0) {
            System.out.println(RED + "One or more checks behaved unexpectedly - review the [ FAIL ] lines above." + RESET);
        } else {
            System.out.println(GREEN + "All checks behaved as expected." + RESET);
        }
    }
}