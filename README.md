[//]: # (Main image, centered)
<p align="center">
  <img width="300" src="https://github.com/GTeamX/CoralGate/blob/prod/assets/coralgate_logo.png?raw=true" alt="CoralGate Logo">
</p>

[//]: # (Main title, centered)
<h1 align="center">CoralGate</h1>

[//]: # (Shield.io badges, main basic stuff, centered)
<div align="center">

  <a href="https://github.com/GTeamX/CoralGate/releases">![GitHub Release](https://img.shields.io/github/v/release/GTeamX/CoralGate?sort=date&display_name=tag&style=for-the-badge&label=Latest%20Release&color=55FFFF)</a>
  <a href="https://github.com/GTeamX/CoralGate/releases">![GitHub Downloads (all assets, latest release)](https://img.shields.io/github/downloads/GTeamX/CoralGate/latest/total?sort=date&style=for-the-badge&label=Latest%20Downloads)</a>
  <br>
  <a href="https://github.com/GTeamX/CoralGate/releases">![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/GTeamX/CoralGate/total?style=for-the-badge&label=Total%20Downloads)</a>
  <a href="https://github.com/GTeamX/CoralGate/blob/prod/LICENSE">![GitHub License](https://img.shields.io/github/license/GTeamX/CoralGate?style=for-the-badge)</a>
  <br>
  <a href="https://github.com/GTeamX/CoralGate/actions">![GitHub branch check runs](https://img.shields.io/github/check-runs/GTeamX/CoralGate/prod?style=for-the-badge&label='prod'%20branch%20checks)</a>
  <a href="https://github.com/GTeamX/CoralGate/actions">![GitHub branch check runs](https://img.shields.io/github/check-runs/GTeamX/CoralGate/dev?style=for-the-badge&label='dev'%20branch%20checks)</a>
  <br>
  <a href="https://github.com/GTeamX/CoralGate/stargazers">![GitHub Repo stars](https://img.shields.io/github/stars/GTeamX/CoralGate?style=for-the-badge)</a>
  <a href="https://github.com/GTeamX/CoralGate/watchers">![GitHub watchers](https://img.shields.io/github/watchers/GTeamX/CoralGate?style=for-the-badge)</a>
  <a href="https://github.com/GTeamX/CoralGate/forks">![GitHub forks](https://img.shields.io/github/forks/GTeamX/CoralGate?style=for-the-badge)</a>
  <a href="https://discord.gteam.cloud">![Discord](https://img.shields.io/discord/1046001788106575912?style=for-the-badge&label=Discord)</a>

</div>

On-the-fly packet inspection and real-time IP verification for Minecraft servers and networks.

CoralGate acts as a high-performance application firewall for your Minecraft infrastructure. By analyzing incoming packets: malicious payloads, scanners and bots are blocked before they truly reach your server. It secures your servers by spoofing your MOTD to appear as a generic server, verifies proper packet order and even checks certain incoming connection fields.

If you wish to get support, test or have any questions about CoralGate, make sure to join our [Discord server](https://discord.gteam.cloud)!

## Supported Platforms

CoralGate supports a wide range of Minecraft server implementations.

| Platform       | 1.8.x | 1.9.x – 1.15.x | 1.16.x – 1.20.x | 1.21.x | 26.x | &gt; 26.2 |
|----------------|:-----:|:--------------:|:---------------:|:------:|:----:|:---------:|
| **Spigot**     |  ⚠️   |       ✅        |        ✅        |   ✅    |  ✅   |     ❓     |
| **Paper**      |   ✅   |       ✅        |       ⚠️        |   ✅    |  ✅   |     ❓     |
| **BungeeCord** |   ✅   |       ✅        |        ✅        |   ✅    |  ✅   |     ❓     |
| **Velocity**   |   ❓   |       ❓        |        ❓        |   ❓    |  ❓   |     ❓     |
| **Sponge**     |   ❌   |       ❌        |        ❌        |   ❌    |  ❌   |     ❌     |
| **Fabric**     |   ❌   |       ❌        |        ❌        |   ❌    |  ❌   |     ❌     |
| **Forge**      |   ❌   |       ❌        |        ❌        |   ❌    |  ❌   |     ❌     |
| **NeoForge**   |   ❌   |       ❌        |        ❌        |   ❌    |  ❌   |     ❌     |

<details>
<summary><i>View platform compatibility notes.</i></summary>

**Spigot**:
- **< 1.8.3:** not supported.
- **1.8.3:** works with an outdated packetevents version (2.10.1).
- **1.19.3:** not supported (broken server jar).

**Paper**:
- **1.19.3:** not supported (broken server jar).
- **1.19.4 - 1.20.x:** not supported (broken modern 'paper-plugin.yml').

*Sponge, Fabric, Forge, NeoForge and any hybrid server platform is not officially supported.*
</details>

<details>
<summary><i>View fully detailed platform and version support list.</i></summary>

| Platform       | <1.8 | 1.8 | 1.8.3 | 1.8.8 | 1.9 | 1.9.1 | 1.9.2 | 1.9.3/4 | 1.10.x | 1.11 | 1.11.x | 1.12 | 1.12.1 | 1.12.2 | 1.13 | 1.13.1 | 1.13.2 | 1.14 | 1.14.1 | 1.14.2 | 1.14.3 | 1.14.4 | 1.15 | 1.15.1 | 1.15.2 | 1.16 | 1.16.1 | 1.16.2 | 1.16.3 | 1.16.4/5 | 1.17 | 1.17.1 | 1.18 | 1.18.1 | 1.18.2 | 1.19 | 1.19.1/2 | 1.19.3 | 1.19.4 | 1.20/.1 | 1.20.2 | 1.20.3/4 | 1.20.5/6 | 1.21/.1 | 1.21.2/3 | 1.21.4 | 1.21.5 | 1.21.6 | 1.21.7/8 | 1.21.9/10 | 1.21.11 | 26.1.x | 26.2 | &gt;26.2 |
|:---------------|:----:|:---:|:-----:|:-----:|:---:|:-----:|:-----:|:-------:|:------:|:----:|:------:|:----:|:------:|:------:|:----:|:------:|:------:|:----:|:------:|:------:|:------:|:------:|:----:|:------:|:------:|:----:|:------:|:------:|:------:|:--------:|:----:|:------:|:----:|:------:|:------:|:----:|:--------:|:------:|:------:|:-------:|:------:|:--------:|:--------:|:-------:|:--------:|:------:|:------:|:------:|:--------:|:---------:|:-------:|:------:|:----:|:--------:|
| **Spigot**     |  ❌   |  ❌  |  ⚠️   |   ✅   |  ✅  |   ❌   |   ✅   |    ✅    |   ✅    |  ✅   |   ✅    |  ✅   |   ✅    |   ✅    |  ✅   |   ✅    |   ✅    |  ✅   |   ✅    |   ✅    |   ✅    |   ✅    |  ✅   |   ✅    |   ✅    |  ❌   |   ✅    |   ✅    |   ✅    |    ✅     |  ✅   |   ✅    |  ✅   |   ✅    |   ✅    |  ✅   |    ✅     |   ❌    |   ✅    |    ✅    |   ✅    |    ✅     |    ✅     |    ✅    |    ✅     |   ✅    |   ✅    |   ✅    |    ✅     |     ✅     |    ✅    |   ✅    |  ✅   |    ❓     |
| **Paper**      |  ❌   |  ❌  |   ❌   |   ✅   |  ❌  |   ❌   |   ❌   |    ✅    |   ✅    |  ❌   |   ✅    |  ❌   |   ❌    |   ✅    |  ✅   |   ✅    |   ✅    |  ✅   |   ✅    |   ✅    |   ✅    |   ✅    |  ✅   |   ✅    |   ✅    |  ❌   |   ✅    |   ✅    |   ✅    |    ✅     |  ✅   |   ✅    |  ❌   |   ✅    |   ✅    |  ✅   |    ✅     |   ❌    |   ❌    |    ❌    |   ❌    |    ❌     |    ❌     |    ✅    |    ✅     |   ✅    |   ✅    |   ✅    |    ✅     |     ✅     |    ✅    |   ✅    |  ✅   |    ❓     |
| **BungeeCord** |  ❌   |  ✅  |   ✅   |   ✅   |  ✅  |   ❌   |   ✅   |    ✅    |   ✅    |  ✅   |   ✅    |  ✅   |   ✅    |   ✅    |  ✅   |   ✅    |   ✅    |  ✅   |   ✅    |   ✅    |   ✅    |   ✅    |  ✅   |   ✅    |   ✅    |  ❌   |   ✅    |   ✅    |   ✅    |    ✅     |  ✅   |   ✅    |  ✅   |   ✅    |   ✅    |  ✅   |    ✅     |   ✅    |   ✅    |    ✅    |   ❌    |    ✅     |    ✅     |    ✅    |    ✅     |   ✅    |   ✅    |   ✅    |    ✅     |     ✅     |    ✅    |   ✅    |  ✅   |    ❓     |
| **Velocity**   |  ❌   |  ❓  |   ❓   |   ❓   |  ❓  |   ❓   |   ❓   |    ❓    |   ❓    |  ❓   |   ❓    |  ❓   |   ❓    |   ❓    |  ❓   |   ❓    |   ❓    |  ❓   |   ❓    |   ❓    |   ❓    |   ❓    |  ❓   |   ❓    |   ❓    |  ❓   |   ❓    |   ❓    |   ❓    |    ❓     |  ❓   |   ❓    |  ❓   |   ❓    |   ❓    |  ❓   |    ❓     |   ❓    |   ❓    |    ❓    |   ❓    |    ❓     |    ❓     |    ❓    |    ❓     |   ❓    |   ❓    |   ❓    |    ❓     |     ❓     |    ❓    |   ❓    |  ❓   |    ❓     |
| **Sponge**     |  ❌   |  ❌  |   ❌   |   ❌   |  ❌  |   ❌   |   ❌   |    ❌    |   ❌    |  ❌   |   ❌    |  ❌   |   ❌    |   ❌    |  ❌   |   ❌    |   ❌    |  ❌   |   ❌    |   ❌    |   ❌    |   ❌    |  ❌   |   ❌    |   ❌    |  ❌   |   ❌    |   ❌    |   ❌    |    ❌     |  ❌   |   ❌    |  ❌   |   ❌    |   ❌    |  ❌   |    ❌     |   ❌    |   ❌    |    ❌    |   ❌    |    ❌     |    ❌     |    ❌    |    ❌     |   ❌    |   ❌    |   ❌    |    ❌     |     ❌     |    ❌    |   ❌    |  ❌   |    ❌     |
| **Fabric**     |  ❌   |  ❌  |   ❌   |   ❌   |  ❌  |   ❌   |   ❌   |    ❌    |   ❌    |  ❌   |   ❌    |  ❌   |   ❌    |   ❌    |  ❌   |   ❌    |   ❌    |  ❌   |   ❌    |   ❌    |   ❌    |   ❌    |  ❌   |   ❌    |   ❌    |  ❌   |   ❌    |   ❌    |   ❌    |    ❌     |  ❌   |   ❌    |  ❌   |   ❌    |   ❌    |  ❌   |    ❌     |   ❌    |   ❌    |    ❌    |   ❌    |    ❌     |    ❌     |    ❌    |    ❌     |   ❌    |   ❌    |   ❌    |    ❌     |     ❌     |    ❌    |   ❌    |  ❌   |    ❌     |
| **Forge**      |  ❌   |  ❌  |   ❌   |   ❌   |  ❌  |   ❌   |   ❌   |    ❌    |   ❌    |  ❌   |   ❌    |  ❌   |   ❌    |   ❌    |  ❌   |   ❌    |   ❌    |  ❌   |   ❌    |   ❌    |   ❌    |   ❌    |  ❌   |   ❌    |   ❌    |  ❌   |   ❌    |   ❌    |   ❌    |    ❌     |  ❌   |   ❌    |  ❌   |   ❌    |   ❌    |  ❌   |    ❌     |   ❌    |   ❌    |    ❌    |   ❌    |    ❌     |    ❌     |    ❌    |    ❌     |   ❌    |   ❌    |   ❌    |    ❌     |     ❌     |    ❌    |   ❌    |  ❌   |    ❌     |
| **NeoForge**   |  ❌   |  ❌  |   ❌   |   ❌   |  ❌  |   ❌   |   ❌   |    ❌    |   ❌    |  ❌   |   ❌    |  ❌   |   ❌    |   ❌    |  ❌   |   ❌    |   ❌    |  ❌   |   ❌    |   ❌    |   ❌    |   ❌    |  ❌   |   ❌    |   ❌    |  ❌   |   ❌    |   ❌    |   ❌    |    ❌     |  ❌   |   ❌    |  ❌   |   ❌    |   ❌    |  ❌   |    ❌     |   ❌    |   ❌    |    ❌    |   ❌    |    ❌     |    ❌     |    ❌    |    ❌     |   ❌    |   ❌    |   ❌    |    ❌     |     ❌     |    ❌    |   ❌    |  ❌   |    ❌     |

</details>

## Installation

This installation procedure is platform independent, it is the same for every supported platform above:
1. Download the latest version of [packetevents](https://github.com/retrooper/packetevents/releases).
2. Download the latest version of CoralGate for your platform *(see download mirrors below)*.
3. Put both plugins in your "plugins" folder situated at the root of your server jar.
4. Restart your server.

CoralGate is now protecting your server from scanners!

## Download Mirrors

To prevent any malware from infecting your server, only download CoralGate from our **trusted sources**:
- [Official GitHub](https://github.com/GTeamX/CoralGate/releases)

## Configuration

CoralGate has a very extensive and complete configuration file, situated in your plugins folder under the CoralGate folder. The file is named `config.yml`.
Every default value is marked in the comments above the field with explanation on its impact.

If you ever mess up your configuration file, delete it and restart your server. The default configuration will be loaded.

## API

CoralGate utilizes a proprietary, custom-built API hosted in Germany (fully GDPR-compliant) to determine IP reputation. The API is free, requires no authentication keys, and supports both IPv4 and IPv6.

You can manually query the API using the following structure:
`https://api.gteam.cloud/coralgate/v2/<ip_address>`

**Examples:**
- `https://api.gteam.cloud/coralgate/v2/185.65.134.164` *(Returns malicious)*
- `https://api.gteam.cloud/coralgate/v2/9.9.9.9` *(Returns safe)*

### False Positives

If your ISP, domain name, or personal IP is falsely flagged and blocked from CoralGate-protected servers, please open a ticket on our [Discord](https://discord.gteam.cloud). Provide the affected IP addresses and their primary use case, and our support team will assist with whitelisting.

**Traffic routinely blocked by the API:**
Port/IP scanners, crawlers, MOTD/player-count fetchers, VPNs, proxies, TOR exit nodes, and automated hosting services (e.g., Shodan, OpenHeimer).

**Traffic exempt from blocking:**
Known voting sites and verified server lists.

### Privacy & Disclosure

By default, CoralGate automatically verifies the reputation of connecting players to block malicious traffic.

* **Data Transmitted:** When a player attempts to join, their IP address is sent via a secure GET request to our proprietary API.
* **Endpoint:** `https://api.gteam.cloud/coralgate/v2/<ip_address>`
* **Privacy:** The API is hosted in Germany (fully GDPR-compliant). No personally identifiable information (PII) beyond the IP is processed, and data is used strictly for real-time risk assessment.
* **Opt-Out:** You can entirely disable this external API lookup or route requests through your own custom endpoint by modifying the `config.yml` file.

## Contributing & Support

**Issues & Feature Requests:**
Please utilize the provided GitHub templates when opening an issue or requesting a feature. If your ticket requires urgent attention, you may reference it in our Discord support channels.

**Contributing:**
We welcome pull requests. Ensure your code follows our existing naming conventions and indentation standards. When submitting a PR, detail the platform, exact version, and any other relevant plugins used during your testing.

## Credits & License

CoralGate is built on top of these incredible open-source projects:

* [packetevents](https://github.com/retrooper/packetevents) by [retrooper](https://github.com/retrooper) - Powers our core packet analysis.
* [Lamp](https://github.com/Revxrsal/lamp) by [Revxrsal](https://github.com/Revxrsal) - Handles our cross-platform command ecosystem.
* [bStats](https://github.com/Bastian/bStats) by [Bastian Oppermann](https://github.com/Bastian) - Provides anonymous usage metrics.
* [Jankson](https://github.com/falkreon/Jankson) by [Falkreon](https://github.com/falkreon) - Parses our API JSON results.
* [async-http-client](https://github.com/AsyncHttpClient/async-http-client) by [Aayush Atharva](https://github.com/hyperxpro) - Powers our network API queries and update checks.
* [boosted-yaml](https://github.com/dejvokep/boosted-yaml) by [dejvokep](https://github.com/dejvokep) - Drives our internal configuration engine.

This project is licensed under the [GNU General Public License v3.0 (GPL)](LICENSE).
