package com.jeremy.rootwebrtcstreamer

import fi.iki.elonen.NanoHTTPD
import java.io.IOException

class LocalWebServer(
    private val onBrowserOffer: (String) -> String,
    port: Int = 8080
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri

        if (uri == "/offer" && session.method == Method.POST) {
            val map = HashMap<String, String>()
            try {
                session.parseBody(map)
            } catch (e: IOException) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "IO Error: ${e.message}")
            } catch (e: ResponseException) {
                return newFixedLengthResponse(session.status, MIME_PLAINTEXT, "Response Error: ${e.message}")
            }

            val clientOfferSdp = map["postData"] ?: session.parms["offer"] ?: ""
            if (clientOfferSdp.isBlank()) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing WebRTC offer payload")
            }

            val answerSdp = onBrowserOffer(clientOfferSdp)
            if (answerSdp.isBlank()) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Failed to generate WebRTC answer")
            }

            return newFixedLengthResponse(Response.Status.OK, "application/sdp", answerSdp)
        }

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Root WebRTC Streamer</title>
                <style>
                    body { margin: 0; background: #000; display: flex; justify-content: center; align-items: center; height: 100vh; overflow: hidden; font-family: sans-serif; }
                    video { width: 100%; height: 100%; object-fit: contain; }
                    #status { position: absolute; top: 10px; color: #00ff00; font-size: 14px; background: rgba(0,0,0,0.6); padding: 5px 10px; border-radius: 4px; }
                </style>
            </head>
            <body>
                <div id="status">Connecting WebRTC...</div>
                <video id="remoteVideo" autoplay playsinline muted></video>
                <script>
                    const videoElement = document.getElementById('remoteVideo');
                    const statusEl = document.getElementById('status');
                    
                    const pc = new RTCPeerConnection({ iceServers: [{ urls: 'stun:stun.l.google.com:19302' }] });
                    
                    pc.ontrack = (event) => {
                        statusEl.style.display = 'none';
                        if (videoElement.srcObject !== event.streams[0]) {
                            videoElement.srcObject = event.streams[0];
                            videoElement.play().catch(err => console.error("Autoplay prevented:", err));
                        }
                    };

                    pc.addTransceiver('video', { direction: 'recvonly' });

                    async function startStreaming() {
                        try {
                            const offer = await pc.createOffer();
                            await pc.setLocalDescription(offer);

                            const response = await fetch('/offer', {
                                method: 'POST',
                                headers: { 'Content-Type': 'text/plain' },
                                body: offer.sdp
                            });

                            if (!response.ok) throw new Error('Signaling error: ' + response.statusText);

                            const answerSdp = await response.text();
                            await pc.setRemoteDescription(new RTCSessionDescription({ type: 'answer', sdp: answerSdp }));
                            statusEl.innerText = "Stream Connected";
                        } catch (err) {
                            console.error(err);
                            statusEl.innerText = "Connection Failed: " + err.message;
                            statusEl.style.color = "#ff5555";
                        }
                    }

                    startStreaming();
                </script>
            </body>
            </html>
        """.trimIndent()

        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=UTF-8", htmlContent)
    }
}
