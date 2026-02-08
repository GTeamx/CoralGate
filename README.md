[//]: # (Main image, centered)
<p align="center">
  <img width="300" src="https://github.com/GTeamx/CoralGate/blob/prod/assets/coralgate_logo.png?raw=true">
</p>

[//]: # (Main title, centered)
<h1 align="center">CoralGate</h1>

[//]: # (Shield.io badges, main basic stuff, centered)
<div align="center">

  <a href="">![GitHub Release](https://img.shields.io/github/v/release/GTeamX/CoralGate?sort=date&display_name=tag&style=for-the-badge&label=Latest%20Release&color=55FFFF)</a>
  <a href="">![GitHub Downloads (all assets, latest release)](https://img.shields.io/github/downloads/GTeamX/CoralGate/latest/total?sort=date&style=for-the-badge&label=Latest%20Downloads)</a>
  <a href="">![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/GTeamX/CoralGate/total?style=for-the-badge&label=Total%20Downloads)</a>
  <a href="">![GitHub License](https://img.shields.io/github/license/GTeamX/CoralGate?style=for-the-badge)</a>
  <br>
  <a href="">![GitHub commits since latest release](https://img.shields.io/github/commits-since/GTeamX/CoralGate/latest?sort=date&style=for-the-badge&label=commits%20since%20release)</a>
  <a href="">![GitHub commit activity (branch)](https://img.shields.io/github/commit-activity/m/GTeamX/CoralGate/dev?style=for-the-badge&label='dev'%20branch%20commits)</a>
  <br>
  <a href="">![GitHub branch check runs](https://img.shields.io/github/check-runs/GTeamX/CoralGate/prod?style=for-the-badge&label='prod'%20branch%20checks)</a>
  <a href="">![GitHub branch check runs](https://img.shields.io/github/check-runs/GTeamX/CoralGate/dev?style=for-the-badge&label='dev'%20branch%20checks)</a>
  <br>
  <a href="">![GitHub Repo stars](https://img.shields.io/github/stars/GTeamX/CoralGate?style=for-the-badge)</a>
  <a href="">![GitHub watchers](https://img.shields.io/github/watchers/GTeamX/CoralGate?style=for-the-badge)</a>
  <a href="">![GitHub forks](https://img.shields.io/github/forks/GTeamX/CoralGate?style=for-the-badge)</a>
  <a href="">![Discord](https://img.shields.io/discord/1046001788106575912?style=for-the-badge&label=Discord)</a>

</div>

CoralGate is a simple plugin to prevent server scanners from reaching your server.

If you wish to get support, test or have any questions about CoralGate, make sure to join our [Discord server](https://discord.gg/rxV89DZHEd)!

## ✅ Supported platforms/versions

| Platform          | Version             | Supported?  |
| :---------------- | :------------------ | :---------: |
| Spigot            | 1.8 - 1.21.11       | ✅         |
| Paper             | 1.8 - 1.21.11       | ✅         |
| BungeeCord        | 1.8 - 1.21.11       | ⚠️         |
| Velocity          | 1.8 - 1.21.11       | ⚠️         |
| Sponge            | Any                 | ❌         |
| Fabric            | Any                 | ❌         |
| Forge             | Any                 | ❌         |
| NeoForge          | Any                 | ❌         |

*Legend:*
- ✅: *Fully supported.*
- ⚠️: *Partially supported (missing features, incomplete or not fully tested...).*
- ❌: *Not supported.*

## ⬇️ Installation

This installation procedure is platform independant, it is the same for every supported platform above:
- Download the latest version of [packetevents](https://github.com/retrooper/packetevents/releases).
- Download the latest version of CoralGate for your platform *(see download mirrors bellow)*.
- Put both plugins in your "plugins" folder situated at the root of your server jar.
- Restart your server.

CoralGate is now protecting your server from scanners!

## 🔄 Updates

Updates are released on a non-fixed schedule. Therefore update may appear at any given time, whereas to fix issues or add new features.
The update process is the same as the installation process. To update your configuration file please take a look bellow for further instructions.

## ✏️ Configuration file

CoralGate has a very extensive and complete configuration file, situated in your plugins folder under the CoralGate folder. The file is named 'config.yml'.
Every default value is marked in the comments above the field with explanation on it's impact.

If you ever mess up your configuration file, delete it and restart your server. The default configuration will be loaded.

If you recently updated CoralGate and get a warning saying your configuration file is outdated, either use the command /coralgate cfg update, this will try to keep your active configuration and add the missing configuration parts OR delete your configuration file and let the new default configuration appear.

## 🔔 Releases

CoralGate has a built-in update checker, however it will not automatically install nor download the update, it will only send a message telling you if you are up to date or not.
To prevent any malware from infecting your server, only download CoralGate from our **trusted sources**:
- [Official GitHub](https://github.com/GTeamx/CoralGate/releases)

Feel free to compile CoralGate yourself, however note that doing so will prevent you from getting support. If you want features or fixes to be added to CoralGate, please follow the [Issues/Feature request](https://github.com/GTeamx/CoralGate?tab=readme-ov-file#-issuesfeature-request) section.

## 🚧 API

CoralGate uses it's own API to determine if an IP is malicious or not.

The API is hosted in Germarny and all data is processed within this country. This API doesn't require any key or subscription to be used. You can use it for free manually too.

To use the API manually, use the base URL: https://api.gteam.cloud/coralgate/v2/enter-ip-here

Simply replace "enter-ip-here" by the IP you wish to verify.<br>
E.g.: https://api.gteam.cloud/coralgate/v2/185.65.134.164 *(This is a known malicious ip address)*<br>
E.g.: https://api.gteam.cloud/coralgate/v2/9.9.9.9 *(This is not a malicious ip address)*<br>

The API supports both IPv4 and IPv6 and has currently **2** major versions:
- v2 *(latest)*
- v1

## 🚷 API false positives

If your API, ISP, domain name or holders get blocked by our API and therefore from the CoralGate powered servers, contact us on Discord via our ticket system.<br>

Provide the impacted ip address(es) and further explanation on the usage behind the ip address(es). Our support team will try their best to help you and get you unblocked from CoralGate.

*Please note that we detect and block ip addresses that exerce the following behavior/service:*
- Port scanning
- IP scanning
- Crawlers
- MOTD fetchers
- Online player count fetchers/monitors
- VPN/Proxy
- TOR
- Hosting services
- Cloud providers
- Bots
- Server finders/scanners (shodan, OpenHeimer...)
....

*Some service are exempt from these rules, such as:*
- Voting sites
- Server lists

## 🎯 Issues/Feature request

Before opening an issue or feature request make sure you follow our templates.<br>
If your issue/feature request goes unotice, you can open a ticket on our Discord server or bump it.

## 🔃 Contributing

Before contributing, please make sure that you follow our conventions (naming scheme, indentation) and that your code works.<br>
Make sure to also precise on what platform and version your test was ran, with other plugins/mods installed (if applicable).

## 📎 Special Credits

Special credits to [retrooper](https://github.com/retrooper) for making [packetevents](https://github.com/retrooper/packetevents) that we are using to analyze packets!<br>
Thank you so much for your amazing work and dedication!

## 📜 License

This project is licensed under GNU General Public License v3.0 (GPL).
