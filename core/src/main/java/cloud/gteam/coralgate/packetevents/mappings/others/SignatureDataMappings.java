/*
 * This file is part of CoralGate - https://github.com/GTeamX/CoralGate
 * Copyright (C) 2025 GTeamX (GTeam) and it's contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cloud.gteam.coralgate.packetevents.mappings.others;

import java.security.PublicKey;
import java.time.Instant;

public class SignatureDataMappings {

    private final Instant timestamp;
    private final PublicKey publicKey;
    private final byte[] signature;

    public SignatureDataMappings(Instant timestamp, PublicKey publicKey, byte[] signature) {
        this.timestamp = timestamp;
        this.publicKey = publicKey;
        this.signature = signature;
    }

    public SignatureDataMappings(Instant timestamp, byte[] encodedPublicKey, byte[] signature) {
        this.timestamp = timestamp;
        this.publicKey = MinecraftEncryptionUtilMappings.publicKey(encodedPublicKey);
        this.signature = signature;
    }

    public Instant getTimestamp() {
        return this.timestamp;
    }

    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    public byte[] getSignature() {
        return this.signature;
    }

}
