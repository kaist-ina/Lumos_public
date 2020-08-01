# Lumos

This is a public repository for Lumos that is a system to support interoperation among IoT devices which are not co-operable.  Its goal is to provide researchers and practitioners with a tool and library on which they can base their own research projects. Our tool has many program analysis techniques such as static taint analysis, dynamic UI analysis, and network message learning. We hope this research contribute to another research.

Lumos has three components: 1) application analysis module, Lumos-gateway, and Lumos-app. Thus, you can access to those comonents in this repository. Now, I will explain the requirements of Lumos and how to use it.

## Demo video
Our demo video presents an interoperation scenario with Lumos-app, which is to turn off a bulb in the living room when playing a Netflix movie on the TV connected to Chromecast.
URL: <https://youtu.be/enwQwZV0fJE>

## Lumos-app

Lumos-app is built on the top of SUGILITE that is a tasking automation tool (<https://github.com/tobyli/Sugilite_development>).

- Minimum requirements: Android 6.0.1 (Marshmallow)
- Used IDE: Android studio 3.1.

You just install Lumos-app if you want to use it. The path is "Lumos-app\APK\Lumos.apk".
And we also uploaded the source-code on "Lumos-app\".

## Lumos application analysis module

We implement this module using a program analyis framework, called Extractocol, that extracts network messages and infers dependency information between two messages by analyzning Android Application binary. In addition, we extend the framework to identify pairs that consist of specific UI control and corresponding network message. This information is used in Lumos-gateway.

- Path: "Lumos-app-analysis"
- Minimum requirements
  - JDK 1.8.1 version  
- Used IDE: Intellij Idea, any JAVA development tools (Eclipse)

### How to run application analysis module

- How to open the project using Intellij Idea
  - File -> Open -> "extractocol_private\src"
  - You should set JDK path (File -> Project Structure -> Modules tap -> Module SDK path)

- To extract network signatures
  - Main class: extractocol.tester.Extractocol.main
  - Program Argument: [APK name] --maxms 2 -- maxdepth 3
  - maxms and maxdepth are to avoid overtaint problem. (recommended)
  
```Java
    SmartThings --maxms 2 -- maxdepth 3
```

- To extract UI-signature pair after signature building
  - Main class: extractocol.tester.Extractocol.main
  - Program Argument: [APK name] --maxms 2 -- maxdepth 3 --UI
  - maxms and maxdepth are to avoid overtaint problem. (recommended)

```Java
    SmartThings --maxms 2 -- maxdepth 3 --UI
```

## Lumos-gateway

Lumos-gateway is extended from mitmproxy that is a free and open source interactive HTTPS proxy. It takes the results of our application analysis moudle (static network signature-UI pairs), and outputs then Learning instances that a user use when she makes own interoperation.

- Minimum requirements
  - Ubuntu 14.04 (but, we tested and implemented it on Ubuntu 16.04)
- Used IDE: pycharm
- Path: "Lumos-gateway"
- If you want to know detail information about mitmproxy, please see this site (<http://https://mitmproxy.org/>)
  
## Frida script for certificate replacing

As mentioned in our paper, we replace a certificate with Lumos enabled certificate that is used between Lumos-gateway and Lumos-app.
To use our scripts, you should install Frida server and Frida client. We will serve an automatic shell script to use Frida scripts soon. Now,you manually install and use it. We proviedes three Frida scripts for SmartThings, August, and Wink app.

### How to use Frida

- Quick start Frida: <https://frida.re/docs/quickstart/>
- A tutorial on Android: <https://frida.re/docs/android/>
- Frida script path: "Lumos-Frida-Script"
- **Frida requires root permission**
- Example of usage
  
```bash
    adb devices
    adb root
    adb shell "/data/local/tmp/frida-server"&
    python August_hooker.py
```
