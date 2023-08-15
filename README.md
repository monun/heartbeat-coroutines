# Heartbeat Coroutines

[![Kotlin](https://img.shields.io/badge/java-17-ED8B00.svg?logo=java)](https://www.azul.com/)
[![Kotlin](https://img.shields.io/badge/kotlin-1.8.22-585DEF.svg?logo=kotlin)](http://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/kotlinx.coroutines-1.7.3-585DEF.svg)](https://github.com/Kotlin/kotlinx.coroutines)
[![Gradle](https://img.shields.io/badge/gradle-8.2.1-02303A.svg?logo=gradle)](https://gradle.org)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.monun/heartbeat-coroutines)](https://search.maven.org/artifact/io.github.monun/heartbeat-coroutines)
[![GitHub](https://img.shields.io/github/license/monun/invfx)](https://www.gnu.org/licenses/gpl-3.0.html)
[![Kotlin](https://img.shields.io/badge/youtube-각별-red.svg?logo=youtube)](https://www.youtube.com/channel/UCDrAR1OWC2MD4s0JLetN0MA)

<del><ruby>두근두근<rp>（</rp><rt>ドキドキ</rt><rp>）</rp></ruby>❤️<ruby>코루틴<rp>（</rp><rt>コルーチン</rt><rp>）</rp></ruby></del>

### Coroutine for Paper

---

* #### Features
    * Bukkit의 mainHeartBeat(GameLoop)에서 dispatch되는 Coroutine
    * JavaPlugin 생명주기의 CoroutineScope
    * 유연한 지연작업
* #### Supported minecraft versions
  * 1.18 이상의 **Paper** 및 포크들
  * Spigot, CraftBukkit은 지원하지 않습니다.

---

GameLoop 내에서 병렬 혹은 비동기 라이브러리 없이 다음 연쇄 작업을 처리하는 코드를 작성해보겠습니다.

1. 3초간 1초마다 카운트다운 메시지를 방송
2. 5초간 1초마다 모든 개체에게 데미지
3. `서프라이즈~` 메시지를 방송 후 종료 ~~악질 운영자~~

<br>

#### Thread

```kotlin
Runnable {
    repeat(3) {
        // 비동기 문제를 해결하기 위해 GameLoop의 Thread에서 호출 
        GameLoop.runLater {
            broadcast(3 - it)
            Thread.sleep(1000L)
        }
    }
    repeat(5) {
        GameLoop.runLater {
            damageAll()
        }
        Thread.sleep(1000L)
    }
    GameLoop.runLater {
        broadcast("surprise~")
    }
}.let {
    Thread(it).start()
}
```

<br>

#### Callback

```kotlin
// 비동기로 어디선가 실행해주는 함수
async({
    repeat(3) {
        GameLoop.runLater {
            broadcast(3 - it)
            Thread.sleep(1000L)
        }
    }
}) {
    async({
        repeat(5) {
            GameLoop.runLater {
                damageAll()
            }
            Thread.sleep(1000L)
        }
    }) {
        async {
            GameLoop.runLater {
                broadcast("surprise~")
            }
        }
    }
}

```

<br>

#### FSM

```kotlin
// 취소 가능한 태스크
class Surprise : GameLoopTask() {
    private var state = 0
    private var countdownTicks = 0
    private var damageTicks = 0

    // 1초마다 GameLoop에서 호출
    override fun run() {
        when (state) {
            0 -> {
                val message = 3 - countdownTicks++
                broadcast(message)

                if (countdownTicks >= 3) state = 1
            }
            1 -> {
                damageAll()

                if (++damageTicks >= 5) state = 2
            }
            else -> {
                broadcast("surprise~")
                cancel()
            }
        }
    }
}

```

<br>

극단적인 예를 들었습니다만..

실제로 GameLoop 내에서 연쇄, 순차적인 루틴을 처리하기 위해선 대부분 위 예제들과 같은 구조의 코드를 작성하게 됩니다.

루틴이 복잡해질수록 비동기 문제, 복잡성으로 인해 유연성은 떨어지고 유지보수 난이도는 기하급수로 상승합니다.

Coroutine을 이용하면 GameLoop 내 연쇄 작업 코드의 복잡성을 획기적으로 줄일 수 있습니다.

아래 예제는 GameLoop 내에서 동기적으로 실행되는 Coroutine 코드입니다.

<br>

#### Coroutine

```kotlin
// GameLoopDispatcher = GameLoop에서 Coroutine을 실행하는 CoroutineDispatcher 
CoroutineScope(GameLoopDispatcher).launch {
    repeat(3) {
        broadcast(3 - it)
        delay(1000L)
    }
    repeat(5) {
        damageAll()
        delay(1000L)
    }
    broadcast("surprise~")
}
```

Thread의 코드와 비슷하지만 GameLoop 내에서 동기적으로 실행 가능한 코드입니다!

Coroutine의 동작원리는 이 [문서](https://kotlinlang.org/docs/coroutines-overview.html) 를 참고하세요.

---

## Heartbeat coroutines 시작하기

### Gradle

```gradle
repositories {
    mavenCentral()
}
```

```gradle
dependencies {
    implementation("io.github.monun:heartbeat-coroutines:<version>")
}
```

### Example

```kotlin
import io.github.monun.heartbeat.coroutines.HeartbeatScope
import kotlinx.coroutines.launch

// JavaPlugin#onEnable()
HeartbeatScope().launch {
    val suspension = Suspension()
    repeat(10) {
        logger.info(server.isPrimaryThread)
        suspension.delay(75L)
    }
    logger.info("BOOM")
}
```

---

### Dispatchers.Heartbeat

`JavaPlugin`과 같은 생명주기를 가진 `CoroutineDispatcher`입니다.

Coroutine을 Bukkit의 PrimaryThread에서만 실행합니다.

---

### HeartbeatScope()

`JavaPlugin`과 같은 생명주기를 가진 `CoroutineScope`입니다.

`Dispatchers.Heartbeat`를 `CoroutineDispatcher`로 가지며 `JavaPlugin` 생명주기를 따라가는 `SupervisorJob`을 부모로 가집니다.

---

### Suspension

누적 지연 기능을 가진 클래스입니다.

`Dispatchers.Heartbeat`는 Coroutine을 Bukkit의 PrimaryThread에서 실행하기 위해서 `BukkitScheduler#runTask`를 사용합니다.

`BukkitScheduler`는 1tick(50ms)마다 등록된 태스크들을 실행하며 서버 상태에 따라 지연될 수 있습니다.

Coroutine은 지연을 millisecond 단위로 제어 할 수 있으며 이는 `Dispatchers.Heartbeat`에서 실행될 때 결과가 기대와 다를 수 있습니다.

`delay(1)` 함수가 호출 될 때 `Dispatchers.Heartbeat`에서는 50ms 이상 늘어날 수 있습니다.

`Suspension`은 내부적으로 누적되는 지연 시간을 가지며 누적된 시간이 과거일 경우 yield()를 호출하고 미래일 경우 남은 시간만큼 `delay`를 호출합니다. 
