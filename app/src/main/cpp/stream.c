#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <linux/input.h>
#include <rtc/rtc.h>

int bridge_worker_running = 0;

void bridge_worker_on_message(int dc, const char *message, int size, void *user_ptr) {
    printf("[Stream HID] Received input command: %.*s\n", size, message);
}

int bridge_worker_init() {
    printf("[*] Initializing WebRTC stream worker...\n");
    
    rtcConfiguration config;
    memset(&config, 0, sizeof(config));
    const char *stunServers[] = {"stun:stun.l.google.com:19302"};
    config.iceServers = stunServers;
    config.iceServerSize = 1;

    int pc = rtcCreatePeerConnection(&config);
    if (pc < 0) {
        fprintf(stderr, "[-] Failed to create WebRTC peer connection\n");
        return -1;
    }

    int dc = rtcCreateDataChannel(pc, "stream-control");
    if (dc >= 0) {
        rtcSetMessageCallback(dc, bridge_worker_on_message);
    }

    bridge_worker_running = 1;
    return pc;
}

void bridge_worker_loop_step(int pc) {
    usleep(50000); 
}

void bridge_worker_stop(int pc) {
    printf("[*] Stopping stream worker...\n");
    bridge_worker_running = 0;
    rtcClosePeerConnection(pc);
}
