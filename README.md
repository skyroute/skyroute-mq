# SkyRouteMQ

*SkyRouteMQ* is a lightweight and modular MQTT connection manager for Android, built with Kotlin. 
Uses method annotation for subscribing into a topic to reduce boilerplate code.

Inspired by [EventBus](https://github.com/greenrobot/EventBus) by Greenrobot, it uses a publish-subscribe (pub/sub) model to handle message routing through MQTT topics in a clean, decoupled way.

This library is ideal for apps that require persistant messaging, such as IoT dashboards, real-time status apps, or remote control panels.

## ✨ Features

- 🔁 Persistent MQTT connection via a background `Service`
- 📨 Topic-based message subscription using annotations
- 🧵 Optional thread-mode control for subscribers (`MAIN`, `BACKGROUND`, `ASYNC`)
- ⚙️ Configurable via metadata or code (XML or programmatic)
- 🔐 Supports authentication with username/password


## 🚀 Getting Started

1. Add `SkyRouteService` to your `AndroidManifest.xml`

    ```xml
    <!-- Required Internet permissions -->
    <uses-permission android:name="android.permission.INTERNET" />

    <application>
        <service android:name="com.skyroute.service.SkyRouteService" />
    </application>
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
        override fun onCreate() {
            super.onCreate()
            SkyRoute.getDefault().init(applicationContext, MqttConfig(
                brokerUrl = "tcp://your-broker-url:1883",
                clientPrefix = "sky",
                /* ... */
            ))
        }
    }
    ```

3. Define a Message Type (POJO / Data Class):

    ```kotlin
    data class TopicMessage(
        val message: String,
        /* Add fields as needed */
    )
    ```

4. Create a Subscriber<br>

    Annotate methods with `@Subscribe`. `threadMode` is optional (default is `ThreadMode.MAIN`):

    ```kotlin
    @Subscribe(topic = "topic/name", threadMode = ThreadMode.BACKGROUND)
    fun subscribeToNames(message: TopicMessage) {
        // Do your stuff
    }
    ```

5. Register / Unregister the Subscriber<br>

    Typically inside an Activity or Fragment lifecycle:

    ```kotlin
    override fun onStart() {
        super.onStart()
        SkyRoute.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        SkyRoute.getDefault().unregister(this)
    }
    ```

5. Publish Messages

    ```kotlin
    SkyRoute.getDefault().publish("topic/name", TopicMessage("Hello!"))
    ```


## 🛠️ Service Metadata Configuration

You can configure SkyRouteMQ using `<meta-data>` tags inside the `AndroidManifest.xml`:


```xml
<service android:name="com.skyroute.service.SkyRouteService">
    <meta-data
        android:name="mqttBrokerUrl"
        android:value="tcp://your-broker-url:1883" />
    <meta-data
        android:name="keepAliveInterval"
        android:value="60" />
</service>
```

> **ℹ️** If you initialize SkyRouteMQ using code (e.g., `init(context, config)`), it will override these metadata values.


### Supported Metadata Options

| Key                  | Type    | Default Value          | Description                                     |
|----------------------|---------|------------------------|-------------------------------------------------|
| `mqttBrokerUrl`      | String  | `tcp://127.0.0.1:1883` | URL of the MQTT broker.                         |
| `clientPrefix`       | String  | `skyroute`             | Prefix used for the MQTT client ID.             |
| `cleanSession`       | Boolean | `true`                 | Whether to start a clean session on connection. |
| `connectionTimeout`  | Int     | `10`                   | Connection timeout in seconds.                  |
| `keepAliveInterval`  | Int     | `30`                   | Keep-alive interval in seconds.                 |
| `maxInFlight`        | Int     | `10`                   | Maximum number of in-flight messages.           |
| `autoReconnect`      | Boolean | `true`                 | Whether to automatically reconnect on failure.  |
| `username`           | String  | `null`                 | MQTT username for authentication (optional).    |
| `password`           | String  | `null`                 | MQTT password for authentication (optional).    |


## 📁 Example

You can find a full working example in the [example-app folder](https://github.com/skyroute/skyroute-mq/tree/main/example-app).
It demonstrates how to:

- Initialize SkyRouteMQ in an `Application` class
- Register and unregister subscribers in an `Activity` and `ViewModel`
- Define message classes and handle subscriptions using `@Subscribe`
- Publish messages to topics


## 📄 License

SkyRouteMQ is licensed under the [Apache License 2.0](LICENSE).  
You are free to use, modify, and distribute this software under the terms of that license.