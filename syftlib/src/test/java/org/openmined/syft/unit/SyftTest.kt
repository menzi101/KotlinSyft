package org.openmined.syft.unit

import android.net.NetworkCapabilities
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import org.openmined.syft.Syft
import org.openmined.syft.domain.SyftConfiguration
import org.openmined.syft.execution.SyftJob
import org.openmined.syft.monitor.DeviceMonitor
import org.openmined.syft.networking.clients.HttpClient
import org.openmined.syft.networking.clients.SocketClient
import org.openmined.syft.networking.datamodels.syft.AuthenticationRequest
import org.openmined.syft.networking.datamodels.syft.AuthenticationResponse
import org.openmined.syft.threading.ProcessSchedulers

internal class SyftTest {

    @Test
    @ExperimentalUnsignedTypes
    fun `Given a syft object when requestCycle is invoked the socket client calls authenticate api`() {
        val socketClient = mock<SocketClient> {
            on { authenticate(AuthenticationRequest("auth token")) }.thenReturn(
                Single.just(
                    AuthenticationResponse.AuthenticationSuccess(
                        "test id"
                    )
                )
            )
        }
        val httpClient = mock<HttpClient>() {
            on { apiClient } doReturn mock()
        }
        val schedulers = object : ProcessSchedulers {
            override val computeThreadScheduler: Scheduler
                get() = Schedulers.io()
            override val calleeThreadScheduler: Scheduler
                get() = AndroidSchedulers.mainThread()
        }

        val deviceMonitor = mock<DeviceMonitor> {
            on { isActivityStateValid() }.thenReturn(true)
            on { isNetworkStateValid() }.thenReturn(true)
            on { isBatteryStateValid() }.thenReturn(true)
        }

        val config = SyftConfiguration(
            mock(),
            schedulers,
            schedulers,
            mock(),
            true,
            listOf(),
            NetworkCapabilities.TRANSPORT_WIFI,
            0L,
            1,
            socketClient,
            httpClient,
            SyftConfiguration.NetworkingClients.SOCKET
        )
        val workerTest = spy(
            Syft( config, deviceMonitor,"auth token")
        )
        val syftJob = SyftJob.create(
            "model name",
            "1.0.0",
            workerTest,
            config
        )

        workerTest.executeCycleRequest(syftJob)
        verify(socketClient).authenticate(AuthenticationRequest("auth token"))
    }
}
