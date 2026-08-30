package com.jeremy.rootwebrtcstreamer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.webrtc.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private var factory: PeerConnectionFactory? = null
    private var webServer: LocalWebServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        extractAndPrepareBinary()

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        
        val options = PeerConnectionFactory.Options()
        factory = PeerConnectionFactory.builder().setOptions(options).createPeerConnectionFactory()

        startEmbeddedWebServer()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StreamerDashboard()
                }
            }
        }
    }

    private fun extractAndPrepareBinary() {
        Thread {
            try {
                val sourcePath = applicationInfo.nativeLibraryDir + "/libstream.so"
                val targetPath = "/data/local/tmp/stream"
                Runtime.getRuntime().exec(arrayOf("su", "-c", "cp $sourcePath $targetPath && chmod 755 $targetPath")).waitFor()
            } catch (_: Exception) {}
        }.start()
    }

    private fun startEmbeddedWebServer() {
        webServer = LocalWebServer(onBrowserOffer = { browserOfferSdp ->
            processWebRtcNegotiation(browserOfferSdp)
        }, port = 8080)
        
        try {
            webServer?.start()
        } catch (_: Exception) {}
    }

    private fun processWebRtcNegotiation(offerSdp: String): String {
        var answerSdp = ""
        val latch = CountDownLatch(1)
        val config = PeerConnection.RTCConfiguration(emptyList())

        val peerConnection = factory?.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(p0: IceCandidate?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(dc: DataChannel?) {
                dc?.registerObserver(object : DataChannel.Observer {
                    override fun onBufferedAmountChange(p0: Long) {}
                    override fun onStateChange() {}
                    override fun onMessage(buffer: DataChannel.Buffer) {
                        val data = buffer.data
                        val bytes = ByteArray(data.remaining())
                        data.get(bytes)
                        executeRootInput(String(bytes))
                    }
                })
            }
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
        }) ?: return ""

        val remoteDescription = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        
        peerConnection.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                peerConnection.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(answer: SessionDescription?) {
                        if (answer != null) {
                            peerConnection.setLocalDescription(object : SdpObserver {
                                override fun onCreateSuccess(p0: SessionDescription?) {}
                                override fun onSetSuccess() {
                                    answerSdp = answer.description
                                    latch.countDown()
                                }
                                override fun onCreateFailure(p0: String?) { latch.countDown() }
                                override fun onSetFailure(p0: String?) { latch.countDown() }
                            }, answer)
                        } else {
                            latch.countDown()
                        }
                    }
                    override fun onCreateFailure(p0: String?) { latch.countDown() }
                    override fun onSetSuccess() {}
                    override fun onSetFailure(p0: String?) { latch.countDown() }
                }, MediaConstraints())
            }
            override fun onCreateFailure(p0: String?) { latch.countDown() }
            override fun onSetFailure(p0: String?) { latch.countDown() }
        }, remoteDescription)

        latch.await(3, TimeUnit.SECONDS)
        return answerSdp
    }

    private fun executeRootInput(payload: String) {
        Thread {
            try {
                Runtime.getRuntime().exec(arrayOf("su", "-c", "/data/local/tmp/stream", payload)).waitFor()
            } catch (_: Exception) {}
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        webServer?.stop()
    }
}

@Composable
fun StreamerDashboard() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Root WebRTC Streamer Active", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "iPhone Target: http://192.168.43.1:8080",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
