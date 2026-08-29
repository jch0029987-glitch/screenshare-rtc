package com.jeremy.rootwebrtcstreamer

import fi.iki.elonen.NanoHTTPD

class LocalWebServer(private val onBrowserOffer: (String) -> String, port: Int = 8080) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {
        if (session.uri == "/negotiate" && session.method == Method.POST) {
            val map = HashMap<String, String>()
            session.parseBody(map)
            val browserOfferSdp = map["postData"] ?: ""
            
            // Android generates the answer automatically and returns it back to the iPhone script
            val answerSdp = onBrowserOffer(browserOfferSdp)
            return newFixedLengthResponse(Response.Status.OK, "application/json", answerSdp)
        }

        val html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>Auto Streamer</title>
                <style>
                    body { background: #000; color: #fff; margin: 0; display: flex; justify-content: center; align-items: center; height: 100vh; overflow: hidden; }
                    video { width: 100vw; height: 100vh; object-fit: contain; }
                </style>
            </head>
            <body>
                <video id="remoteVideo" autoplay playsinline></video>
                <script>
                    async function autoConnect() {
                        const pc = new RTCPeerConnection({iceServers: []});
                        pc.ontrack = e => document.getElementById('remoteVideo').srcObject = e.streams[0];
                        
                        const dc = pc.createDataChannel("stream-control");
                        
                        const offer = await pc.createOffer();
                        await pc.setLocalDescription(offer);

                        // Automatically post offer to Android server without user intervention
                        const response = await fetch('/negotiate', {
                            method: 'POST',
                            body: offer.sdp
                        });
                        const answerSdp = await response.text();

                        await pc.setRemoteDescription(new RTCSessionDescription({type: 'answer', sdp: answerSdp}));
                    }
                    window.onload = autoConnect;
                </script>
            </body>
            </html>
        """.trimIndent()
        return newFixedLengthResponse(Response.Status.OK, "text/html", html)
    }
}
