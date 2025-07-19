# SkyRouteMQ

*SkyRouteMQ* is a lightweight and modular MQTT connection manager for Android, built with Kotlin.
Uses method annotation for subscribing into a topic to reduce boilerplate code.

Inspired by [EventBus](https://github.com/greenrobot/EventBus) by Greenrobot, it uses a
publish-subscribe (pub/sub) model to handle message routing through MQTT topics in a clean,
decoupled way.

This library is ideal for apps that require persistent messaging, such as IoT dashboards, real-time
status apps, or remote control panels.

## ✨ Features

- 🔁 Persistent MQTT connection via a background `Service`
- 📨 Topic-based message subscription using annotations
- 🧵 Optional thread-mode control for subscribers (`MAIN`, `BACKGROUND`, `ASYNC`)
- ⚙️ Configurable via metadata or code (XML or programmatic)
- 🔐 Supports authentication with username/password
- 🛡️ Supports security layer TLS or mTLS

## 🚀 Getting Started

1. Add `SkyRouteService` to your `AndroidManifest.xml`

    ```xml
    <manifest>
        <!-- Required Internet permissions -->
        <uses-permission android:name="android.permission.INTERNET" />

        <application>
            <service android:name="com.skyroute.service.SkyRouteService" />
        </application>
   </manifest>
    ```

2. Initialize `SkyRouteMQ` in your `Application` class

    ```kotlin
    class MainApplication : Application() {
        override fun onCreate() {
            super.onCreate()
            SkyRoute.getDefault().init(applicationContext)
        }
    }
    ```

   Or initialize with custom config:

    ```kotlin
    class MainApplication : Application() {
        
        companion object {
            @Volatile
            private var _skyRoute: SkyRoute? = null
            val skyRoute: SkyRoute
                get() = _skyRoute ?: throw IllegalStateException("SkyRouteMQ has not been initialized yet.")
        }
   
        override fun onCreate() {
            super.onCreate()
            _skyRoute = SkyRoute.newBuilder()
                .brokerUrl("tcp://your-broker-url:1883")
                .clientId("skyroute-123456")
                .build()
            skyRoute.init(applicationContext)
        }
    }
    ```

3. Define a Message Type (POJO / Data Class):

    ```kotlin
    data class TopicMessage(
        val title: String,
        val description: String,
        /* Add fields as needed */
    )
    ```

4. Create a Subscriber<br>

   Annotate methods with `@Subscribe`. `threadMode` is optional (default is `ThreadMode.MAIN`):

    ```kotlin
    @Subscribe(topic = "topic/name", threadMode = ThreadMode.BACKGROUND)
    fun subscribeToNames(message: TopicMessage) {
        // Do your stuff
        message.title // Access message fields
        message.description // Access message fields
    }
    ```

5. Register / Unregister the Subscriber<br>

   Typically inside an Activity or Fragment lifecycle:

    ```kotlin
    class MainActivity : AppCompatActivity() {
        override fun onStart() {
            super.onStart()
            SkyRoute.getDefault().register(this)
        }

        override fun onStop() {
            super.onStop()
            SkyRoute.getDefault().unregister(this)
        }
    }
    ```

6. Publish Messages

    ```kotlin
    SkyRoute.getDefault().publish("topic/name", TopicMessage("Hello!"))
    ```

---

## 🛠️ Service Metadata Configuration

You can configure SkyRouteMQ using `<meta-data>` tags inside the `AndroidManifest.xml`:

```xml

<service android:name="com.skyroute.service.SkyRouteService">
    <meta-data android:name="mqttBrokerUrl" android:value="tcp://your-broker-url:1883" />
    <meta-data android:name="keepAliveInterval" android:value="60" />
</service>
```

### ✅ Supported Metadata Options

| Key                     | Type              | Default Value | Description                                                                |
|-------------------------|-------------------|---------------|----------------------------------------------------------------------------|
| `mqttBrokerUrl`         | String (required) | N/A           | URL of the MQTT broker, including host and port.                           |
| `clientPrefix`          | String (required) | `skyroute`    | Prefix used to generate the MQTT client ID.                                |
| `cleanStart`            | Boolean           | `true`        | Whether to start a clean session. If `false`, previous session may resume. |
| `sessionExpiryInterval` | Int               | `0`           | Duration (in seconds) to keep session after disconnect.                    |
| `connectionTimeout`     | Int               | `10`          | Max time (in seconds) to wait for connection to establish.                 |
| `keepAliveInterval`     | Int               | `30`          | Interval (in seconds) for sending PING messages to keep connection alive.  |
| `autoReconnect`         | Boolean           | `true`        | Whether to auto-reconnect on connection loss.                              |
| `autoReconnectMinDelay` | Int               | `10`          | Minimum delay (in seconds) before the first reconnect attempt.             |
| `autoReconnectMaxDelay` | Int               | `60`          | Maximum delay (in seconds) between reconnect attempt.                      |
| `maxReconnectDelay`     | Int               | `120`         | Upper limit (in seconds) for total reconnect delay.                        |
| `username`              | String            | `null`        | MQTT username for authentication (optional).                               |
| `password`              | String            | `null`        | MQTT password for authentication (optional).                               |
| `caCertPath`            | String            | `null`        | URI to CA certificate file for TLS validation. Enable TLS.                 |
| `clientCertPath`        | String            | `null`        | URI to client certificate for mTLS. Requires `clientKeyPath`.              |
| `clientKeyPath`         | String            | `null`        | URI to client private key for mTLS. Requires `clientCertPath`.             |
| `clientKeyPassword`     | String            | `null`        | Password for the client private key (optional).                            |
| `insecureSkipVerify`    | Boolean           | `false`       | Whether to skip server certificate verification (not recommended).         |

---

### 🔐 TLS Mode Determination (Basedo on Metadata)

| Condition                                                           | TLS Mode            |
|---------------------------------------------------------------------|---------------------|
| `mqttBrokerUrl` starts with `tcp://`                                | `None`              |
| `mqttBrokerUrl` starts with `ssl://` and no TLS metadata provided   | `Default`           |
| `caCertPath` is set but `clientCertPath` or `clientKeyPath` are not | `ServerAuth` (TLS)  |
| `caCertPath`, `clientCertPath`, and `clientKeyPath` are all set     | `MutualAuth` (mTLS) |

---

## 📁 Example

You can find a full working example in the [example-app folder](/example-app).
It demonstrates how to:

- Initialize SkyRouteMQ in an `Application` class
- Register and unregister subscribers in an `Activity` and `ViewModel`
- Define message classes and handle subscriptions using `@Subscribe`
- Publish messages to topics

---

## 📄 License

SkyRouteMQ is licensed under the [Apache License 2.0](LICENSE).  
You are free to use, modify, and distribute this software under the terms of that license.
